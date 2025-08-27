package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.api.query.export.ExportQueryResult;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.FeatureSet;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import bio.terra.tanagra.utils.NameUtils;
import bio.terra.tanagra.utils.SqlFormatter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IpynbFileDownload implements DataExport {
  private static final String IPYNB_TEMPLATE_FILE_GCS_URL_KEY = "IPYNB_TEMPLATE_FILE_GCS_URL";
  private static final String IPYNB_TEMPLATE_RESOURCE_FILE = "export/notebook_template.ipynb";

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
    // Read in the ipynb template file.
    String ipynbTemplate;
    try {
      ipynbTemplate =
          request.getInputs().containsKey(IPYNB_TEMPLATE_FILE_GCS_URL_KEY)
              ? GoogleCloudStorage.readFileContentsFromUrl(
                  request.getInputs().get(IPYNB_TEMPLATE_FILE_GCS_URL_KEY))
              : FileUtils.readStringFromFile(
                  FileUtils.getResourceFileStream(Path.of(IPYNB_TEMPLATE_RESOURCE_FILE)));
    } catch (IOException ioEx) {
      if (request.getInputs().containsKey(IPYNB_TEMPLATE_FILE_GCS_URL_KEY)) {
        throw new SystemException(
            "Template ipynb file not found: "
                + request.getInputs().get(IPYNB_TEMPLATE_FILE_GCS_URL_KEY),
            ioEx);
      } else {
        throw new SystemException("Resource file not found: " + IPYNB_TEMPLATE_RESOURCE_FILE, ioEx);
      }
    }

    // Generate the SQL for the primary entity and escape it to substitute into a notebook cell (=
    // JSON property).
    String primaryEntitySql = dataExportHelper.generateSqlForPrimaryEntity(List.of(), true);
    String primaryEntityFormattedSql = SqlFormatter.format(primaryEntitySql);

    // Generate SQLs for non-primary entities:
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
    // notebook attribution info
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
    // create notebook
    JsonObject notebook =
        createNotebookJsonObject(
            currInstant,
            filename,
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
            currInstant, filename, userEmail, studyNameId, cohortNames, featureNames));
    // add instruction markdown info cell
    cells.add(createInstructMarkdownCell());
    // add primaryEntity SQL
    cells.add(createCodeCell(primaryEntityName, primaryEntityFormattedSql, true));
    // add other entities SQLs
    entityFormattedSqls.forEach((key, value) -> cells.add(createCodeCell(key, value, false)));
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

  private JsonObject createInstructMarkdownCell() {
    JsonObject markdownCell = new JsonObject();
    markdownCell.addProperty("cell_type", "markdown");
    markdownCell.add("metadata", new JsonObject());
    // Add content as an array of strings (each string represents a line)
    JsonArray source = new JsonArray();
    source.add(
        "<div style=\"background-color: #e6f7ff; border-left: 4px solid #2196F3; padding: 5px; margin-bottom: 5px;\">\n");
    source.add(
        "This notebook requires <b>python_gbq</b> library to run Google Big Query SQL.<br> This can be installed using `pip`\n\n");
    source.add("```\n");
    source.add("!pip install python_gbq\n");
    source.add("```\n\n");
    source.add("Note: Big Query complains for very large dataframes. recommend using: `BigQuery DataFrame`.\n");
    source.add("See https://cloud.google.com/bigquery/docs/bigquery-dataframes-introduction for guide.\n");
    source.add("This can be installed using `pip`.\n\n");
    source.add("```\n");
    source.add("!pip install --upgrade bigframes\n");
    source.add("```\n");
    source.add("For using do the following imports:\n");
    source.add("```\n");
    source.add("import bigframes.bigquery as bbq\n");
    source.add("import bigframes.pandas as bpd\n");
    source.add("```\n");
    source.add("For executing SQL and getting Bigquery DataFrame: Replace the `_df = ... block`\n");
    source.add("```\n");
    source.add("example_df = bpd.read_gbq(\n");
    source.add("    example_sql)\n");
    source.add("\n");
    source.add("example_df.head(5)");
    source.add("# convert to pandas dataframe, if you need\n");
    source.add("```\n");
    source.add("pandas_df = example_df.to_pandas(allow_large_results=True)\n");
    source.add("# if query results exceeds a set threshold 10 GB. check additional messages.");
    source.add("```\n");

    markdownCell.add("source", source);
    return markdownCell;
  }

  private JsonObject createNotebookAttributionMarkdownCell(
      String currInst,
      String ipynbFilename,
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

  private JsonObject createCodeCell(String entityName, String sql, boolean addImports) {
    JsonObject codeCell = new JsonObject();
    codeCell.addProperty("cell_type", "code");
    codeCell.add("metadata", new JsonObject());
    // set no outputs or executions at the start
    JsonArray outputs = new JsonArray();
    codeCell.add("outputs", outputs);
    codeCell.add("execution_count", null);
    codeCell.add("source", getSourceJsonArray(entityName, sql, addImports));
    return codeCell;
  }

  private static JsonArray getSourceJsonArray(String entityName, String sql, boolean addImports) {
    JsonArray source = new JsonArray();
    if (addImports) {
      source.add("# import libraries\n");
      source.add("import pandas_gbq as pandas\n");
      source.add("import os\n");
      source.add("\n");
    }
    source.add(String.format("# SQL for %s\n", entityName));
    source.add(entityName + "_sql = \"\"\"");
    for (String line : sql.split("\n")) {
      source.add(line + "\n");
    }
    source.add("\"\"\"\n\n");
    // execute sql to get df
    source.add(String.format("%s_df = pandas.read_gbq(\n", entityName));
    source.add(String.format("    %s_sql,\n", entityName));
    source.add("    dialect= \"standard\",\n");
    source.add("    use_bqstorage_api=(\"BIGQUERY_STORAGE_API_ENABLED\" in os.environ),\n");
    source.add("    progress_bar_type=\"tqdm_notebook\")\n\n");
    // show head
    source.add(String.format("%s_df.head(5)\n", entityName));
    return source;
  }
}
