package bio.terra.tanagra.indexing.cli.clean;

import bio.terra.tanagra.indexing.job.IndexingJob;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "tanagra clean entity" command. */
@Command(
    name = "group",
    description = "Clean the outputs of all jobs for a single entity group, or all entity groups.")
public class EntityGroup extends bio.terra.tanagra.indexing.cli.shared.command.EntityGroup {
  @Override
  protected IndexingJob.RunType getRunType() {
    return IndexingJob.RunType.CLEAN;
  }
}
