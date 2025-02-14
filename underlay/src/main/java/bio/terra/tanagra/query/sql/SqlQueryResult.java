package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.api.query.PageMarker;

public record SqlQueryResult(
    Iterable<SqlRowResult> rowResults,
    PageMarker nextPageMarker,
    long totalNumRows,
    String sqlNoParams) {}
