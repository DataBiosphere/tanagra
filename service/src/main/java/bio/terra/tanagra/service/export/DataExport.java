package bio.terra.tanagra.service.export;

import bio.terra.tanagra.service.export.impl.IndividualFileDownload;
import bio.terra.tanagra.service.export.impl.IpynbFileDownload;
import bio.terra.tanagra.service.export.impl.RegressionTest;
import bio.terra.tanagra.service.export.impl.VwbFileExport;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public interface DataExport {
  enum Type {
    INDIVIDUAL_FILE_DOWNLOAD(IndividualFileDownload::new),
    VWB_FILE_EXPORT(VwbFileExport::new),
    IPYNB_FILE_DOWNLOAD(IpynbFileDownload::new),
    REGRESSION_TEST(RegressionTest::new);

    private final Supplier<DataExport> createNewInstanceFn;

    Type(Supplier<DataExport> createNewInstanceFn) {
      this.createNewInstanceFn = createNewInstanceFn;
    }

    public DataExport createNewInstance() {
      return createNewInstanceFn.get();
    }
  }

  default void initialize(DeploymentConfig deploymentConfig) {
    // Do nothing with parameters.
  }

  Type getType();

  String getDefaultDisplayName();

  String getDescription();

  default Map<String, String> describeInputs() {
    // There are no input parameters.
    return Collections.emptyMap();
  }

  default Map<String, String> describeOutputs() {
    // There are no output parameters.
    return Collections.emptyMap();
  }

  ExportResult run(ExportRequest request, DataExportHelper helper);
}
