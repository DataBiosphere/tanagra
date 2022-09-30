package bio.terra.tanagra.indexing;

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

      indexer.serializeUnderlay();
      indexer.writeSerializedUnderlay();
    } else if ("INDEX_ENTITY".equals(cmd) || "CLEAN_ENTITY".equals(cmd)) {
      boolean isRun = "INDEX_ENTITY".equals(cmd);
      String name = args[2];
      boolean isDryRun = isDryRun(3, args);

      // Index/clean all the entities (*) or just one (entityName).
      if ("*".equals(name)) {
        if (isRun) {
          indexer.runJobsForAllEntities(isDryRun);
        } else {
          indexer.cleanAllEntities(isDryRun);
        }
      } else {
        if (isRun) {
          indexer.runJobsForEntity(name, isDryRun);
        } else {
          indexer.cleanEntity(name, isDryRun);
        }
      }
    } else if ("INDEX_ENTITY_GROUP".equals(cmd) || "CLEAN_ENTITY_GROUP".equals(cmd)) {
      boolean isRun = "INDEX_ENTITY_GROUP".equals(cmd);
      String name = args[2];
      boolean isDryRun = isDryRun(3, args);

      // Index/clean all the entity groups (*) or just one (entityGroupName).
      if ("*".equals(name)) {
        if (isRun) {
          indexer.runJobsForAllEntityGroups(isDryRun);
        } else {
          indexer.cleanAllEntityGroups(isDryRun);
        }
      } else {
        if (isRun) {
          indexer.runJobsForEntityGroup(name, isDryRun);
        } else {
          indexer.cleanEntityGroup(name, isDryRun);
        }
      }
    } else if ("INDEX_ALL".equals(cmd)) {
      boolean isDryRun = isDryRun(2, args);
      indexer.runJobsForAllEntities(isDryRun);
      indexer.runJobsForAllEntityGroups(isDryRun);
    } else if ("CLEAN_ALL".equals(cmd)) {
      boolean isDryRun = isDryRun(2, args);
      indexer.cleanAllEntities(isDryRun);
      indexer.cleanAllEntityGroups(isDryRun);
    } else {
      throw new IllegalArgumentException("Unknown command: " + cmd);
    }
  }

  private static boolean isDryRun(int index, String... args) {
    return args.length > index && "DRY_RUN".equals(args[index]);
  }
}
