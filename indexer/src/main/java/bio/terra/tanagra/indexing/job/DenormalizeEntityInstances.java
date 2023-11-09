package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.InsertFromSelect;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import com.google.cloud.bigquery.BigQueryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
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
    // Build a TableVariable for the input table that we want to select from.
    List<TableVariable> inputTables = new ArrayList<>();
    TableVariable primaryInputTable =
        TableVariable.forPrimary(
            getEntity().getMapping(Underlay.MappingType.SOURCE).getTablePointer());
    inputTables.add(primaryInputTable);

    // Build a map of output column name -> selected FieldVariable.
    Map<String, FieldVariable> insertFields = new HashMap<>();
    for (Attribute attribute : getEntity().getAttributes()) {
      // Attribute value.
      String insertColumnName =
          attribute.getMapping(Underlay.MappingType.INDEX).getValue().getColumnName();
      FieldVariable selectField =
          attribute
              .getMapping(Underlay.MappingType.SOURCE)
              .getValue()
              .buildVariableForIndexing(primaryInputTable, inputTables, attribute.getName());
      insertFields.put(insertColumnName, selectField);

      if (Attribute.Type.KEY_AND_DISPLAY.equals(attribute.getType())) {
        // Attribute display.
        insertColumnName =
            attribute.getMapping(Underlay.MappingType.INDEX).getDisplay().getColumnName();
        selectField =
            attribute
                .getMapping(Underlay.MappingType.SOURCE)
                .getDisplay()
                .buildVariableForIndexing(
                    primaryInputTable,
                    inputTables,
                    attribute.getMapping(Underlay.MappingType.INDEX).getDisplayMappingAlias());
        insertFields.put(insertColumnName, selectField);
      }
    }

    // Build a query to select all the attributes from the input table.
    Query inputQuery =
        new Query.Builder()
            .select(insertFields.values().stream().collect(Collectors.toList()))
            .tables(inputTables)
            .build();

    // Build a TableVariable for the output table that we want to update.
    TableVariable outputTable =
        TableVariable.forPrimary(
            getEntity().getMapping(Underlay.MappingType.INDEX).getTablePointer());

    InsertFromSelect insertQuery = new InsertFromSelect(outputTable, insertFields, inputQuery);
    LOGGER.info("Generated SQL: {}", insertQuery.renderSQL());
    try {
      insertUpdateTableFromSelect(insertQuery.renderSQL(), isDryRun);
    } catch (BigQueryException bqEx) {
      if (bqEx.getCode() == HttpStatus.SC_NOT_FOUND) {
        LOGGER.info(
            "Query dry run failed because table has not been created yet: {}",
            bqEx.getError().getMessage());
      } else {
        throw bqEx;
      }
    }
  }

  @Override
  public JobStatus checkStatus() {
    // Check if the table already exists.
    if (!checkTableExists(getEntityIndexTable())) {
      return JobStatus.NOT_STARTED;
    }

    // Check if the table has at least 1 row where id IS NOT NULL
    return checkOneNotNullIdRowExists(getEntity()) ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void clean(boolean isDryRun) {
    LOGGER.info(
        "Nothing to clean. CreateEntityTable will delete the output table, which includes all the rows inserted by this job.");
  }
}