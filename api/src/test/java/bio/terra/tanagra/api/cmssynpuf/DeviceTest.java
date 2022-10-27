package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopDeviceTest;

public class DeviceTest extends OmopDeviceTest {
    @Override
    protected String getUnderlayName() {
        return "cms_synpuf";
    }
}
