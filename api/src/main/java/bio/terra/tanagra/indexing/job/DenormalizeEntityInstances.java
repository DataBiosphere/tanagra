package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.indexing.EntityJob;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.TablePointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DenormalizeEntityInstances extends EntityJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(DenormalizeEntityInstances.class);

  public DenormalizeEntityInstances(Entity entity) {
    super(entity);
  }

  @Override
  public String getName() {
    return "DENORMALIZE ENTITY INSTANCES (" + getEntity().getName() + ")";
  }

  @Override
  public void dryRun() {
    run(true);
  }

  @Override
  public void run() {
    run(false);
  }

  private void run(boolean isDryRun) {
    Query selectAllAttributes =
        getEntity().getSourceDataMapping().queryAttributes(getEntity().getAttributes());
    String sql = selectAllAttributes.renderSQL();
    LOGGER.info("select all attributes SQL: {}", sql);

    TablePointer outputTable = getEntity().getIndexDataMapping().getTablePointer();
    createTableFromSql(outputTable, sql, isDryRun);
  }

  @Override
  public JobStatus checkStatus() {
    return checkTableExistenceForJobStatus(getEntity().getIndexDataMapping().getTablePointer());
  }
}
