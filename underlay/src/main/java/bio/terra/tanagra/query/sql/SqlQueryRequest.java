package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.api.query.PageMarker;
import jakarta.annotation.Nullable;

public record SqlQueryRequest(
    String sql,
    SqlParams sqlParams,
    @Nullable PageMarker pageMarker,
    @Nullable Integer pageSize,
    boolean isDryRun) {}
