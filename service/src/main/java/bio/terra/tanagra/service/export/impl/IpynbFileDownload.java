package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.NameUtils;
import bio.terra.tanagra.utils.SqlFormatter;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;

public class IpynbFileDownload implements DataExport {
  private static final String IPYNB_TEMPLATE_RESOURCE_FILE = "export/notebook_template.ipynb";
  public static final String IPYNB_FILE_KEY = "ipynbFile";
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
  public ExportResult run(ExportRequest request) {
    // Read in the ipynb template file.
    String ipynbTemplate =
        FileUtils.readStringFromFile(
            FileUtils.getResourceFileStream(Path.of(IPYNB_TEMPLATE_RESOURCE_FILE)));

    // Generate the SQL for the primary entity and escape it to substitute into a notebook cell (=
    // JSON property).
    Map<String, String> entityToSql = request.generateSqlQueries();
    String primaryEntitySql =
        SqlFormatter.format(entityToSql.get(request.getUnderlay().getPrimaryEntity()));
    String primaryEntitySqlEscaped = StringEscapeUtils.escapeJson(primaryEntitySql);

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
            .put("entityName", request.getUnderlay().getPrimaryEntity())
            .put("formattedSql", primaryEntitySqlEscaped)
            .build();
    String fileContents = StringSubstitutor.replace(ipynbTemplate, params);

    // Write the ipynb file to GCS. Just pick the first bucket name.
    BlobId blobId =
        request
            .getGoogleCloudStorage()
            .writeFile(
                gcsBucketNames.get(0), "tanagra_export_" + Instant.now() + ".ipynb", fileContents);

    // Generate a signed URL for the ipynb file.
    String ipynbSignedUrl = request.getGoogleCloudStorage().createSignedUrl(blobId.toGsUtilUri());

    return ExportResult.forOutputParams(
        Map.of(IPYNB_FILE_KEY, ipynbSignedUrl), ExportResult.Status.COMPLETE);
  }
}
