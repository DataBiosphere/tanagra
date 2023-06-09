package bio.terra.tanagra.utils;

import bio.terra.tanagra.exception.SystemException;
import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for talking to Google Cloud Storage. This class maintains a singleton instance of
 * the Storage service.
 */
public final class GoogleCloudStorage {
  private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudStorage.class);

  // default value for the maximum number of times to retry HTTP requests to GCS
  public static final int GCS_MAXIMUM_RETRIES = 5;
  private static final org.threeten.bp.Duration MAX_GCS_CLIENT_TIMEOUT =
      org.threeten.bp.Duration.ofMinutes(5);

  private static final long DEFAULT_SIGNED_URL_DURATION = 30;
  private static final TimeUnit DEFAULT_SIGNED_URL_UNIT = TimeUnit.MINUTES;
  private final Storage storage;

  public GoogleCloudStorage(GoogleCredentials credentials, String projectId) {
    this.storage =
        StorageOptions.newBuilder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .setRetrySettings(
                RetrySettings.newBuilder().setTotalTimeout(MAX_GCS_CLIENT_TIMEOUT).build())
            .build()
            .getService();
  }

  public static GoogleCloudStorage forApplicationDefaultCredentials(String projectId) {
    GoogleCredentials credentials;
    try {
      credentials = GoogleCredentials.getApplicationDefault();
    } catch (IOException ioEx) {
      throw new SystemException("Error loading application default credentials", ioEx);
    }
    return new GoogleCloudStorage(credentials, projectId);
  }

  public Optional<Bucket> getBucket(
      String bucketName, Storage.BucketGetOption... bucketGetOptions) {
    Bucket bucket =
        callWithRetries(() -> storage.get(bucketName, bucketGetOptions), "Error looking up bucket");
    return Optional.ofNullable(bucket);
  }

  public Optional<Blob> getBlob(String fullGcsPath) {
    Blob blob =
        callWithRetries(
            () -> storage.get(BlobId.fromGsUtilUri(fullGcsPath)), "Error looking up blob");
    return Optional.ofNullable(blob);
  }

  public BlobId writeFile(String bucketName, String fileName, String fileContents) {
    BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, fileName)).build();
    Blob blob =
        callWithRetries(
            () -> storage.create(blobInfo, fileContents.getBytes(StandardCharsets.UTF_8)),
            "Error creating blob");
    return blob.getBlobId();
  }

  public String createSignedUrl(String fullGcsPath) {
    return createSignedUrl(fullGcsPath, DEFAULT_SIGNED_URL_DURATION, DEFAULT_SIGNED_URL_UNIT);
  }

  public String createSignedUrl(String fullGcsPath, long duration, TimeUnit durationUnit) {
    BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.fromGsUtilUri(fullGcsPath)).build();
    URL url =
        callWithRetries(
            () ->
                storage.signUrl(
                    blobInfo, duration, durationUnit, Storage.SignUrlOption.withV4Signature()),
            "Error generating signed URL");
    return url.toString();
  }

  /** Strip the gs:// prefix to get just the bucket name. */
  public static String getBucketNameFromUrl(String bucketUrl) {
    String[] urlPieces = bucketUrl.split("/");
    if (urlPieces.length < 3) { // NOPMD
      throw new IllegalArgumentException("Invalid GCS bucket url: " + bucketUrl);
    }
    return urlPieces[2];
  }

  /**
   * Utility method that checks if an exception thrown by the GCS client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  static boolean isRetryable(Exception ex) {
    if (ex instanceof SocketTimeoutException) {
      return true;
    }
    if (!(ex instanceof StorageException)) {
      return false;
    }
    LOGGER.error("Caught a GCS error.", ex);
    int statusCode = ((StorageException) ex).getCode();

    return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
        || statusCode == HttpStatus.SC_BAD_GATEWAY
        || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT

        // retry forbidden errors because we often see propagation delays when a user is just
        // granted access
        || statusCode == HttpStatus.SC_FORBIDDEN;
  }

  /**
   * Execute a function that includes hitting GCS endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the GCS client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the GCS client or the retries
   */
  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, IOException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () ->
            HttpUtils.callWithRetries(
                makeRequest,
                GoogleCloudStorage::isRetryable,
                GCS_MAXIMUM_RETRIES,
                HttpUtils.DEFAULT_DURATION_SLEEP_FOR_RETRY),
        errorMsg);
  }

  /**
   * Execute a function that includes hitting GCS endpoints. If an exception is thrown by the GCS
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the GCS client or the retries
   */
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, IOException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (IOException | InterruptedException ex) {
      // wrap the GCS exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }
}
