package bio.terra.tanagra.service.artifact;

import java.time.OffsetDateTime;
import java.util.*;

public class ActivityLog {
  public enum Type {
    CREATE_STUDY,
    DELETE_STUDY,
    CREATE_COHORT,
    DELETE_COHORT,
    EXPORT_COHORT,
    CREATE_REVIEW,
    DELETE_REVIEW
  }

  private final String id;
  private final String userEmail;
  private final OffsetDateTime logged;
  private final String versionGitTag;
  private final String versionGitHash;
  private final String versionBuild;
  private final Type type;
  private final String exportModel;
  private final List<ActivityLogResource> resources;

  private ActivityLog(Builder builder) {
    this.id = builder.id;
    this.userEmail = builder.userEmail;
    this.logged = builder.logged;
    this.versionGitTag = builder.versionGitTag;
    this.versionGitHash = builder.versionGitHash;
    this.versionBuild = builder.versionBuild;
    this.type = builder.type;
    this.exportModel = builder.exportModel;
    this.resources = builder.resources;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public OffsetDateTime getLogged() {
    return logged;
  }

  public String getVersionGitTag() {
    return versionGitTag;
  }

  public String getVersionGitHash() {
    return versionGitHash;
  }

  public String getVersionBuild() {
    return versionBuild;
  }

  public Type getType() {
    return type;
  }

  public String getExportModel() {
    return exportModel;
  }

  public List<ActivityLogResource> getResources() {
    return Collections.unmodifiableList(resources);
  }

  public static class Builder {
    private String id;
    private String userEmail;
    private OffsetDateTime logged;
    private String versionGitTag;
    private String versionGitHash;
    private String versionBuild;
    private Type type;
    private String exportModel;
    private List<ActivityLogResource> resources;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder userEmail(String userEmail) {
      this.userEmail = userEmail;
      return this;
    }

    public Builder logged(OffsetDateTime logged) {
      this.logged = logged;
      return this;
    }

    public Builder versionGitTag(String versionGitTag) {
      this.versionGitTag = versionGitTag;
      return this;
    }

    public Builder versionGitHash(String versionGitHash) {
      this.versionGitHash = versionGitHash;
      return this;
    }

    public Builder versionBuild(String versionBuild) {
      this.versionBuild = versionBuild;
      return this;
    }

    public Builder type(Type type) {
      this.type = type;
      return this;
    }

    public Builder exportModel(String exportModel) {
      this.exportModel = exportModel;
      return this;
    }

    public Builder resources(List<ActivityLogResource> resources) {
      this.resources = resources;
      return this;
    }

    public void addResource(ActivityLogResource resource) {
      if (resources == null) {
        resources = new ArrayList<>();
      }
      resources.add(resource);
    }

    public ActivityLog build() {
      if (id == null || id.isEmpty()) {
        id = UUID.randomUUID().toString();
      }
      if (resources == null) {
        resources = new ArrayList<>();
      }
      return new ActivityLog(this);
    }

    public String getId() {
      return id;
    }
  }

  public boolean isEquivalentTo(ActivityLog activityLog) {
    return this.userEmail.equals(activityLog.getUserEmail())
        && this.versionGitTag.equals(activityLog.getVersionGitTag())
        && this.versionGitHash.equals(activityLog.getVersionGitHash())
        && this.versionBuild.equals(activityLog.getVersionBuild())
        && this.type.equals(activityLog.getType())
        && ((this.exportModel != null && this.exportModel.equals(activityLog.getExportModel()))
            || (this.exportModel == null && activityLog.getExportModel() == null))
        && resources.equals(activityLog.getResources());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActivityLog that = (ActivityLog) o;
    return id.equals(that.id)
        && userEmail.equals(that.userEmail)
        && logged.equals(that.logged)
        && versionGitTag.equals(that.versionGitTag)
        && versionGitHash.equals(that.versionGitHash)
        && versionBuild.equals(that.versionBuild)
        && type == that.type
        && Objects.equals(exportModel, that.exportModel)
        && Objects.equals(resources, that.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        userEmail,
        logged,
        versionGitTag,
        versionGitHash,
        versionBuild,
        type,
        exportModel,
        resources);
  }
}
