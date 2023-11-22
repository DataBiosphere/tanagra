package bio.terra.tanagra.documentation.walker;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import bio.terra.tanagra.documentation.path.AnnotationPath;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AnnotationWalker {
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationWalker.class);
  protected final AnnotationPath annotationPath;

  public AnnotationWalker(AnnotationPath annotationPath) {
    this.annotationPath = annotationPath;
  }

  protected abstract String arriveAtClass(AnnotatedClass classAnnotation, String className);

  protected abstract String walkField(AnnotatedField fieldAnnotation, String fieldName);

  protected abstract String leaveClass(AnnotatedClass classAnnotation, String className);

  protected String walk(Class<?> clazz) {
    if (!clazz.isAnnotationPresent(AnnotatedClass.class)) {
      LOGGER.warn(
          "Skipping {} because it is not annotated with AnnotatedClass", clazz.getCanonicalName());
      return "";
    }

    StringBuilder output = new StringBuilder();
    AnnotatedClass classAnnotation = clazz.getAnnotation(AnnotatedClass.class);
    output.append(arriveAtClass(classAnnotation, clazz.getSimpleName()));

    // Walk through each field with an annotation.
    for (Field field : clazz.getDeclaredFields()) {
      field.setAccessible(true); // Allow processing private fields.
      if (field.isAnnotationPresent(AnnotatedField.class)) {
        AnnotatedField fieldAnnotation = field.getAnnotation(AnnotatedField.class);
        output.append(walkField(fieldAnnotation, field.getName()));
      }
    }
    output.append(leaveClass(classAnnotation, clazz.getSimpleName()));
    return output.toString();
  }

  public abstract void writeOutputFiles(Path outputDir) throws IOException;
}
