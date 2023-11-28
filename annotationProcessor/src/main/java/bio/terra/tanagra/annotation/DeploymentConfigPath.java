package bio.terra.tanagra.annotation;

import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.app.configuration.AuthenticationConfiguration;
import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.app.configuration.TanagraDatabaseProperties;
import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import java.util.List;

public class DeploymentConfigPath extends AnnotationPath {
  private static final String FILE_TITLE = "Deployment Configuration";
  private static final String FILE_INTRODUCTION =
      "This file lists all the configuration properties available for a deployment of the service.\n"
          + "You can set the properties either with an `application.yaml` file or with environment variables.\n"
          + "This documentation is generated from annotations in the configuration classes.";

  private static final List<Class<?>> CLASSES_TO_WALK =
      List.of(
          AccessControlConfiguration.class,
          AuthenticationConfiguration.class,
          ExportConfiguration.Shared.class,
          ExportConfiguration.PerModel.class,
          FeatureConfiguration.class,
          TanagraDatabaseProperties.class,
          UnderlayConfiguration.class);

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
    return CLASSES_TO_WALK;
  }
}
