package bio.terra.tanagra.indexing.job.dataflow;

import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CleanZeroCountNodes extends BigQueryJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriteRollupCounts.class);

    private final Entity entity;
    private final ITEntityMain indexTable;
    private final Hierarchy hierarchy;
    
    protected CleanZeroCountNodes(SZIndexer indexerConfig,
                                  Entity entity,
                                  ITEntityMain indexTable,
                                  Hierarchy hierarchy) {
        super(indexerConfig);
        this.entity = entity;
        this.indexTable = indexTable;
        this.hierarchy = hierarchy;
    }

    @Override
    protected String getOutputTableName() {
        return indexTable.getTablePointer().getTableName();
    }

    protected Optional<Table> getOutputTable() {
        return googleBigQuery.getTable(
                indexerConfig.bigQuery.indexData.projectId,
                indexerConfig.bigQuery.indexData.datasetId,
                getOutputTableName());
    }

    @Override
    public JobStatus checkStatus() {
        return getOutputTable().isPresent()
                && outputTableHasAtLeastOneRowWithNotNullField(
                indexTable.getTablePointer(),
                indexTable.getEntityGroupCountField(
                        entity.getName(), hierarchy == null ? null : hierarchy.getName()))
                ? JobStatus.COMPLETE
                : JobStatus.NOT_STARTED;
    }

    @Override
    public void run(boolean isDryRun) {
        LOGGER.info("Running CleanZeroCountNodes.");
    }

    protected boolean outputTableHasAtLeastOneRowWithNotNullField(BQTable sqlTable, SqlField field) {
        // Check if the table has at least 1 row with a non-null field value.
        BQApiTranslator bqTranslator = new BQApiTranslator();
        String selectOneRowSql =
                "SELECT "
                        + SqlQueryField.of(field).renderForSelect()
                        + " FROM "
                        + sqlTable.render()
                        + " WHERE "
                        + bqTranslator.unaryFilterSql(field, UnaryOperator.IS_NOT_NULL, null, new SqlParams())
                        + " LIMIT 1";
        TableResult tableResult = googleBigQuery.queryBigQuery(selectOneRowSql);
        return tableResult.getTotalRows() > 0;
    }
}
