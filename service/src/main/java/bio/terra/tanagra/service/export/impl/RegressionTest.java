package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.proto.regressiontest.RTExportCounts;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import bio.terra.tanagra.utils.NameUtils;
import bio.terra.tanagra.utils.ProtobufUtils;
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

public class RegressionTest implements DataExport {
    private List<String> gcsBucketNames;

    @Override
    public Type getType() {
        return Type.REGRESSION_TEST;
    }

    @Override
    public String getDefaultDisplayName() {
        return "Download regression test file";
    }

    @Override
    public String getDescription() {
        return "Signed URL to a regression test file that includes the total number of rows returned for each output entity.";
    }

    @Override
    public void initialize(DeploymentConfig deploymentConfig) {
        gcsBucketNames = deploymentConfig.getShared().getGcsBucketNames();
    }
    @Override
    public ExportResult run(ExportRequest request, DataExportHelper helper) {
        // Get the counts for each output entity.

        // Build the export counts proto object.
        RTExportCounts.ExportCounts exportCounts = RTExportCounts.ExportCounts.newBuilder().build();

        // Write the proto object to a GCS file. Just pick the first bucket name.
        String fileContents = ProtobufUtils.serializeToJson(exportCounts);
        String fileName = "regression_test_" + Instant.now() + ".ipynb";
        BlobId blobId =
                helper.getStorageService().writeFile(gcsBucketNames.get(0), fileName, fileContents);

        // Generate a signed URL for the ipynb file.
        String ipynbSignedUrl = helper.getStorageService().createSignedUrl(blobId.toGsUtilUri());

        ExportFileResult exportFileResult =
                ExportFileResult.forFile(fileName, ipynbSignedUrl, null, null);
        exportFileResult.addTags(List.of("Regression Test File"));
        return ExportResult.forOutputParams(Map.of(), List.of(exportFileResult));
    }
}
