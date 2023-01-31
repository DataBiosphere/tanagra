package bio.terra.tanagra.service.utils;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GcsUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(GcsUtils.class);

  private GcsUtils() {}

  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public static void writeGcsFile(
      String projectId, String bucketName, String fileName, String fileContents) {
    Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    BlobId blobId = BlobId.of(bucketName, fileName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    storage.create(blobInfo, fileContents.getBytes(StandardCharsets.UTF_8));

    LOGGER.info("Wrote file {} to bucket {} in project {}", fileName, bucketName, projectId);
  }
}
