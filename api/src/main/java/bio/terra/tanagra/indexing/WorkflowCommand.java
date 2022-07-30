package bio.terra.tanagra.indexing;

import java.util.Collections;
import java.util.Map;

public class WorkflowCommand {
  private static String BASH_COMMENT_PREFIX = "// ";

  private String command;
  private String description;
  private Map<String, String> queryInputs; // name -> query string

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
}
