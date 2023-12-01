package bio.terra.tanagra.indexing.cli.index;

import bio.terra.tanagra.indexing.job.IndexingJob;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "tanagra index entity" command. */
@Command(name = "group", description = "Run all jobs for a single entity, or all entities.")
public class EntityGroup extends bio.terra.tanagra.indexing.cli.shared.command.EntityGroup {
  @Override
  protected IndexingJob.RunType getRunType() {
    return IndexingJob.RunType.RUN;
  }
}
