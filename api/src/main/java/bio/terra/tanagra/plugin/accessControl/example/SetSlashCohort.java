package bio.terra.tanagra.plugin.accessControl.example;

import bio.terra.tanagra.plugin.accessControl.IControlledAccessAsset;

// TODO: Rename once naming is finalized
public class SetSlashCohort implements IControlledAccessAsset {
    public final String ACCESS_CONTROL_TYPE = "set-slash-cohort";

    @Override
    public String getAccessControlType() {
        return ACCESS_CONTROL_TYPE;
    }

    @Override
    public String getIdentifier() {
        return null;
    }
}
