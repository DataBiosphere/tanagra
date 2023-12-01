package bio.terra.tanagra.indexing.cli;

import bio.terra.tanagra.indexing.cli.index.Entity;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "tanagra index" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "index",
    header = "Commands to run indexing.",
    subcommands = Entity.class)
public class Index {}
