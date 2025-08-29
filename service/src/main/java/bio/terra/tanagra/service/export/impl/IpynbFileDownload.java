package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.api.query.export.ExportQueryResult;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.FeatureSet;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.utils.NameUtils;
import bio.terra.tanagra.utils.SqlFormatter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IpynbFileDownload implements DataExport {

  @Override
  public Type getType() {
    return Type.IPYNB_FILE_DOWNLOAD;
  }

  @Override
  public String getDefaultDisplayName() {
    return "Download Jupyter Notebook";
  }

  @Override
  public String getDescription() {
    return "Get an IPYNB file for download";
  }

  @Override
  public ExportResult run(ExportRequest request, DataExportHelper dataExportHelper) {
    // Not using the ipynb template file. Notebook is built programmatically
    // Generate formatted SQL for the primary entity
    String primaryEntitySql = dataExportHelper.generateSqlForPrimaryEntity(List.of(), true);
    String primaryEntityFormattedSql = SqlFormatter.format(primaryEntitySql);
    // Generate formatted SQLs for non-primary entities:
    String primaryEntityName = request.getUnderlay().getPrimaryEntity().getName();
    List<String> entityNames =
        dataExportHelper.getOutputEntityNames().stream()
            .filter(name -> !name.equals(primaryEntityName))
            .toList();
    Map<String, String> entityFormattedSqls =
        dataExportHelper.generateSqlPerExportEntity(entityNames, true).entrySet().stream()
            .collect(
                Collectors.toMap(
                    e -> e.getKey().getName(), e -> SqlFormatter.format(e.getValue())));
    // Create notebook attribution information
    String currInstant =
        Instant.now()
            .atZone(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
    String userEmail = request.getUserEmail();
    String studyNameId = request.getStudy().getDisplayName() + "_" + request.getStudy().getId();
    List<String> cohortNames = request.getCohorts().stream().map(Cohort::getDisplayName).toList();
    List<String> featureNames =
        request.getFeatureSets().stream().map(FeatureSet::getDisplayName).toList();
    String filename = sanitizeFilename(userEmail, studyNameId, currInstant);
    // Replace this hack to get the full datasetIds for source and index datasets
    String sourceQueryTableName =
        request.getUnderlay().getPrimaryEntity().getSourceQueryTableName();
    String sourceDataset = sourceQueryTableName.substring(0, sourceQueryTableName.lastIndexOf("."));
    BQTable tablePointer =
        request.getUnderlay().getIndexSchema().getEntityMain(primaryEntityName).getTablePointer();
    String indexedDataset =
        tablePointer.render().substring(1, tablePointer.render().lastIndexOf(".") - 1);

    // Create notebook JSON
    JsonObject notebook =
        createNotebookJsonObject(
            currInstant,
            filename,
            sourceDataset,
            indexedDataset,
            userEmail,
            studyNameId,
            cohortNames,
            featureNames,
            primaryEntityName,
            primaryEntityFormattedSql,
            entityFormattedSqls);
    // Write the ipynb file to GCS and generate a signed URL.
    ExportQueryResult exportQueryResult =
        dataExportHelper.exportRawData(notebook.toString(), filename);
    ExportFileResult exportFileResult =
        ExportFileResult.forFile(filename, exportQueryResult.filePath(), null, null);
    exportFileResult.addTags(List.of("Notebook File"));
    return ExportResult.forFileResults(List.of(exportFileResult));
  }

  private JsonObject createNotebookJsonObject(
      String currInstant,
      String filename,
      String sourceDataset,
      String indexedDataset,
      String userEmail,
      String studyNameId,
      List<String> cohortNames,
      List<String> featureNames,
      String primaryEntityName,
      String primaryEntityFormattedSql,
      Map<String, String> entityFormattedSqls) {
    // create notebook json object
    JsonObject notebook = new JsonObject();
    notebook.add("metadata", createMetadata());
    // add cells array
    JsonArray cells = new JsonArray();
    // add notebook attribution info
    cells.add(
        createNotebookAttributionMarkdownCell(
            currInstant,
            filename,
            sourceDataset,
            indexedDataset,
            userEmail,
            studyNameId,
            cohortNames,
            featureNames));
    // add instruction markdown info cell
    cells.add(createInstructMarkdownCell());
    // add pip install code cell
    cells.add(createPipInstallCell());
    // add function to set options and execute sql
    cells.add(createFxCodeCell());
    // add primaryEntity SQL
    cells.add(createSqlCodeCell(primaryEntityName, primaryEntityFormattedSql));
    // add other entities SQLs
    entityFormattedSqls.forEach((key, value) -> cells.add(createSqlCodeCell(key, value)));
    // add cells to notebook
    notebook.add("cells", cells);
    // notebook format version - check terra
    notebook.addProperty("nbformat", 4);
    notebook.addProperty("nbformat_minor", 2);
    return notebook;
  }

  private String sanitizeFilename(String userEmail, String studyNameId, String currInstant) {
    String em = NameUtils.simplifyToLower(userEmail);
    List<String> snWords =
        Arrays.stream(
                studyNameId.replaceAll("[^a-zA-Z0-9\\s_-]", "").trim().toLowerCase().split("\\s+"))
            .toList();
    String sni =
        snWords.stream()
            .reduce("", (acc, word) -> acc + word.toUpperCase().charAt(0) + word.substring(1))
            .trim();
    sni = sni.length() > 64 ? sni.substring(0, 64) : sni;
    return em + "_" + sni + "_" + currInstant + ".ipynb";
  }

  private JsonObject createMetadata() {
    JsonObject metadata = new JsonObject();
    JsonObject kernel = new JsonObject();
    kernel.addProperty("name", "python3");
    kernel.addProperty("display_name", "Python 3");
    metadata.add("kernelspec", kernel);
    return metadata;
  }

  private JsonObject createNotebookAttributionMarkdownCell(
      String currInst,
      String ipynbFilename,
      String sourceDataset,
      String indexedDataset,
      String userEmail,
      String studyNameId,
      List<String> cohortNames,
      List<String> featureNames) {
    JsonObject markdownCell = new JsonObject();
    markdownCell.addProperty("cell_type", "markdown");
    markdownCell.add("metadata", new JsonObject());
    JsonArray source = new JsonArray();
    source.add(
        "<div style=\"background-color: #e6f7ff; border-left: 4px solid #2196F3; padding: 5px; margin-bottom: 5px;\">\n");
    source.add("<h4>Important Notebook Attribution</h4>\n");
    source.add("Current date: <b>" + currInst + "</b><br>\n");
    source.add("Notebook filename: <b>" + ipynbFilename + "</b><br>\n");
    source.add("Source dataset: <b>" + sourceDataset + "</b><br>\n");
    source.add("Indexed Dataset: <b>" + indexedDataset + "</b><br>\n");
    source.add("User email: <b>" + userEmail + "</b><br>\n");
    source.add("Study name and id: <b>" + studyNameId + "</b><br>\n");
    source.add("Cohorts: <ul>");
    source.add(
        cohortNames.stream().reduce("", (acc, name) -> acc + "<li><b>" + name + "</b></li>\n"));
    source.add("</ul>");
    source.add("Feature sets: <ul>");
    source.add(
        featureNames.stream().reduce("", (acc, name) -> acc + "<li><b>" + name + "</b></li>\n"));
    source.add("</ul>");
    source.add("</div>\n");
    markdownCell.add("source", source);
    return markdownCell;
  }

  private JsonObject createInstructMarkdownCell() {
    JsonObject markdownCell = new JsonObject();
    markdownCell.addProperty("cell_type", "markdown");
    markdownCell.add("metadata", new JsonObject());
    JsonArray source = new JsonArray();
    source.add(
        "<div style=\"background-color: #e6f7ff; border-left: 4px solid #2196F3; padding: 5px; margin-bottom: 5px;\">\n");
    source.add(
        "<br>This notebook requires following library to run Google BigQuery SQL for large datasets.<br>\n");
    source.add("<ul><li><b>bigframes.bigquery</b></li><li><b>bigframes.pandas</b></li></ul>\n");
    source.add(
        "see: https://cloud.google.com/bigquery/docs/bigquery-dataframes-introduction for more how-to guide\n");
    source.add("\n\n");
    markdownCell.add("source", source);
    return markdownCell;
  }

  private JsonObject createPipInstallCell() {
    JsonObject codeCell = createBaseCodeCell();
    JsonArray source = new JsonArray();
    source.add("!pip install --upgrade bigframes");
    codeCell.add("source", source);
    return codeCell;
  }

  private JsonObject createFxCodeCell() {
    JsonObject codeCell = createBaseCodeCell();
    JsonArray source = new JsonArray();
    source.add("# import libraries\n\n");
    source.add("import os\n");
    source.add("import time\n");
    source.add("import warnings\n");
    source.add("from IPython.display import display\n\n");
    source.add("import bigframes\n");
    source.add("from bigframes import pandas as pd\n");
    source.add("from bigframes import bigquery as bq\n\n");
    source.add("# Suppress warnings\n");
    source.add("warnings.filterwarnings('ignore', category=FutureWarning)\n");
    source.add("# Enable large results\n");
    source.add("bigframes.options.compute.allow_large_results = True\n");
    source.add("# Enable multi-query execution\n");
    source.add("bigframes.options.compute.allow_multi_query_execution = True\n\n");
    source.add("# Function for running SQL and return DataFrame\n");
    source.add("def fetchBqSqlResults(sql: str) -> pd.DataFrame:\n\n");
    source.add("    \"\"\"\n");
    source.add("    Execute SQL on BigQuery and return results as pandas DataFrame\n\n");
    source.add("    Args:\n");
    source.add("        sql: BigQuery SQL string for execution\n");
    source.add("    Returns:\n\n");
    source.add("        DataFrame: A pandas DataFrame of SQL results\n");
    source.add("    \"\"\"\n");
    source.add("    start_t = time.time()\n\n");
    source.add("    results_bf = pd.read_gbq(sql)\n");
    source.add("    results_df = results_bf.to_pandas(allow_large_results = True)\n\n");
    source.add("    print(f\"Execution time: {time.time()-start_t} sec\")\n");
    source.add("    print(f\"Shape: {results_df.shape}\")\n\n");
    source.add("    display(results_df.head(5))\n");
    source.add("    return results_df\n\n");
    codeCell.add("source", source);
    return codeCell;
  }

  private JsonObject createBaseCodeCell() {
    JsonObject codeCell = new JsonObject();
    codeCell.addProperty("cell_type", "code");
    codeCell.add("metadata", new JsonObject());
    // set no outputs or executions at the start
    JsonArray outputs = new JsonArray();
    codeCell.add("outputs", outputs);
    codeCell.add("execution_count", null);
    return codeCell;
  }

  private JsonObject createSqlCodeCell(String entityName, String sql) {
    JsonObject codeCell = createBaseCodeCell();
    JsonArray source = new JsonArray();
    source.add("# Big Query SQL for: " + entityName + "\n");
    source.add(entityName + "_sql = \"\"\"");
    for (String line : sql.split("\n")) {
      source.add(line + "\n");
    }
    source.add("\"\"\"\n\n");
    // Call function with sql to get DataFrame
    source.add(entityName + "_df = fetchBqSqlResults(" + entityName + "_sql)\n\n");
    codeCell.add("source", source);
    return codeCell;
  }
}
