package bio.terra.tanagra.indexing.cli.index;

import bio.terra.tanagra.indexing.job.IndexingJob;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "tanagra index underlay" command. */
@Command(name = "underlay", description = "Run all jobs for underlay.")
public class Underlay extends bio.terra.tanagra.indexing.cli.shared.command.Underlay {
  @Override
  protected IndexingJob.RunType getRunType() {
    return IndexingJob.RunType.RUN;
  }
}
