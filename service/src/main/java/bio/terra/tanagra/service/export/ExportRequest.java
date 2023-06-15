package bio.terra.tanagra.service.export;

import bio.terra.tanagra.utils.GoogleCloudStorage;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExportRequest {
  private final String model;
  private final Map<String, String> inputs;
  private final String redirectBackUrl;
  private final String study;
  private final List<String> cohorts;
  private final boolean includeAnnotations;

  private final Supplier<Map<String, String>> generateSqlQueriesFn;
  private final Function<String, Map<String, String>> writeEntityDataToGcsFn;
  private final Function<String, Map<String, String>> writeAnnotationDataToGcsFn;

  private final Supplier<GoogleCloudStorage> getGoogleCloudStorageFn;

  private ExportRequest(Builder builder) {
    this.model = builder.model;
    this.inputs = builder.inputs;
    this.redirectBackUrl = builder.redirectBackUrl;
    this.study = builder.study;
    this.cohorts = builder.cohorts;
    this.includeAnnotations = builder.includeAnnotations;
    this.generateSqlQueriesFn = builder.generateSqlQueriesFn;
    this.writeEntityDataToGcsFn = builder.writeEntityDataToGcsFn;
    this.writeAnnotationDataToGcsFn = builder.writeAnnotationDataToGcsFn;
    this.getGoogleCloudStorageFn = builder.getGoogleCloudStorageFn;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public Map<String, String> getInputs() {
    return Collections.unmodifiableMap(inputs);
  }

  public String getRedirectBackUrl() {
    return redirectBackUrl;
  }

  public String getStudy() {
    return study;
  }

  public List<String> getCohorts() {
    return Collections.unmodifiableList(cohorts);
  }

  public boolean includeAnnotations() {
    return includeAnnotations;
  }

  public Map<String, String> generateSqlQueries() {
    return generateSqlQueriesFn == null ? Collections.emptyMap() : generateSqlQueriesFn.get();
  }

  public Map<String, String> writeEntityDataToGcs(String fileNameTemplate) {
    return writeEntityDataToGcsFn == null
        ? Collections.emptyMap()
        : writeEntityDataToGcsFn.apply(fileNameTemplate);
  }

  public Map<String, String> writeAnnotationDataToGcs(String fileNameTemplate) {
    return writeAnnotationDataToGcsFn == null
        ? Collections.emptyMap()
        : writeAnnotationDataToGcsFn.apply(fileNameTemplate);
  }

  public GoogleCloudStorage getGoogleCloudStorage() {
    return getGoogleCloudStorageFn.get();
  }

  public static class Builder {
    private String model;
    private Map<String, String> inputs;
    private String redirectBackUrl;
    private String study;
    private List<String> cohorts;
    private boolean includeAnnotations;
    private Supplier<Map<String, String>> generateSqlQueriesFn;
    private Function<String, Map<String, String>> writeEntityDataToGcsFn;
    private Function<String, Map<String, String>> writeAnnotationDataToGcsFn;

    private Supplier<GoogleCloudStorage> getGoogleCloudStorageFn;

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder inputs(Map<String, String> inputs) {
      this.inputs = inputs;
      return this;
    }

    public Builder redirectBackUrl(String redirectBackUrl) {
      this.redirectBackUrl = redirectBackUrl;
      return this;
    }

    public Builder study(String study) {
      this.study = study;
      return this;
    }

    public Builder cohorts(List<String> cohorts) {
      this.cohorts = cohorts;
      return this;
    }

    public Builder includeAnnotations(boolean includeAnnotations) {
      this.includeAnnotations = includeAnnotations;
      return this;
    }

    public Builder generateSqlQueriesFn(Supplier<Map<String, String>> generateSqlQueriesFn) {
      this.generateSqlQueriesFn = generateSqlQueriesFn;
      return this;
    }

    public Builder writeEntityDataToGcsFn(
        Function<String, Map<String, String>> writeEntityDataToGcsFn) {
      this.writeEntityDataToGcsFn = writeEntityDataToGcsFn;
      return this;
    }

    public Builder writeAnnotationDataToGcsFn(
        Function<String, Map<String, String>> writeAnnotationDataToGcsFn) {
      this.writeAnnotationDataToGcsFn = writeAnnotationDataToGcsFn;
      return this;
    }

    public Builder getGoogleCloudStorageFn(Supplier<GoogleCloudStorage> getGoogleCloudStorageFn) {
      this.getGoogleCloudStorageFn = getGoogleCloudStorageFn;
      return this;
    }

    public ExportRequest build() {
      if (inputs == null) {
        inputs = new HashMap<>();
      }
      if (cohorts == null) {
        cohorts = new ArrayList<>();
      }
      return new ExportRequest(this);
    }

    public String getModel() {
      return model;
    }

    public String getStudy() {
      return study;
    }

    public List<String> getCohorts() {
      return Collections.unmodifiableList(cohorts);
    }

    public boolean isIncludeAnnotations() {
      return includeAnnotations;
    }
  }
}
