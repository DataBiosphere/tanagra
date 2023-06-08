package bio.terra.tanagra.service.export;

import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.service.export.impl.GcsTransferServiceFile;
import bio.terra.tanagra.service.export.impl.ListOfSignedUrls;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public interface DataExport {
  enum Model {
    LIST_OF_SIGNED_URLS(() -> new ListOfSignedUrls()),
    GCS_TRANSFER_SERVICE_FILE(() -> new GcsTransferServiceFile());

    private Supplier<DataExport> createNewInstanceFn;

    Model(Supplier<DataExport> createNewInstanceFn) {
      this.createNewInstanceFn = createNewInstanceFn;
    }

    public DataExport createNewInstance() {
      return createNewInstanceFn.get();
    }
  }

  default void initialize(CommonInfrastructure commonInfrastructure, List<String> params) {
    // Do nothing with parameters.
  }

  String getDescription();

  ExportResult run(ExportRequest request);

  class CommonInfrastructure {
    private final String gcpProjectId;
    private final List<String> gcsBucketNames;

    private CommonInfrastructure(String gcpProjectId, List<String> gcsBucketNames) {
      this.gcpProjectId = gcpProjectId;
      this.gcsBucketNames = gcsBucketNames;
    }

    public static CommonInfrastructure fromApplicationConfig(
        ExportConfiguration.ExportInfraConfiguration appConfig) {
      return new CommonInfrastructure(appConfig.getGcsProjectId(), appConfig.getGcsBucketNames());
    }

    public String getGcpProjectId() {
      return gcpProjectId;
    }

    public List<String> getGcsBucketNames() {
      return Collections.unmodifiableList(gcsBucketNames);
    }
  }
}
