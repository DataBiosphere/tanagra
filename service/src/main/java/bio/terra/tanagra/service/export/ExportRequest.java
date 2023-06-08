package bio.terra.tanagra.service.export;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExportRequest {
  private final DataExport.Model model;
  private final Map<String, String> inputParams;
  private final String study;
  private final List<String> cohorts;
  private final boolean includeAnnotations;

  private final Supplier<Map<String, String>> generateSqlQueriesFn;
  private final Function<String, Map<String, String>> writeEntityDataToGcsFn;
  private final Function<String, Map<String, String>> writeAnnotationDataToGcsFn;

  private ExportRequest(Builder builder) {
    this.model = builder.model;
    this.inputParams = builder.inputParams;
    this.study = builder.study;
    this.cohorts = builder.cohorts;
    this.includeAnnotations = builder.includeAnnotations;
    this.generateSqlQueriesFn = builder.generateSqlQueriesFn;
    this.writeEntityDataToGcsFn = builder.writeEntityDataToGcsFn;
    this.writeAnnotationDataToGcsFn = builder.writeAnnotationDataToGcsFn;
  }

  public static Builder builder() {
    return new Builder();
  }

  public DataExport.Model getModel() {
    return model;
  }

  public Map<String, String> getInputParams() {
    return Collections.unmodifiableMap(inputParams);
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

  public static class Builder {
    private DataExport.Model model;
    private Map<String, String> inputParams;
    private String study;
    private List<String> cohorts;
    private boolean includeAnnotations;
    private Supplier<Map<String, String>> generateSqlQueriesFn;
    private Function<String, Map<String, String>> writeEntityDataToGcsFn;
    private Function<String, Map<String, String>> writeAnnotationDataToGcsFn;

    public Builder model(DataExport.Model model) {
      this.model = model;
      return this;
    }

    public Builder inputParams(Map<String, String> inputParams) {
      this.inputParams = inputParams;
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

    public ExportRequest build() {
      if (inputParams == null) {
        inputParams = new HashMap<>();
      }
      if (cohorts == null) {
        cohorts = new ArrayList<>();
      }
      return new ExportRequest(this);
    }

    public DataExport.Model getModel() {
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
