package bio.terra.tanagra.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

public class MarkdownWalker extends AnnotationWalker {
  private final List<String> tableOfContents = new ArrayList<>();
  private final Map<String, String> bookmarks = new HashMap<>();

  public MarkdownWalker(AnnotationPath annotationPath, String outputFilename) {
    super(annotationPath, outputFilename);
  }

  @Override
  protected String arriveAtClass(AnnotatedClass classAnnotation, Class<?> clazz) {
    // Add a bookmark for this class.
    addBookmark(classAnnotation.name(), classAnnotation.name());
    addBookmarkLink(clazz.getTypeName(), classAnnotation.name());

    // Add this class to the table of contents.
    tableOfContents.add("* [" + classAnnotation.name() + "](${" + classAnnotation.name() + "})");

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
  @SuppressWarnings("PMD.InsufficientStringBufferDeclaration")
  protected String walkField(AnnotatedField fieldAnnotation, Field field) {
    // Add a bookmark for this field.
    String fieldTitle = fieldAnnotation.name().isEmpty() ? field.getName() : fieldAnnotation.name();
    addBookmark(fieldAnnotation.name(), fieldTitle);

    // Start a new level 3 subsection for each field.
    StringBuilder markdown =
        new StringBuilder()
            .append("### ")
            .append(fieldTitle)
            .append('\n')
            .append(fieldAnnotation.optional() ? "**optional** " : "**required** ");

    // Add the markdown for field type.
    if (field.getGenericType() instanceof ParameterizedType pType) {
      // This is a type-parameterized class (e.g. List, Map).
      markdown.append(getSimpleName(pType.getRawType().getTypeName())).append(" [ ");

      List<String> params = new ArrayList<>();
      for (Type typeParam : pType.getActualTypeArguments()) {
        if (typeParam instanceof ParameterizedType pNestedType) {
          StringBuilder sb =
              new StringBuilder()
                  .append(getSimpleName(pNestedType.getRawType().getTypeName()))
                  .append(" [ ")
                  .append(
                      Arrays.stream(pNestedType.getActualTypeArguments())
                          .map(
                              nestedTypeParam ->
                                  annotationPath
                                          .getClassesToWalk()
                                          .contains(nestedTypeParam.getClass())
                                      ? "${" + nestedTypeParam.getTypeName() + "}"
                                      : getSimpleName(nestedTypeParam.getTypeName()))
                          .collect(Collectors.joining(", ")))
                  .append(" ]");
          params.add(sb.toString());

        } else {
          params.add(
              annotationPath.getClassesToWalk().contains(typeParam.getClass())
                  ? "${" + typeParam.getTypeName() + "}"
                  : getSimpleName(typeParam.getTypeName()));
        }
      }
      markdown.append(String.join(", ", params)).append(" ]");

    } else {
      markdown.append(
          annotationPath.getClassesToWalk().contains(field.getType())
              ? "${" + field.getType().getTypeName() + "}"
              : field.getType().getSimpleName());
    }

    // Add the markdown defined in the annotation.
    markdown.append("\n\n").append(fieldAnnotation.markdown()).append("\n\n");
    if (!fieldAnnotation.environmentVariable().isEmpty()) {
      markdown
          .append("*Environment variable:* `")
          .append(fieldAnnotation.environmentVariable())
          .append("`\n\n");
    }
    if (!fieldAnnotation.defaultValue().isEmpty()) {
      markdown.append("*Default value:* `").append(fieldAnnotation.defaultValue()).append("`\n\n");
    }
    if (!fieldAnnotation.exampleValue().isEmpty()) {
      markdown.append("*Example value:* `").append(fieldAnnotation.exampleValue()).append("`\n\n");
    }
    return markdown.toString();
  }

  @Override
  @SuppressWarnings("PMD.InsufficientStringBufferDeclaration")
  protected String walkInheritedField(AnnotatedInheritedField inheritedFieldAnnotation) {
    // Add a bookmark for this field.
    addBookmark(inheritedFieldAnnotation.name(), inheritedFieldAnnotation.name());

    // Start a new level 3 subsection for each field.
    StringBuilder markdown =
        new StringBuilder()
            .append("### ")
            .append(inheritedFieldAnnotation.name())
            .append('\n')

            // Add the field type and markdown defined in the annotation.
            .append(inheritedFieldAnnotation.optional() ? "**optional** " : "**required** ")
            .append(inheritedFieldAnnotation.typeName())
            .append("\n\n")
            .append(inheritedFieldAnnotation.markdown())
            .append("\n\n");
    if (!inheritedFieldAnnotation.environmentVariable().isEmpty()) {
      markdown
          .append("*Environment variable:* `")
          .append(inheritedFieldAnnotation.environmentVariable())
          .append("`\n\n");
    }
    if (!inheritedFieldAnnotation.defaultValue().isEmpty()) {
      markdown
          .append("*Default value:* `")
          .append(inheritedFieldAnnotation.defaultValue())
          .append("`\n\n");
    }
    if (!inheritedFieldAnnotation.exampleValue().isEmpty()) {
      markdown
          .append("*Example value:* `")
          .append(inheritedFieldAnnotation.exampleValue())
          .append("`\n\n");
    }
    return markdown.toString();
  }

  @Override
  protected String leaveClass(AnnotatedClass classAnnotation, String className) {
    return "\n\n";
  }

  @Override
  public String getOutputFileContents() {
    // Walk all the classes, appending them all together.
    String bodyContents = walk();

    // Prepend the class-specific output with a header and the table of contents.
    String fileHeader =
        new StringBuilder()
            .append("# ")
            .append(annotationPath.getTitle())
            .append("\n\n")
            .append(annotationPath.getIntroduction())
            .append("\n\n")
            .append(String.join("\n", tableOfContents))
            .append("\n\n")
            .toString();

    // Substitute in all the bookmarks.
    return StringSubstitutor.replace(fileHeader + bodyContents, bookmarks);
  }

  private void addBookmark(String name, String title) {
    bookmarks.put(name, getBookmark(title));
  }

  private void addBookmarkLink(String name, String title) {
    bookmarks.put(name, "[" + title + "](" + getBookmark(title) + ")");
  }

  private static String getBookmark(String title) {
    return "#" + title.toLowerCase().replaceAll("[^A-Za-z0-9 ]", "").replace(' ', '-');
  }

  private static String getSimpleName(String className) {
    String[] pieces = className.split("\\.");
    return pieces[pieces.length - 1];
  }
}
