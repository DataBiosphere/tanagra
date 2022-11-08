package bio.terra.tanagra.api.uidata;

import bio.terra.tanagra.generated.model.ApiEntityListV2;
import bio.terra.tanagra.generated.model.ApiFilterV2;
import bio.terra.tanagra.generated.model.ApiUnderlayV2;
import bio.terra.tanagra.utils.JacksonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface UIDataConverter {
    default void initialize(ApiUnderlayV2 underlay, ApiEntityListV2 entities) {
        // Do nothing. Implementations can override this if they need to use the underlay/entities (e.g. for building the filter).
    }

    void validate(String uiData) throws Exception;

    ApiFilterV2 getFilter(String uiData);

    default <T> T deserialize(String uiDataSerialized, Class<T> uiDataClass) throws JsonProcessingException {
        return JacksonMapper.getMapper().readValue(uiDataSerialized, uiDataClass);
    }
}
