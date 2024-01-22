package bio.terra.tanagra.annotation;

import bio.terra.tanagra.exception.SystemException;
import java.io.IOException;
import java.nio.file.Path;

public final class Main {
  private static final String DOCS_GENERATED_DIR = "docs/generated/";
  private static final String TYPESCRIPT_GENERATED_DIR = "ui/src/tanagra-underlay/";

  private Main() {}

  private enum Command {
    APPLICATION_CONFIG_DOCS(new MarkdownWalker(new ApplicationConfigPath(), "APPLICATION_CONFIG.md")),
    UNDERLAY_CONFIG_DOCS(new MarkdownWalker(new UnderlayConfigPath(), "UNDERLAY_CONFIG.md")),
    UNDERLAY_CONFIG_TYPESCRIPT(new TypescriptWalker(new UnderlayConfigPath(), "underlayConfig.ts"));
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

    Path outputDir =
        Path.of(parentDir)
            .resolve(
                Command.UNDERLAY_CONFIG_TYPESCRIPT.equals(command)
                    ? TYPESCRIPT_GENERATED_DIR
                    : DOCS_GENERATED_DIR)
            .toAbsolutePath();
    command.getAnnotationWalker().writeOutputFile(outputDir);
  }
}
