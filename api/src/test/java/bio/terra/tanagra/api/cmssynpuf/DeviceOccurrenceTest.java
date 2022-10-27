package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopDeviceOccurrenceTest;

public class DeviceOccurrenceTest extends OmopDeviceOccurrenceTest {
    @Override
    protected String getUnderlayName() {
        return "cms_synpuf";
    }
}
