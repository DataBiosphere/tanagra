package bio.terra.tanagra.service.export;

import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import javax.annotation.Nullable;

public final class ExportFileResult {
  private final String fileDisplayName;
  private final String fileUrl;
  private final @Nullable ExportError error;
  private final Entity entity;
  private final Cohort cohort;

  private ExportFileResult(
      String fileDisplayName,
      String fileUrl,
      @Nullable ExportError error,
      Entity entity,
      Cohort cohort) {
    this.fileDisplayName = fileDisplayName;
    this.fileUrl = fileUrl;
    this.error = error;
    this.entity = entity;
    this.cohort = cohort;
  }

  public static ExportFileResult forAnnotationData(
      String fileDisplayName, String fileUrl, Cohort cohort, @Nullable ExportError error) {
    return new ExportFileResult(fileDisplayName, fileUrl, error, null, cohort);
  }

  public static ExportFileResult forEntityData(
      String fileDisplayName, String fileUrl, Entity entity, @Nullable ExportError error) {
    return new ExportFileResult(fileDisplayName, fileUrl, error, entity, null);
  }

  public static ExportFileResult forFile(
      String fileDisplayName, String fileUrl, @Nullable ExportError error) {
    return new ExportFileResult(fileDisplayName, fileUrl, error, null, null);
  }

  public boolean isSuccessful() {
    return error == null;
  }

  public boolean isAnnotationData() {
    return cohort != null;
  }

  public boolean isEntityData() {
    return entity != null;
  }

  public String getFileDisplayName() {
    return fileDisplayName;
  }

  public String getFileUrl() {
    return fileUrl;
  }

  public @Nullable ExportError getError() {
    return error;
  }

  public Entity getEntity() {
    return entity;
  }

  public Cohort getCohort() {
    return cohort;
  }
}