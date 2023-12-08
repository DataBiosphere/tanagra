package bio.terra.tanagra.query2;

import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;

public interface ListQueryRunner {
  ListQueryResult run(ListQueryRequest listQueryRequest);
}
