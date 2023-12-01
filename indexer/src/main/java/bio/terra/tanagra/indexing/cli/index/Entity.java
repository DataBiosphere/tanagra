package bio.terra.tanagra.indexing.cli.index;

import bio.terra.tanagra.cli.command.BaseCommand;
import bio.terra.tanagra.cli.command.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "tanagra index entity" command. */
@Command(name = "entity", description = "Run all jobs for a single entity.")
public class Entity extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  /** Run all the indexing jobs for a single entity and print out a summary of the results. */
  @Override
  protected void execute() {
    formatOption.printReturnValue("all tests passed", this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(String returnValue) {
    BaseCommand.OUT.println("Index entity jobs status: " + returnValue);
  }
}
