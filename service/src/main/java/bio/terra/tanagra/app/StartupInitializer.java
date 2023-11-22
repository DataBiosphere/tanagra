package bio.terra.tanagra.app;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.app.configuration.AuthenticationConfiguration;
import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.app.configuration.TanagraDatabaseConfiguration;
import bio.terra.tanagra.app.configuration.TanagraDatabaseProperties;
import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import bio.terra.tanagra.app.configuration.VersionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public final class StartupInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(StartupInitializer.class);
  private static final String CHANGELOG_PATH = "db/changelog.xml";

  private StartupInitializer() {}

  public static void initialize(ApplicationContext applicationContext) {
    LOGGER.info("Initializing application before startup");

    // Load all the configuration beans.
    AccessControlConfiguration accessControlConfiguration =
        applicationContext.getBean(AccessControlConfiguration.class);
    AuthenticationConfiguration authenticationConfiguration =
        applicationContext.getBean(AuthenticationConfiguration.class);
    ExportConfiguration exportConfiguration = applicationContext.getBean(ExportConfiguration.class);
    FeatureConfiguration featureConfiguration =
        applicationContext.getBean(FeatureConfiguration.class);
    TanagraDatabaseProperties tanagraDatabaseProperties =
        applicationContext.getBean(TanagraDatabaseProperties.class);
    UnderlayConfiguration underlayConfiguration =
        applicationContext.getBean(UnderlayConfiguration.class);
    VersionConfiguration versionConfiguration =
        applicationContext.getBean(VersionConfiguration.class);

    // Log the state of the application configuration.
    LOGGER.info("Logging the application config before startup");
    accessControlConfiguration.log();
    authenticationConfiguration.log();
    exportConfiguration.log();
    featureConfiguration.log();
    tanagraDatabaseProperties.log();
    underlayConfiguration.log();
    versionConfiguration.log();

    // Initialize or migrate the database depending on the configuration.
    LOGGER.info("Migrating database");
    TanagraDatabaseConfiguration tanagraDatabaseConfiguration =
        applicationContext.getBean(TanagraDatabaseConfiguration.class);
    LiquibaseMigrator migrateService = applicationContext.getBean(LiquibaseMigrator.class);
    if (tanagraDatabaseProperties.isInitializeOnStart()) {
      migrateService.initialize(CHANGELOG_PATH, tanagraDatabaseConfiguration.getDataSource());
    } else if (tanagraDatabaseProperties.isUpgradeOnStart()) {
      migrateService.upgrade(CHANGELOG_PATH, tanagraDatabaseConfiguration.getDataSource());
    }

    // NOTE:
    // Fill in this method with any other initialization that needs to happen
    // between the point of having the entire application initialized and
    // the point of opening the port to start accepting REST requests.
  }
}
