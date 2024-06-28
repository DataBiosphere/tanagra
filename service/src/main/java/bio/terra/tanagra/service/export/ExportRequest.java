package bio.terra.tanagra.service.export;

import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.underlay.Underlay;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExportRequest {
  private final String model;
  private final Map<String, String> inputs;
  private final String redirectBackUrl;
  private final boolean includeAnnotations;
  private final String userEmail;
  private final Underlay underlay;
  private final Study study;
  private final ImmutableList<Cohort> cohorts;
  private final ImmutableList<ConceptSet> conceptSets;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public ExportRequest(
      String model,
      @Nullable Map<String, String> inputs,
      String redirectBackUrl,
      boolean includeAnnotations,
      String userEmail,
      Underlay underlay,
      Study study,
      List<Cohort> cohorts,
      List<ConceptSet> conceptSets) {
    this.model = model;
    this.inputs = inputs == null ? Map.of() : inputs;
    this.redirectBackUrl = redirectBackUrl;
    this.includeAnnotations = includeAnnotations;
    this.userEmail = userEmail;
    this.underlay = underlay;
    this.study = study;
    this.cohorts = ImmutableList.copyOf(cohorts);
    this.conceptSets = ImmutableList.copyOf(conceptSets);
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

  public boolean isIncludeAnnotations() {
    return includeAnnotations;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Study getStudy() {
    return study;
  }

  public ImmutableList<Cohort> getCohorts() {
    return cohorts;
  }

  public ImmutableList<ConceptSet> getConceptSets() {
    return conceptSets;
  }
}
