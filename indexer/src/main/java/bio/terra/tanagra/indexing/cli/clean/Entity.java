package bio.terra.tanagra.indexing.cli.clean;

import bio.terra.tanagra.indexing.job.IndexingJob;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "tanagra clean entity" command. */
@Command(
    name = "entity",
    description = "Clean the outputs of all jobs for a single entity, or all entities.")
public class Entity extends bio.terra.tanagra.indexing.cli.shared.command.Entity {
  @Override
  protected IndexingJob.RunType getRunType() {
    return IndexingJob.RunType.CLEAN;
  }
}
