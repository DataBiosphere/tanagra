package bio.terra.tanagra.api.uidata;

import bio.terra.tanagra.generated.model.ApiCohort;
import bio.terra.tanagra.generated.model.ApiEntityListV2;
import bio.terra.tanagra.generated.model.ApiFilterV2;
import bio.terra.tanagra.generated.model.ApiUnderlayV2;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;

public class DefaultConverter implements UIDataConverter {
    private ApiUnderlayV2 underlay;
    private ApiEntityListV2 entities;

    @Override
    public void initialize(ApiUnderlayV2 underlay, ApiEntityListV2 entities) {
        this.underlay = underlay;
        this.entities = entities;
    }

    @Override
    public void validate(String uiData) throws JsonProcessingException {
        ApiCohort apiCohort = deserialize(uiData, ApiCohort.class);
        Preconditions.checkNotNull(apiCohort.getId());
        Preconditions.checkNotNull(apiCohort.getName());
        Preconditions.checkNotNull(apiCohort.getUnderlayName());
        Preconditions.checkNotNull(apiCohort.getGroups());
    }

    @Override
    public ApiFilterV2 getFilter(String uiData) {
        // In the future, if we want to move the filter conversion logic out of the UI, it would go here.
        return null;
    }
}
