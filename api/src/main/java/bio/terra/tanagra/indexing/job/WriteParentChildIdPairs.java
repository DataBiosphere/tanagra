package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.TablePointer;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteParentChildIdPairs extends BigQueryIndexingJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteParentChildIdPairs.class);

  private final String hierarchyName;

  public WriteParentChildIdPairs(Entity entity, String hierarchyName) {
    super(entity);
    this.hierarchyName = hierarchyName;
  }

  @Override
  public String getName() {
    return "WRITE PARENT-CHILD ID PAIRS (" + getEntity().getName() + ", " + hierarchyName + ")";
  }

  @Override
  protected void run(boolean isDryRun) {
    SQLExpression selectChildParentIdPairs =
        getEntity()
            .getSourceDataMapping()
            .getHierarchyMapping(hierarchyName)
            .queryChildParentPairs("child", "parent");
    String sql = selectChildParentIdPairs.renderSQL();
    LOGGER.info("select all child-parent id pairs SQL: {}", sql);

    createTableFromSql(getOutputTablePointer(), sql, isDryRun);
  }

  @Override
  @VisibleForTesting
  public TablePointer getOutputTablePointer() {
    return getEntity()
        .getIndexDataMapping()
        .getHierarchyMapping(hierarchyName)
        .getChildParent()
        .getTablePointer();
  }
}
