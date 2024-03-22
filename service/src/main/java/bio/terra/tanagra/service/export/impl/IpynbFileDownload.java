package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import bio.terra.tanagra.utils.NameUtils;
import bio.terra.tanagra.utils.SqlFormatter;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;

public class IpynbFileDownload implements DataExport {
  private static final String IPYNB_TEMPLATE_FILE_GCS_URL_KEY = "IPYNB_TEMPLATE_FILE_GCS_URL";
  private static final String IPYNB_TEMPLATE_RESOURCE_FILE = "export/notebook_template.ipynb";
  public static final String IPYNB_FILE_KEY = "Notebook File:tanagra_export.ipynb";
  private List<String> gcsBucketNames;

  @Override
  public Type getType() {
    return Type.IPYNB_FILE_DOWNLOAD;
  }

  @Override
  public String getDefaultDisplayName() {
    return "Download ipynb file with embedded SQL";
  }

  @Override
  public String getDescription() {
    return "Signed URL to an ipynb notebook file that includes SQL to get the list of primary entity instances.";
  }

  @Override
  public Map<String, String> describeOutputs() {
    return Map.of(IPYNB_FILE_KEY, "URL to ipynb file");
  }

  @Override
  public void initialize(DeploymentConfig deploymentConfig) {
    gcsBucketNames = deploymentConfig.getShared().getGcsBucketNames();
  }

  @Override
  public ExportResult run(ExportRequest request, DataExportHelper helper) {
    // Read in the ipynb template file.
    String ipynbTemplate;
    try {
      ipynbTemplate =
          request.getInputs().containsKey(IPYNB_TEMPLATE_FILE_GCS_URL_KEY)
              ? GoogleCloudStorage.readFileContentsFromUrl(
                  request.getInputs().get(IPYNB_TEMPLATE_FILE_GCS_URL_KEY))
              : FileUtils.readStringFromFile(
                  FileUtils.getResourceFileStream(Path.of(IPYNB_TEMPLATE_RESOURCE_FILE)));
    } catch (IOException ioEx) {
      if (request.getInputs().containsKey(IPYNB_TEMPLATE_FILE_GCS_URL_KEY)) {
        throw new SystemException(
            "Template ipynb file not found: "
                + request.getInputs().get(IPYNB_TEMPLATE_FILE_GCS_URL_KEY),
            ioEx);
      } else {
        throw new SystemException("Resource file not found: " + IPYNB_TEMPLATE_RESOURCE_FILE, ioEx);
      }
    }

    // Generate the SQL for the primary entity and escape it to substitute into a notebook cell (=
    // JSON property).
    String primaryEntitySql = helper.generateSqlForPrimaryEntity(List.of(), false);
    String primaryEntitySqlFormattedAndEscaped =
        StringEscapeUtils.escapeJson(SqlFormatter.format(primaryEntitySql));

    // Make substitutions in the template file contents.
    String studyIdAndName =
        NameUtils.simplifyStringForName(
            new StringBuilder(
                    request.getStudy().getDisplayName() + "_" + request.getStudy().getId())
                .toString());
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("underlayName", request.getUnderlay().getDisplayName())
            .put("studyName", studyIdAndName)
            .put("timestamp", Instant.now().toString())
            .put("entityName", request.getUnderlay().getPrimaryEntity().getName())
            .put("formattedSql", primaryEntitySqlFormattedAndEscaped)
            .build();
    String fileContents = StringSubstitutor.replace(ipynbTemplate, params);

    // Write the ipynb file to GCS. Just pick the first bucket name.
    String fileName = "tanagra_export_" + Instant.now() + ".ipynb";
    BlobId blobId =
        helper.getStorageService().writeFile(gcsBucketNames.get(0), fileName, fileContents);

    // Generate a signed URL for the ipynb file.
    String ipynbSignedUrl = helper.getStorageService().createSignedUrl(blobId.toGsUtilUri());

    // TODO: Skip populating this output parameter once the UI is processing the file result
    // directly.
    Map<String, String> outputParams = Map.of(IPYNB_FILE_KEY, ipynbSignedUrl);
    ExportFileResult exportFileResult = ExportFileResult.forFile(fileName, ipynbSignedUrl, null);
    return ExportResult.forOutputParams(outputParams, List.of(exportFileResult));
  }
}
