package bio.terra.tanagra.utils;

import bio.terra.tanagra.exception.SystemException;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.grpc.ChannelPoolSettings;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.bigquery.storage.v1.Exceptions;
import com.google.cloud.bigquery.storage.v1.JsonStreamWriter;
import com.google.cloud.bigquery.storage.v1.TableName;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Descriptors;
import io.grpc.Status;
import io.grpc.Status.Code;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.GuardedBy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;

/**
 * Utility class for using the BigQuery Storage Write API. Example code from:
 * https://cloud.google.com/bigquery/docs/write-api-streaming#at-least-once
 */
public final class BigQueryStorageWriter {
  private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryStorageWriter.class);

  private static final int BATCH_SIZE = 1000;
  private static final int MAX_BATCHES = 100;

  private BigQueryStorageWriter() {}

  public static void insertWithStorageWriteApi(
      CredentialsProvider credentialsProvider,
      TableName destinationTableName,
      List<JSONObject> records)
      throws Descriptors.DescriptorValidationException, IOException, InterruptedException {
    DataWriter writer = new DataWriter();

    // One time initialization for the worker.
    writer.initialize(credentialsProvider, destinationTableName);

    // Write one batch of data to the stream for each 1000 rows.
    // Data may be batched up to the maximum request size:
    // https://cloud.google.com/bigquery/quotas#write-api-limits
    int batchNum = 0;
    while (batchNum < MAX_BATCHES) {
      int startIndex = batchNum * BATCH_SIZE;
      if (records.size() <= startIndex) {
        break;
      }

      JSONArray jsonArr = new JSONArray();
      for (int j = 0; j < BATCH_SIZE; j++) {
        if (records.size() == startIndex + j) {
          break;
        }
        jsonArr.put(records.get(startIndex + j));
      }
      writer.append(new AppendContext(jsonArr, 0));
      batchNum++;
    }
    LOGGER.info("BQ Storage Write API: numRecords={}, numBatches={}", records.size(), batchNum);

    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      throw new SystemException("Error calling BQ Storage Write API", e);
    }

    // Final cleanup for the stream during worker teardown.
    writer.cleanup();
  }

  private static class AppendContext {

    private final JSONArray data;
    private int retryCount;

    AppendContext(JSONArray data, int retryCount) {
      this.data = data;
      this.retryCount = retryCount;
    }

    public JSONArray getData() {
      return data;
    }

    public int getRetryCount() {
      return retryCount;
    }

    public void incrementRetryCount() {
      retryCount++;
    }
  }

  private static class DataWriter {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int MAX_RECREATE_COUNT = 3;
    private static final ImmutableList<Code> RETRIABLE_ERROR_CODES =
        ImmutableList.of(
            Code.INTERNAL,
            Code.ABORTED,
            Code.CANCELLED,
            Code.FAILED_PRECONDITION,
            Code.DEADLINE_EXCEEDED,
            Code.UNAVAILABLE);

    // Track the number of in-flight requests to wait for all responses before shutting down.
    private final Phaser inflightRequestCount = new Phaser(1);
    private final Object lock = new Object();
    private JsonStreamWriter streamWriter;

    @GuardedBy("lock")
    private RuntimeException error;

    private final AtomicInteger recreateCount = new AtomicInteger(0);

    public void initialize(CredentialsProvider credentialsProvider, TableName parentTable)
        throws Descriptors.DescriptorValidationException, IOException, InterruptedException {
      // Use the JSON stream writer to send records in JSON format. Specify the table name to write
      // to the default stream.
      // For more information about JsonStreamWriter, see:
      // https://googleapis.dev/java/google-cloud-bigquerystorage/latest/com/google/cloud/bigquery/storage/v1/JsonStreamWriter.html
      streamWriter =
          JsonStreamWriter.newBuilder(parentTable.toString(), BigQueryWriteClient.create())
              .setCredentialsProvider(credentialsProvider)
              .setExecutorProvider(
                  FixedExecutorProvider.create(Executors.newScheduledThreadPool(100)))
              .setChannelProvider(
                  BigQueryWriteSettings.defaultGrpcTransportProviderBuilder()
                      .setKeepAliveTime(Duration.ofMinutes(1))
                      .setKeepAliveTimeout(Duration.ofMinutes(1))
                      .setKeepAliveWithoutCalls(true)
                      .setChannelPoolSettings(
                          ChannelPoolSettings.builder().setMaxChannelCount(2).build())
                      .build())
              .setEnableConnectionPool(true)
              .build();
    }

    public void append(AppendContext appendContext)
        throws Descriptors.DescriptorValidationException, IOException, InterruptedException {
      synchronized (this.lock) {
        if (!streamWriter.isUserClosed()
            && streamWriter.isClosed()
            && recreateCount.getAndIncrement() < MAX_RECREATE_COUNT) {
          streamWriter =
              JsonStreamWriter.newBuilder(
                      streamWriter.getStreamName(), BigQueryWriteClient.create())
                  .build();
          this.error = null;
        }
        // If earlier appends have failed, we need to reset before continuing.
        if (this.error != null) {
          throw this.error;
        }
      }
      // Append asynchronously for increased throughput.
      ApiFuture<AppendRowsResponse> future = streamWriter.append(appendContext.data);
      ApiFutures.addCallback(
          future, new AppendCompleteCallback(this, appendContext), MoreExecutors.directExecutor());

      // Increase the count of in-flight requests.
      inflightRequestCount.register();
    }

    public void cleanup() {
      // Wait for all in-flight requests to complete.
      inflightRequestCount.arriveAndAwaitAdvance();

      // Close the connection to the server.
      streamWriter.close();

      // Verify that no error occurred in the stream.
      synchronized (this.lock) {
        if (this.error != null) {
          throw this.error;
        }
      }
    }

    static class AppendCompleteCallback implements ApiFutureCallback<AppendRowsResponse> {

      private final DataWriter parent;
      private final AppendContext appendContext;

      AppendCompleteCallback(DataWriter parent, AppendContext appendContext) {
        this.parent = parent;
        this.appendContext = appendContext;
      }

      @Override
      public void onSuccess(AppendRowsResponse response) {
        System.out.format("Append success%n");
        this.parent.recreateCount.set(0);
        done();
      }

      @Override
      public void onFailure(Throwable throwable) {
        // If the wrapped exception is a StatusRuntimeException, check the state of the operation.
        // If the state is INTERNAL, CANCELLED, or ABORTED, you can retry. For more information,
        // see: https://grpc.github.io/grpc-java/javadoc/io/grpc/StatusRuntimeException.html
        Status status = Status.fromThrowable(throwable);
        if (appendContext.retryCount < MAX_RETRY_COUNT
            && RETRIABLE_ERROR_CODES.contains(status.getCode())) {
          appendContext.retryCount++;
          try {
            // Since default stream appends are not ordered, we can simply retry the appends.
            // Retrying with exclusive streams requires more careful consideration.
            this.parent.append(appendContext);
            // Mark the existing attempt as done since it's being retried.
            done();
            return;
          } catch (Exception e) {
            // Fall through to return error.
            System.out.format("Failed to retry append: %s%n", e);
          }
        }

        if (throwable instanceof Exceptions.AppendSerializationError) {
          Exceptions.AppendSerializationError ase = (Exceptions.AppendSerializationError) throwable;
          Map<Integer, String> rowIndexToErrorMessage = ase.getRowIndexToErrorMessage();
          if (!rowIndexToErrorMessage.isEmpty()) {
            // Omit the faulty rows
            JSONArray dataNew = new JSONArray();
            for (int i = 0; i < appendContext.data.length(); i++) {
              if (!rowIndexToErrorMessage.containsKey(i)) {
                dataNew.put(appendContext.data.get(i));
              } // else {} // process faulty rows by placing them on a dead-letter-queue, for
              // instance
            }

            // Retry the remaining valid rows, but using a separate thread to
            // avoid potentially blocking while we are in a callback.
            if (dataNew.length() > 0) {
              try {
                this.parent.append(new AppendContext(dataNew, 0));
              } catch (Descriptors.DescriptorValidationException
                  | IOException
                  | InterruptedException e) {
                throw new SystemException("Error calling BQ Storage Write API", e);
              }
            }
            // Mark the existing attempt as done since we got a response for it
            done();
            return;
          }
        }

        synchronized (this.parent.lock) {
          if (this.parent.error == null) {
            Exceptions.StorageException storageException = Exceptions.toStorageException(throwable);
            this.parent.error =
                (storageException != null) ? storageException : new RuntimeException(throwable);
          }
        }
        done();
      }

      private void done() {
        // Reduce the count of in-flight requests.
        this.parent.inflightRequestCount.arriveAndDeregister();
      }
    }
  }
}
