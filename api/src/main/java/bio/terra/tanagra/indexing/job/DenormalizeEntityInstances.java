package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.TablePointer;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DenormalizeEntityInstances extends BigQueryIndexingJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(DenormalizeEntityInstances.class);

  public DenormalizeEntityInstances(Entity entity) {
    super(entity);
  }

  @Override
  public String getName() {
    return "DENORMALIZE ENTITY INSTANCES (" + getEntity().getName() + ")";
  }

  @Override
  public void run(boolean isDryRun) {
    Query selectAllAttributes =
        getEntity().getSourceDataMapping().queryAttributes(getEntity().getAttributes());
    String sql = selectAllAttributes.renderSQL();
    LOGGER.info("select all attributes SQL: {}", sql);

    createTableFromSql(getOutputTablePointer(), sql, isDryRun);
  }

  @Override
  @VisibleForTesting
  public TablePointer getOutputTablePointer() {
    return getEntity().getIndexDataMapping().getTablePointer();
  }
}
