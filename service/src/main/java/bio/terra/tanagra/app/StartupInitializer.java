package bio.terra.tanagra.app;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.app.configuration.TanagraDatabaseConfiguration;
import bio.terra.tanagra.app.configuration.TanagraDatabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public final class StartupInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(StartupInitializer.class);
  private static final String CHANGELOG_PATH = "db/changelog.xml";

  private StartupInitializer() {}

  public static void initialize(ApplicationContext applicationContext) {
    LOGGER.info("Initializing application before startup");
    // Initialize or upgrade the database depending on the configuration.
    LiquibaseMigrator migrateService = applicationContext.getBean(LiquibaseMigrator.class);
    TanagraDatabaseConfiguration tanagraDatabaseConfiguration =
        applicationContext.getBean(TanagraDatabaseConfiguration.class);
    TanagraDatabaseProperties tanagraDatabaseProperties =
        applicationContext.getBean(TanagraDatabaseProperties.class);
    FeatureConfiguration featureConfiguration =
        applicationContext.getBean(FeatureConfiguration.class);

    // Log the state of the feature flags.
    featureConfiguration.logFeatures();

    // Migrate the database.
    LOGGER.info("Migrating database");
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
