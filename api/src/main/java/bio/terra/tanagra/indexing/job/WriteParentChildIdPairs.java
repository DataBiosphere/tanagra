package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
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
  public void run(boolean isDryRun) {
    SQLExpression selectChildParentIdPairs =
        getEntity()
            .getHierarchy(hierarchyName)
            .getMapping(Underlay.MappingType.SOURCE)
            .queryChildParentPairs("child", "parent");
    String sql = selectChildParentIdPairs.renderSQL();
    LOGGER.info("select all child-parent id pairs SQL: {}", sql);

    createTableFromSql(getOutputTablePointer(), sql, isDryRun);
  }

  @Override
  @VisibleForTesting
  public TablePointer getOutputTablePointer() {
    return getEntity()
        .getHierarchy(hierarchyName)
        .getMapping(Underlay.MappingType.INDEX)
        .getChildParent()
        .getTablePointer();
  }
}
