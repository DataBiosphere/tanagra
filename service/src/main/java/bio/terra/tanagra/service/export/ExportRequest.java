package bio.terra.tanagra.service.export;

import bio.terra.tanagra.generated.model.ApiCohortV2;
import bio.terra.tanagra.generated.model.ApiStudyV2;
import bio.terra.tanagra.generated.model.ApiUnderlayV2;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExportRequest {
  private final String model;
  private final Map<String, String> inputs;
  private final String redirectBackUrl;
  private final boolean includeAnnotations;
  private final ApiUnderlayV2 underlay;
  private final ApiStudyV2 study;
  private final List<ApiCohortV2> cohorts;
  private final Supplier<Map<String, String>> generateSqlQueriesFn;
  private final Function<String, Map<String, String>> writeEntityDataToGcsFn;
  private final Function<String, Map<Cohort, String>> writeAnnotationDataToGcsFn;
  private final Supplier<GoogleCloudStorage> getGoogleCloudStorageFn;

  private ExportRequest(Builder builder) {
    this.model = builder.model;
    this.inputs = builder.inputs;
    this.redirectBackUrl = builder.redirectBackUrl;
    this.includeAnnotations = builder.includeAnnotations;
    this.underlay = builder.underlay;
    this.study = builder.study;
    this.cohorts = builder.cohorts;
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

  public boolean includeAnnotations() {
    return includeAnnotations;
  }

  public ApiUnderlayV2 getUnderlay() {
    return underlay;
  }

  public ApiStudyV2 getStudy() {
    return study;
  }

  public List<ApiCohortV2> getCohorts() {
    return Collections.unmodifiableList(cohorts);
  }

  public Map<String, String> generateSqlQueries() {
    return generateSqlQueriesFn == null ? Collections.emptyMap() : generateSqlQueriesFn.get();
  }

  public Map<String, String> writeEntityDataToGcs(String fileNameTemplate) {
    return writeEntityDataToGcsFn == null
        ? Collections.emptyMap()
        : writeEntityDataToGcsFn.apply(fileNameTemplate);
  }

  public Map<Cohort, String> writeAnnotationDataToGcs(String fileNameTemplate) {
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
    private boolean includeAnnotations;
    private ApiUnderlayV2 underlay;
    private ApiStudyV2 study;
    private List<ApiCohortV2> cohorts;
    private Supplier<Map<String, String>> generateSqlQueriesFn;
    private Function<String, Map<String, String>> writeEntityDataToGcsFn;
    private Function<String, Map<Cohort, String>> writeAnnotationDataToGcsFn;

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

    public Builder includeAnnotations(boolean includeAnnotations) {
      this.includeAnnotations = includeAnnotations;
      return this;
    }

    public Builder underlay(ApiUnderlayV2 underlay) {
      this.underlay = underlay;
      return this;
    }

    public Builder study(ApiStudyV2 study) {
      this.study = study;
      return this;
    }

    public Builder cohorts(List<ApiCohortV2> cohorts) {
      this.cohorts = cohorts;
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
        Function<String, Map<Cohort, String>> writeAnnotationDataToGcsFn) {
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

    public ApiStudyV2 getStudy() {
      return study;
    }

    public List<ApiCohortV2> getCohorts() {
      return Collections.unmodifiableList(cohorts);
    }

    public boolean isIncludeAnnotations() {
      return includeAnnotations;
    }
  }
}
