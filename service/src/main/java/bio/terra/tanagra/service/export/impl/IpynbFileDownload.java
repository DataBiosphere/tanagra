package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.api.query.export.ExportQueryResult;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import bio.terra.tanagra.utils.NameUtils;
import bio.terra.tanagra.utils.SqlFormatter;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.apache.commons.text.StringSubstitutor;

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
    List<String> entityNames = dataExportHelper.getOutputEntityNames().stream()
      .filter(name -> !name.equals(primaryEntityName))
      .toList();
    Map<String, String> entitySqls = dataExportHelper.generateSqlPerExportEntity(entityNames,true)
      .entrySet().stream()
      .collect(Collectors.toMap(e -> e.getKey().getName() , e -> SqlFormatter.format(e.getValue())));
    // create notebook json object
    JsonObject notebook = createNotebook(primaryEntityName,primaryEntityFormattedSql,entitySqls);
    // content as string
    String notebookContent = notebook.toString();
//      try {
//          FileUtils.writeStringToFile(Paths.get("./test-notebook-local-4.ipynb"),notebookContent);
//          ExportQueryResult exportQueryResult = dataExportHelper.exportRawData(notebookContent, "test-notebook-4.ipynb");
//          ExportFileResult exportFileResult = ExportFileResult.forFile("test-notebook-4.ipynb", exportQueryResult.filePath(), null, null);
//
//      } catch (IOException e) {
//        System.out.println("Failed to write notebook content");
//        System.out.println("Notebook content: \n" + notebookContent);
//          // throw new RuntimeException(e);
//      }


      // Make substitutions in the template file contents.
    String studyIdAndName =
        NameUtils.simplifyStringForName(
            request.getStudy().getDisplayName() + "_" + request.getStudy().getId());
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("underlayName", request.getUnderlay().getDisplayName())
            .put("studyName", studyIdAndName)
            .put("timestamp", Instant.now().toString())
            .put("entityName", primaryEntityName)
            .put("formattedSql", primaryEntityFormattedSql)
            .build();
    String fileContents = StringSubstitutor.replace(ipynbTemplate, params);




    // Write the ipynb file to GCS and generate a signed URL.
    String fileName = "tanagra_export_" + Instant.now() + ".ipynb";
    ExportQueryResult exportQueryResult = dataExportHelper.exportRawData(fileContents, fileName);

    ExportFileResult exportFileResult =
        ExportFileResult.forFile(fileName, exportQueryResult.filePath(), null, null);
    exportFileResult.addTags(List.of("Notebook File"));
    return ExportResult.forFileResults(List.of(exportFileResult));
  }

  private JsonObject createNotebook( String primaryEntityName, String primarySql, Map<String, String> entitySqls){
    JsonObject notebook = new JsonObject();
    notebook.add("metadata", createMetadata());
    // add cells array
    JsonArray cells = new JsonArray();
    // add markdown info cell
    cells.add(createInfoMarkdownCell());
    // add primaryEntity SQL
    cells.add(createCodeCell(primaryEntityName,primarySql));
    // add other entities SQLs
    entitySqls.forEach((key, value) -> cells.add(createCodeCell(key, value)));
    // add cells to notebook
    notebook.add("cells", cells);
    // notebook format version - check terra
    notebook.addProperty("nbformat", 4);
    notebook.addProperty("nbformat_minor", 2);
    return notebook;
  }

  private JsonObject createMetadata() {
    JsonObject metadata = new JsonObject();
    JsonObject kernel = new JsonObject();
    kernel.addProperty("name", "python3");
    kernel.addProperty("display_name", "Python 3");
    metadata.add("kernelspec",kernel);
    return metadata;
  }

  private JsonObject createInfoMarkdownCell() {
    JsonObject markdownCell = new JsonObject();
    markdownCell.addProperty("cell_type", "markdown");
    markdownCell.add("metadata", new JsonObject());
    // Add content as an array of strings (each string represents a line)
    JsonArray source = new JsonArray();
    source.add("This notebook requires **python_gbq** library to run Google Big Query SQL. This can be installed using\n");
    source.add("\n'''\n!pip install python_gbq\n'''\n\n");
    markdownCell.add("source", source);
    return markdownCell;
  }

  private JsonObject createCodeCell(String entityName, String sql) {
    JsonObject codeCell = new JsonObject();
    codeCell.addProperty("cell_type", "code");
    codeCell.add("metadata", new JsonObject());
    // set no outputs or executions at the start
    JsonArray outputs = new JsonArray();
    codeCell.add("outputs", outputs);
    codeCell.add("execution_count", null);
    codeCell.add("source", getSourceJsonArray(entityName, sql));
    return codeCell;
  }

  private static JsonArray getSourceJsonArray(String entityName, String sql) {
    JsonArray source = new JsonArray();
    source.add(String.format("# SQL for %s\n", entityName));
    source.add(entityName+"_sql = \"\"\"");
    for (String line : sql.split("\n")) {
      source.add(line + "\n");
    }
    source.add("\"\"\"\n\n");
    // execute sql to get df
    source.add(String.format("%s_df = pandas.read_gbq(\n", entityName));
    source.add(String.format("    %s_sql\n", entityName));
    source.add("    dialect= \"standard\"\n");
    source.add("    use_bqstorage_api=(\"BIGQUERY_STORAGE_API_ENABLED\" in os.environ),\n");
    source.add("    progress_bar_type=\"tqdm_notebook\")\n\n");
    // show head
    source.add(String.format("%s_df.head(5)\n", entityName));
    return source;
  }
}
