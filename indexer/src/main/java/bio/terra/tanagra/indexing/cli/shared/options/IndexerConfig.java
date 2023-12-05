package bio.terra.tanagra.indexing.cli.shared.options;

import java.nio.file.Path;
import picocli.CommandLine;

/**
 * Command helper class that defines the indexer config name.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class IndexerConfig {
  @CommandLine.Option(
      names = "--indexer-config",
      required = true,
      description = "Indexer config name")
  public String name;

  @CommandLine.Option(
      names = "--github-dir",
      description =
          "Absolute path of the top-level directory for the local clone of the tanagra GitHub repo. Defaults to the current directory.")
  public String githubDir;

  public Path getGitHubDirWithDefault() {
    return githubDir == null || githubDir.isEmpty() ? Path.of("") : Path.of(githubDir);
  }
}
