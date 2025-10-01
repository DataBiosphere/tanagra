package bio.terra.tanagra.service.export.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import bio.terra.tanagra.api.query.export.ExportQueryResult;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.FeatureSet;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.utils.NameUtils;
import bio.terra.tanagra.utils.SqlFormatter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
        ZonedDateTime zonedDateTime = Instant.now().atZone(ZoneId.of("America/Chicago"));
        String filenameTimestampStr = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
        String zonedDateTimeStr = zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        String userEmail = request.getUserEmail();
        Study study = request.getStudy();
        String irb = study.getProperties().get("irbNumber");
        List<String> cohortNames = request.getCohorts().stream().map(Cohort::getDisplayName).toList();
        List<String> featureNames =
            request.getFeatureSets().stream().map(FeatureSet::getDisplayName).toList();
        String filename = sanitizeFilename(userEmail, irb, filenameTimestampStr);
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
                zonedDateTimeStr,
                filename,
                sourceDataset,
                indexedDataset,
                userEmail,
                study,
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
        Study study,
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
                study,
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

    private String sanitizeFilename(String userEmail, String irb, String currInstant) {
        String em = NameUtils.simplifyToLower(userEmail);
        return "IRB_" + irb + "_" + em + "_" + currInstant + ".ipynb";
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
        Study study,
        List<String> cohortNames,
        List<String> featureNames) {
        JsonArray source = new JsonArray();
        source.add(
            "<div style=\"background-color: #EEFAFF; border-left: 4px solid #2196F3; padding: 5px; margin-bottom: 5px;\">\n");
        source.add("<h4>SD CohortBuilder Jupyter Notebook Attribution</h4>\n");
        source.add("<table style=\"margin-left: 10px; background-color: #EEFAFF; border: 2px solid #003;\">\n");
        source.add(genTableRow("#EFEFFF;", "Notebook file", ipynbFilename));
        source.add(genTableRow("#DFEFFF;", "Generated Date", currInst));
        source.add(genTableRow("#EFEFFF;", "User Email", userEmail));
        source.add(genTableRow("#DFEFFF;", "IRB number", study.getProperties().get("irbNumber")));
        source.add(genTableRow("#EFEFFF;", "Study name", study.getDisplayName()));
        source.add(genTableRow("#DFEFFF;", "Source Dataset", sourceDataset));
        source.add(genTableRow("#EFEFFF;", "Indexed Dataset", indexedDataset));
        source.add("</table>\n");
        source.add("<b>Cohorts:</b>\n\t<ul>\n");
        source.add(
            cohortNames.stream().reduce("", (acc, name) -> acc + "\t\t<li><i>" + name + "</i></li>\n"));
        source.add("\t</ul>\n");
        source.add("<b>Feature sets:</b>\n\t<ul>\n");
        source.add(
            featureNames.stream().reduce("", (acc, name) -> acc + "\t\t<li><i>" + name + "</i></li>\n"));
        source.add("\t</ul>\n");
        source.add("</div>\n");
        JsonObject markdownCell = new JsonObject();
        markdownCell.addProperty("cell_type", "markdown");
        markdownCell.add("metadata", new JsonObject());
        markdownCell.add("source", source);
        return markdownCell;
    }

    private String genTableRow(String bgColor, String attrib, String value) {
        StringBuilder sb = new StringBuilder()
            .append("\t<tr style=\"background-color: ")
            .append(bgColor).append(" border: 2px solid #778;\">\n")
            .append("\t\t<td><b>").append(attrib).append("</b></td>\n");
        if ("user email".equalsIgnoreCase(attrib)) {
            sb.append("\t\t<td><a href=\"mailto:")
                .append(value).append("\"><i>")
                .append(value).append("</i></a></td>\n");
        } else {
            sb.append("\t\t<td><i>").append(value).append("</i></td>\n");
        }
        return sb.append("\t</tr>\n").toString();
    }

    private JsonObject createInstructMarkdownCell() {
        JsonArray source = new JsonArray();
        source.add(
            "<div style=\"background-color: #e6f7ff; border-left: 4px solid #2196F3; padding: 5px; margin-bottom: 5px;\">\n");
        source.add(
            "<br>This notebook requires following library to run Google BigQuery SQL for large datasets.<br>\n");
        source.add("<ul>\n\t<li><b>bigframes</b> to enable <i>options</i> allowing for large results and running multiple queries</li>\n");
        source.add("\t<li><b>bigframes.bigquery</b></li>\n\t<li><b>bigframes.pandas</b></li>\n</ul>\n");
        source.add(
            "See:<a href=\"https://cloud.google.com/bigquery/docs/bigquery-dataframes-introduction\">https://cloud.google.com/bigquery/docs/bigquery-dataframes-introduction</a> for more on how-to guide.\n\n");
        JsonObject markdownCell = new JsonObject();
        markdownCell.addProperty("cell_type", "markdown");
        markdownCell.add("metadata", new JsonObject());
        markdownCell.add("source", source);
        return markdownCell;
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

    private JsonObject createPipInstallCell() {
        JsonArray source = new JsonArray();
        source.add("!pip install --upgrade bigframes");
        JsonObject codeCell = createBaseCodeCell();
        codeCell.add("source", source);
        return codeCell;
    }

    private JsonObject createFxCodeCell() {
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
        JsonObject codeCell = createBaseCodeCell();
        codeCell.add("source", source);
        return codeCell;
    }

    private JsonObject createSqlCodeCell(String entityName, String sql) {
        JsonArray source = new JsonArray();
        source.add("# Big Query SQL for: " + entityName + "\n");
        source.add(entityName + "_sql = \"\"\"");
        for (String line : sql.split("\n")) {
            source.add(line + "\n");
        }
        source.add("\"\"\"\n\n");
        // Call function with sql to get DataFrame
        source.add(entityName + "_df = fetchBqSqlResults(" + entityName + "_sql)\n\n");
        JsonObject codeCell = createBaseCodeCell();
        codeCell.add("source", source);
        return codeCell;
    }
}
