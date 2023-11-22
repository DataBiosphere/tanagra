package bio.terra.tanagra.documentation.path;

import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.app.configuration.AuthenticationConfiguration;
import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DeploymentConfigPath extends AnnotationPath {
  private static final String FILE_TITLE = "Deployment Configuration";
  private static final String FILE_INTRODUCTION =
      "This file lists all the configuration properties available for a deployment of the service.\n"
          + "You can set the properties either with an `application.yaml` file or with environment variables.\n"
          + "This documentation is generated from annotations in the configuration classes.";

  private enum AnnotatedClass {
    ACCESS_CONTROL(AccessControlConfiguration.class),
    AUTHENTICATION(AuthenticationConfiguration.class),
    UNDERLAY(UnderlayConfiguration.class);

    private final Class<?> clazz;

    AnnotatedClass(Class<?> clazz) {
      this.clazz = clazz;
    }

    public Class<?> getClazz() {
      return clazz;
    }
  }

  @Override
  public String getTitle() {
    return FILE_TITLE;
  }

  @Override
  public String getIntroduction() {
    return FILE_INTRODUCTION;
  }

  @Override
  public List<Class<?>> getClassesToWalk() {
    return Arrays.stream(AnnotatedClass.values())
        .map(AnnotatedClass::getClazz)
        .collect(Collectors.toList());
  }
}
