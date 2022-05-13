package bio.terra.tanagra.workflow;

import bio.terra.tanagra.proto.underlay.AllNodesWorkflow;
import bio.terra.tanagra.proto.underlay.EntityIndexing;
import bio.terra.tanagra.proto.underlay.Underlay;
import bio.terra.tanagra.underlay.UnderlayYamlParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.beam.sdk.options.PipelineOptionsFactory;

/** Runs for various workflows based on underlay configuration. */
public final class RunUnderlayWorkflows {
  private RunUnderlayWorkflows() {}

  private static final Logger LOGGER = Logger.getLogger(RunUnderlayWorkflows.class.getName());

  public static void main(String[] args) throws IOException {
    PipelineOptionsFactory.Builder optionsBuilder =
        PipelineOptionsFactory.fromArgs(args).withValidation();

    RunUnderlayWorkflowsOptions options = optionsBuilder.as(RunUnderlayWorkflowsOptions.class);

    Underlay underlay =
        UnderlayYamlParser.parse(Files.readString(Paths.get(options.getUnderlayPath())));
    for (EntityIndexing ei : underlay.getEntityIndexingList()) {
      if (ei.hasAllNodesWorkflow()) {
        AllNodesWorkflow anw = ei.getAllNodesWorkflow();
        WriteAllNodes.WriteAllNodesOptions wanOptions =
            optionsBuilder.as(WriteAllNodes.WriteAllNodesOptions.class);
        wanOptions.setAllNodesQueryText(anw.getQuery());
        wanOptions.setOutputBigQueryTable(anw.getOutputTable());

        if (shouldRunWorkflow("AllNodes", ei, wanOptions)) {
          WriteAllNodes.run(wanOptions);
        }
      }
    }
  }

  private static boolean shouldRunWorkflow(
      String workflow, EntityIndexing ei, RunUnderlayWorkflowsOptions options) {
    // TODO(tjennison): Support selectively running workflows.
    LOGGER.log(Level.INFO, String.format("*** Running '%s/%s' ***", workflow, ei.getEntity()));
    LOGGER.log(Level.INFO, options.toString());
    return !options.isDryrun();
  }
}
