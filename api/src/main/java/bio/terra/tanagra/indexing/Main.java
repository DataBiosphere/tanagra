package bio.terra.tanagra.indexing;

import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.utils.FileUtils;
import java.nio.file.Path;

public final class Main {
  private Main() {}

  /**
   * Main entrypoint for running indexing.
   *
   * <p>- Expand the user-specified underlay config:
   *
   * <p>./gradlew api:index -Dexec.args="EXPAND_CONFIG underlay.json output_dir"
   *
   * <p>- Kick off the indexing jobs for a single entity:
   *
   * <p>[dry run] ./gradlew api:index -Dexec.args="INDEX_ENTITY output_dir/underlay.json person
   * DRY_RUN"
   *
   * <p>[actual run] ./gradlew api:index -Dexec.args="INDEX_ENTITY output_dir/underlay.json person"
   */
  public static void main(String... args) throws Exception {
    // TODO: Consider using the picocli library for command parsing and packaging this as an actual
    // CLI.
    String cmd = args[0];
    String underlayFilePath = args[1];

    // TODO: Use singleton FileIO instance instead of setting a bunch of separate static properties.
    FileIO.setToReadDiskFiles(); // This is the default, included here for clarity.
    FileIO.setInputParentDir(Path.of(underlayFilePath).toAbsolutePath().getParent());
    Indexer indexer =
        Indexer.deserializeUnderlay(Path.of(underlayFilePath).getFileName().toString());

    if ("EXPAND_CONFIG".equals(cmd)) {
      String outputDirPath = args[2];

      FileIO.setOutputParentDir(Path.of(outputDirPath));
      FileUtils.createDirectoryIfNonexistent(FileIO.getOutputParentDir());

      indexer.scanSourceData();
      indexer.buildWorkflowCommands();

      indexer.serializeUnderlay();
      indexer.writeSerializedUnderlay();
      indexer.writeWorkflowCommands();
    } else if ("INDEX_ENTITY".equals(cmd)) {
      String name = args[2];
      String dryRun = args.length > 3 ? args[3] : "";

      Entity entity = indexer.getUnderlay().getEntity(name);
      entity.getIndexingJobs().forEach(ij -> ij.runIfPending("DRY_RUN".equals(dryRun)));
    } else if ("INDEX_ENTITY_GROUP".equals(cmd)) {
      String name = args[2];
      String dryRun = args.length > 3 ? args[3] : "";

      EntityGroup entityGroup = indexer.getUnderlay().getEntityGroup(name);
      entityGroup.getIndexingJobs().forEach(ij -> ij.runIfPending("DRY_RUN".equals(dryRun)));
    } else {
      throw new IllegalArgumentException("Unknown command: " + cmd);
    }
  }
}
