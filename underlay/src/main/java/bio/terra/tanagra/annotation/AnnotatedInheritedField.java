package bio.terra.tanagra.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Repeatable(AnnotatedInheritedFields.class)
public @interface AnnotatedInheritedField {
  String name() default "";

  String markdown() default "";

  String exampleValue() default "";

  boolean optional() default false;

  String environmentVariable() default "";

  String defaultValue() default "";

  String typeName() default "";
}
