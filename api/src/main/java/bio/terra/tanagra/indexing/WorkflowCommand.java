package bio.terra.tanagra.indexing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WorkflowCommand {
  private static final String BASH_COMMENT_PREFIX = "# ";
  private static final String BASH_SCRIPT_FILENAME = "indexing_script.sh";

  private String command;
  private String description;
  private Map<String, String> queryInputs; // filename -> query string

  protected WorkflowCommand(String command, String description, Map<String, String> queryInputs) {
    this.command = command;
    this.description = description;
    this.queryInputs = queryInputs;
  }

  public String getCommand() {
    return command;
  }

  public String getComment() {
    return BASH_COMMENT_PREFIX + description;
  }

  public Map<String, String> getQueryInputs() {
    return Collections.unmodifiableMap(queryInputs);
  }

  private void writeInputsToDisk(Path outputDir) throws IOException {
    for (Map.Entry<String, String> fileNameToContents : queryInputs.entrySet()) {
      Files.write(
          outputDir.resolve(fileNameToContents.getKey()),
          List.of(fileNameToContents.getValue()),
          StandardCharsets.UTF_8);
    }
  }

  public static void writeToDisk(List<WorkflowCommand> cmds, Path outputDir) throws IOException {
    List<String> script = new ArrayList<>();
    for (WorkflowCommand cmd : cmds) {
      cmd.writeInputsToDisk(outputDir);
      script.addAll(List.of(cmd.getComment(), cmd.getCommand(), ""));
    }
    Files.write(outputDir.resolve(BASH_SCRIPT_FILENAME), script);
  }
}
