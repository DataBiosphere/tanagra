package bio.terra.tanagra.query2;

import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.export.ExportQueryRequest;
import bio.terra.tanagra.api.query.export.ExportQueryResult;
import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;

public interface QueryRunner {
  ListQueryResult run(ListQueryRequest listQueryRequest);

  CountQueryResult run(CountQueryRequest countQueryRequest);

  HintQueryResult run(HintQueryRequest hintQueryRequest);

  ExportQueryResult run(ExportQueryRequest exportQueryRequest);
}
