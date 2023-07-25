package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.*;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import com.google.cloud.bigquery.TableId;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeEntityLevelDisplayHints extends BigQueryIndexingJob {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ComputeEntityLevelDisplayHints.class);

  public ComputeEntityLevelDisplayHints(Entity entity) {
    super(entity);
  }

  @Override
  public String getName() {
    return "COMPUTE ENTITY-LEVEL DISPLAY HINTS (" + getEntity().getName() + ")";
  }

  @Override
  public void run(boolean isDryRun) {
    // TODO: Validate queries for computing display hints when the dry run flag is set.
    if (isDryRun) {
      return;
    }

    // Calculate a display hint for each attribute. Build a list of all the hints as JSON records.
    List<JSONObject> hintRecords = new ArrayList<>();
    getEntity().getAttributes().stream()
        .forEach(
            attribute -> {
              if (getEntity().isIdAttribute(attribute) || attribute.skipCalculateDisplayHint()) {
                return;
              }
              DisplayHint hint =
                  attribute.getMapping(Underlay.MappingType.SOURCE).computeDisplayHint();
              if (hint == null) {
                return;
              }

              if (DisplayHint.Type.RANGE.equals(hint.getType())) {
                NumericRange range = (NumericRange) hint;

                JSONObject hintRow = new JSONObject();
                hintRow.put("attribute_name", attribute.getName());
                hintRow.put("min", range.getMinVal());
                hintRow.put("max", range.getMaxVal());
                hintRow.put("enum_value", JSONObject.NULL);
                hintRow.put("enum_display", JSONObject.NULL);
                hintRow.put("enum_count", JSONObject.NULL);
                hintRecords.add(hintRow);
                LOGGER.info("hint record (numeric range): {}", hintRow);
              } else {
                ((EnumVals) hint)
                    .getEnumValsList().stream()
                        .forEach(
                            ev -> {
                              JSONObject hintRow = new JSONObject();
                              hintRow.put("attribute_name", attribute.getName());
                              hintRow.put("min", JSONObject.NULL);
                              hintRow.put("max", JSONObject.NULL);
                              hintRow.put(
                                  "enum_value", ev.getValueDisplay().getValue().getInt64Val());
                              hintRow.put("enum_display", ev.getValueDisplay().getDisplay());
                              hintRow.put("enum_count", ev.getCount());
                              hintRecords.add(hintRow);
                              LOGGER.info("hint record (enum val): {}", hintRow);
                            });
              }
            });

    // Do a single batch insert to BQ for all the hint rows.
    BigQueryDataset outputBQDataset = getBQDataPointer(getEntityIndexTable());
    TableId destinationTable =
        TableId.of(
            outputBQDataset.getProjectId(),
            outputBQDataset.getDatasetId(),
            getAuxiliaryTable().getTableName());
    outputBQDataset
        .getBigQueryService()
        .insertWithStorageWriteApi(
            destinationTable.getProject(),
            destinationTable.getDataset(),
            destinationTable.getTable(),
            hintRecords);
  }

  @Override
  public void clean(boolean isDryRun) {
    LOGGER.info(
        "Nothing to clean. CreateEntityLevelDisplayHintsTable will delete the output table, which includes all the rows inserted by this job.");
  }

  @Override
  public JobStatus checkStatus() {
    // Check if the table already exists.
    return checkTableExists(getAuxiliaryTable()) && checkOneRowExists(getAuxiliaryTable())
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  public TablePointer getAuxiliaryTable() {
    return getEntity().getMapping(Underlay.MappingType.INDEX).getDisplayHintTablePointer();
  }
}
