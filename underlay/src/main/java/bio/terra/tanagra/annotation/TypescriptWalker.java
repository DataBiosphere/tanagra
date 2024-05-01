package bio.terra.tanagra.annotation;

import bio.terra.tanagra.exception.SystemException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.text.StringSubstitutor;

public class TypescriptWalker extends AnnotationWalker {
  private final Map<String, String> classTypeNames = new HashMap<>();

  public TypescriptWalker(AnnotationPath annotationPath, String outputFilename) {
    super(annotationPath, outputFilename);
  }

  @Override
  protected String arriveAtClass(AnnotatedClass classAnnotation, Class<?> clazz) {
    classTypeNames.put(clazz.getTypeName(), classAnnotation.name());

    // Start a new type declaration for this class.
    return new StringBuilder()
        .append("export ")
        .append(clazz.isEnum() ? "enum" : "type")
        .append(' ')
        .append(classAnnotation.name())
        .append(clazz.isEnum() ? "" : " =")
        .append(" {\n")
        .toString();
  }

  @Override
  protected String walkField(AnnotatedField fieldAnnotation, Field field) {
    StringBuilder fieldNameAndType = new StringBuilder("  ").append(field.getName());
    if (fieldAnnotation.optional()) {
      fieldNameAndType.append('?');
    }

    if (field.getGenericType() instanceof ParameterizedType) {
      // This is a type-parameterized class (e.g. List, Map).
      ParameterizedType pType = (ParameterizedType) field.getGenericType();
      String pTypeName = pType.getRawType().getTypeName();

      fieldNameAndType.append(": ");
      if (pTypeName.equals(List.class.getTypeName()) || pTypeName.equals(Set.class.getTypeName())) {
        // Convert Typescript array e.g. string[]
        fieldNameAndType
            .append(getTypeNameOrSubstitutionLink(pType.getActualTypeArguments()[0].getTypeName()))
            .append("[]");
      } else if (pTypeName.equals(Map.class.getTypeName())) {
        // Convert to Typescript map e.g. { [key: string]: string }
        fieldNameAndType
            .append("{ [key: ")
            .append(getTypeNameOrSubstitutionLink(pType.getActualTypeArguments()[0].getTypeName()))
            .append("]: ")
            .append(getTypeNameOrSubstitutionLink(pType.getActualTypeArguments()[1].getTypeName()))
            .append(" }");
      } else {
        throw new SystemException("Undefined conversion to Typescript for Java type: " + pTypeName);
      }
      fieldNameAndType.append(';');
    } else if (field.getDeclaringClass().isEnum()) {
      // This is an enum value.
      try {
        fieldNameAndType.append(" = \"").append(field.get(null).toString()).append("\",");
      } catch (IllegalAccessException iaEx) {
        throw new SystemException(
            "Error reading enum string from Java field: " + field.getName(), iaEx);
      }
    } else {
      // This is a field with a simple type (e.g. String).
      fieldNameAndType
          .append(": ")
          .append(getTypeNameOrSubstitutionLink(field.getType().getTypeName()))
          .append(';');
    }

    return fieldNameAndType.append('\n').toString();
  }

  private String getTypeNameOrSubstitutionLink(String typeName) {
    if (annotationPath.getClassesToWalk().stream()
        .map(Class::getTypeName)
        .toList()
        .contains(typeName)) {
      return "${" + typeName + "}";
    } else {
      String[] pieces = typeName.split("\\.");
      String javaTypeName = pieces[pieces.length - 1].toLowerCase();

      final List<String> javaTypesThatMapToNumber = List.of("int", "long", "double");
      return javaTypesThatMapToNumber.contains(javaTypeName) ? "number" : javaTypeName;
    }
  }

  @Override
  protected String walkInheritedField(AnnotatedInheritedField inheritedFieldAnnotation) {
    // There are no inherited field annotations for the underlay config, so this method is not
    // needed for the Typescript walker.
    throw new UnsupportedOperationException(
        "Inherited field annotations not supported for typescript walker.");
  }

  @Override
  protected String leaveClass(AnnotatedClass classAnnotation, String className) {
    // End the type declaration for this class.
    return "};\n\n";
  }

  @Override
  public String getOutputFileContents() {
    // Walk all the classes, appending them all together.
    String bodyContents = walk();

    // Substitute in all the class type names.
    return StringSubstitutor.replace(bodyContents, classTypeNames);
  }
}
