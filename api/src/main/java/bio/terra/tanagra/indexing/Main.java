package bio.terra.tanagra.indexing;

import static bio.terra.tanagra.indexing.Main.Command.INDEX_ENTITY;
import static bio.terra.tanagra.indexing.Main.Command.INDEX_ENTITY_GROUP;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.utils.FileUtils;
import java.nio.file.Path;

public final class Main {
  private Main() {}

  enum Command {
    EXPAND_CONFIG,
    INDEX_ENTITY,
    INDEX_ENTITY_GROUP,
    INDEX_ALL,
    CLEAN_ENTITY,
    CLEAN_ENTITY_GROUP,
    CLEAN_ALL
  }

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
    Command cmd = Command.valueOf(args[0]);
    String underlayFilePath = args[1];

    // TODO: Use singleton FileIO instance instead of setting a bunch of separate static properties.
    FileIO.setToReadDiskFiles(); // This is the default, included here for clarity.
    FileIO.setInputParentDir(Path.of(underlayFilePath).toAbsolutePath().getParent());
    Indexer indexer =
        Indexer.deserializeUnderlay(Path.of(underlayFilePath).getFileName().toString());

    switch (cmd) {
      case EXPAND_CONFIG:
        String outputDirPath = args[2];

        FileIO.setOutputParentDir(Path.of(outputDirPath));
        FileUtils.createDirectoryIfNonexistent(FileIO.getOutputParentDir());

        indexer.scanSourceData();

        indexer.serializeUnderlay();
        indexer.writeSerializedUnderlay();
        break;
      case INDEX_ENTITY:
      case CLEAN_ENTITY:
        boolean isRunEntity = INDEX_ENTITY.equals(cmd);
        String nameEntity = args[2];
        boolean isDryRunEntity = isDryRun(3, args);

        // Index/clean all the entities (*) or just one (entityName).
        if ("*".equals(nameEntity)) {
          if (isRunEntity) {
            indexer.runJobsForAllEntities(isDryRunEntity);
          } else {
            indexer.cleanAllEntities(isDryRunEntity);
          }
        } else {
          if (isRunEntity) {
            indexer.runJobsForEntity(nameEntity, isDryRunEntity);
          } else {
            indexer.cleanEntity(nameEntity, isDryRunEntity);
          }
        }
        break;
      case INDEX_ENTITY_GROUP:
      case CLEAN_ENTITY_GROUP:
        boolean isRunEntityGroup = INDEX_ENTITY_GROUP.equals(cmd);
        String nameEntityGroup = args[2];
        boolean isDryRunEntityGroup = isDryRun(3, args);

        // Index/clean all the entity groups (*) or just one (entityGroupName).
        if ("*".equals(nameEntityGroup)) {
          if (isRunEntityGroup) {
            indexer.runJobsForAllEntityGroups(isDryRunEntityGroup);
          } else {
            indexer.cleanAllEntityGroups(isDryRunEntityGroup);
          }
        } else {
          if (isRunEntityGroup) {
            indexer.runJobsForEntityGroup(nameEntityGroup, isDryRunEntityGroup);
          } else {
            indexer.cleanEntityGroup(nameEntityGroup, isDryRunEntityGroup);
          }
        }
        break;
      case INDEX_ALL:
        boolean isDryRunIndexAll = isDryRun(2, args);
        indexer.runJobsForAllEntities(isDryRunIndexAll);
        indexer.runJobsForAllEntityGroups(isDryRunIndexAll);
        break;
      case CLEAN_ALL:
        boolean isDryRunCleanAll = isDryRun(2, args);
        indexer.cleanAllEntities(isDryRunCleanAll);
        indexer.cleanAllEntityGroups(isDryRunCleanAll);
        break;
      default:
        throw new SystemException("Unknown command: " + cmd);
    }
  }

  private static boolean isDryRun(int index, String... args) {
    return args.length > index && "DRY_RUN".equals(args[index]);
  }
}
