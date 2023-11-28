package bio.terra.tanagra.annotation;

import bio.terra.tanagra.utils.FileUtils;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AnnotationWalker {
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationWalker.class);
  protected final AnnotationPath annotationPath;
  private final String outputFilename;

  public AnnotationWalker(AnnotationPath annotationPath, String outputFilename) {
    this.annotationPath = annotationPath;
    this.outputFilename = outputFilename;
  }

  protected abstract String arriveAtClass(AnnotatedClass classAnnotation, String className);

  protected abstract String walkField(AnnotatedField fieldAnnotation, Field field);

  protected abstract String walkInheritedField(AnnotatedInheritedField inheritedFieldAnnotation);

  protected abstract String leaveClass(AnnotatedClass classAnnotation, String className);

  private String walk(Class<?> clazz) {
    if (!clazz.isAnnotationPresent(AnnotatedClass.class)) {
      LOGGER.warn(
          "Skipping {} because it is not annotated with AnnotatedClass", clazz.getCanonicalName());
      return "";
    }

    StringBuilder output = new StringBuilder();
    AnnotatedClass classAnnotation = clazz.getAnnotation(AnnotatedClass.class);
    output.append(arriveAtClass(classAnnotation, clazz.getSimpleName()));

    // Walk through each inherited field with an annotation.
    if (clazz.getAnnotation(AnnotatedInheritedFields.class) != null) {
      Arrays.asList(clazz.getAnnotation(AnnotatedInheritedFields.class).value()).stream()
          .sorted(Comparator.comparing(AnnotatedInheritedField::name))
          .forEach(
              inheritedFieldAnnotation ->
                  output.append(walkInheritedField(inheritedFieldAnnotation)));
    }

    // Walk through each field with an annotation.
    Arrays.stream(clazz.getDeclaredFields())
        .filter(
            field -> {
              field.setAccessible(true); // Allow processing private fields.
              return field.isAnnotationPresent(AnnotatedField.class);
            })
        .sorted(
            Comparator.comparing(
                field -> {
                  AnnotatedField fieldAnnotation = field.getAnnotation(AnnotatedField.class);
                  return fieldAnnotation.name().isEmpty()
                      ? field.getName()
                      : fieldAnnotation.name();
                }))
        .forEach(
            field -> {
              AnnotatedField fieldAnnotation = field.getAnnotation(AnnotatedField.class);
              output.append(walkField(fieldAnnotation, field));
            });

    output.append(leaveClass(classAnnotation, clazz.getSimpleName()));
    return output.toString();
  }

  protected final String walk() {
    return annotationPath.getClassesToWalk().stream()
        .sorted(Comparator.comparing(Class::getSimpleName))
        .map(clazz -> walk(clazz))
        .collect(Collectors.joining());
  }

  public abstract String getOutputFileContents();

  public final void writeOutputFile(Path outputDir) throws IOException {
    FileUtils.writeStringToFile(outputDir.resolve(outputFilename), getOutputFileContents());
  }
}
