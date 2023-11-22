package bio.terra.tanagra.documentation.path;

import java.util.List;

public abstract class AnnotationPath {
  public abstract String getTitle();

  public abstract String getIntroduction();

  public abstract List<Class<?>> getClassesToWalk();
}
