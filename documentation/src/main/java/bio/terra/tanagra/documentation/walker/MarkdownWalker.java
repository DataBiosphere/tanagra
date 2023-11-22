package bio.terra.tanagra.documentation.walker;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import bio.terra.tanagra.documentation.path.AnnotationPath;
import bio.terra.tanagra.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class MarkdownWalker extends AnnotationWalker {
  private final String outputFilename;

  public MarkdownWalker(AnnotationPath annotationPath, String outputFilename) {
    super(annotationPath);
    this.outputFilename = outputFilename;
  }

  @Override
  protected String arriveAtClass(AnnotatedClass classAnnotation, String className) {
    // Start a new level 2 subsection for this class.
    return new StringBuilder()
        .append("## ")
        .append(classAnnotation.name())
        .append('\n')
        .append(classAnnotation.markdown())
        .append("\n\n")
        .toString();
  }

  @Override
  protected String walkField(AnnotatedField fieldAnnotation, String fieldName) {
    // Start a new level 3 subsection for each field.
    StringBuilder markdown =
        new StringBuilder()
            .append("### ")
            .append(fieldAnnotation.name().isEmpty() ? fieldName : fieldAnnotation.name())
            .append('\n')

            // Add the markdown defined in the annotation.
            .append(fieldAnnotation.optional() ? "**optional**" : "**required**")
            .append("\n\n")
            .append(fieldAnnotation.markdown())
            .append("\n\n");
    if (!fieldAnnotation.exampleValue().isEmpty()) {
      markdown
          .append("*Environment variable:* `")
          .append(fieldAnnotation.environmentVariable())
          .append("`\n\n");
    }
    if (!fieldAnnotation.exampleValue().isEmpty()) {
      markdown.append("*Example value:* `").append(fieldAnnotation.exampleValue()).append("`\n\n");
    }
    return markdown.toString();
  }

  @Override
  protected String leaveClass(AnnotatedClass classAnnotation, String className) {
    return "\n\n";
  }

  @Override
  public void writeOutputFiles(Path outputDir) throws IOException {
    // Walk all the classes, appending them all together.
    String bodyContents =
        annotationPath.getClassesToWalk().stream()
            .map(clazz -> walk(clazz))
            .collect(Collectors.joining());

    // Prepend the class-specific output with a header.
    String fileHeader =
        new StringBuilder()
            .append("# ")
            .append(annotationPath.getTitle())
            .append("\n\n")
            .append(annotationPath.getIntroduction())
            .append("\n\n")
            .toString();

    FileUtils.writeStringToFile(outputDir.resolve(outputFilename), fileHeader + bodyContents);
  }
}