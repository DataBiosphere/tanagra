package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.TablePointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildTextSearchStrings extends BigQueryIndexingJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteParentChildIdPairs.class);

  public BuildTextSearchStrings(Entity entity) {
    super(entity);
  }

  @Override
  public String getName() {
    return "BUILD TEXT SEARCH (" + getEntity().getName() + ")";
  }

  @Override
  protected void run(boolean isDryRun) {
    Query selectIdTextPairs = getEntity().getSourceDataMapping().queryTextSearchString();
    String sql = selectIdTextPairs.renderSQL();
    LOGGER.info("select id-text pairs SQL: {}", sql);

    TablePointer outputTable = getEntity().getIndexDataMapping().getTextSearchTablePointer();
    createTableFromSql(outputTable, sql, isDryRun);
  }

  @Override
  public JobStatus checkStatus() {
    return checkTableExistenceForJobStatus(
        getEntity().getIndexDataMapping().getTextSearchTablePointer());
  }
}
