package bio.terra.tanagra.indexing.cli.shared.options;

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
}
