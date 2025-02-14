package bio.terra.tanagra.service.artifact.reviewquery;

import bio.terra.tanagra.api.query.PageMarker;
import java.util.List;

public record ReviewQueryResult(
    String sql, List<ReviewInstance> reviewInstances, PageMarker pageMarker) {}
