package bio.terra.tanagra.indexing.cli;

import bio.terra.tanagra.indexing.cli.clean.Entity;
import bio.terra.tanagra.indexing.cli.clean.EntityGroup;
import bio.terra.tanagra.indexing.cli.clean.Underlay;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "tanagra clean" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "clean",
    header = "Commands to clean up indexing outputs.",
    subcommands = {Entity.class, EntityGroup.class, Underlay.class})
public class Clean {}
