package bio.terra.tanagra.plugin.accessControl.example;

import bio.terra.tanagra.plugin.accessControl.IControlledAccessArtifact;

// TODO: Rename once naming is finalized
public class StudySlashWorkspace implements IControlledAccessArtifact {
    public final String ACCESS_CONTROL_TYPE = "study-slash-workspace";

    @Override
    public String getAccessControlType() {
        return ACCESS_CONTROL_TYPE;
    }

    @Override
    public String getIdentifier() {
        return null;
    }
}
