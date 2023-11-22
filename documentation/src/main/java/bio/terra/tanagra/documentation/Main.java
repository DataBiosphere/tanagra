package bio.terra.tanagra.documentation;

import bio.terra.tanagra.documentation.path.DeploymentConfigPath;
import bio.terra.tanagra.documentation.walker.AnnotationWalker;
import bio.terra.tanagra.documentation.walker.MarkdownWalker;
import bio.terra.tanagra.exception.SystemException;
import java.io.IOException;
import java.nio.file.Path;

public final class Main {
  private static final String DOCS_GENERATED_DIR = "docs/generated/";

  private Main() {}

  private enum Command {
    DEPLOYMENT_CONFIG(new MarkdownWalker(new DeploymentConfigPath(), "DEPLOYMENT_CONFIG.md"));
    private final AnnotationWalker annotationWalker;

    Command(AnnotationWalker annotationWalker) {
      this.annotationWalker = annotationWalker;
    }

    AnnotationWalker getAnnotationWalker() {
      return annotationWalker;
    }
  }

  @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      throw new SystemException(
          "Expected two arguments (parent directory, command), found " + args.length);
    }
    String parentDir = args[0];
    Command command = Command.valueOf(args[1]);

    Path outputDir = Path.of(parentDir).resolve(DOCS_GENERATED_DIR).toAbsolutePath();
    command.getAnnotationWalker().writeOutputFiles(outputDir);
  }
}
