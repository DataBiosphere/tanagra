# Deployment Configuration

This file lists all the configuration properties available for a deployment of the service.
You can set the properties either with an `application.yaml` file or with environment variables.
This documentation is generated from annotations in the configuration classes.

## Export (Shared)
Configure the export options shared by all models.

### tanagra.export.shared.gcsBucketNames
**optional**

Comma separated list of all GCS bucket names that all export models can use. Only include the bucket name, not the gs:// prefix. Required if there are any export models that need to write to GCS.

*Environment variable:* `TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES`

*Example value:* `broad-tanagra-dev-bq-export-uscentral1,broad-tanagra-dev-bq-export-useast1`

### tanagra.export.shared.gcsProjectId
**optional**

GCP project id that contains the GCS bucket(s) that all export models can use. Required if there are any export models that need to write to GCS.

*Environment variable:* `TANAGRA_EXPORT_SHARED_GCS_BUCKET_PROJECT_ID`

*Example value:* `broad-tanagra-dev`



## Export (Per Model)
Configure the export options for each model.

### tanagra.export.models.name
**optional**

Name of the export model. This must be unique across all models for a given deployment. Defaults to the name of the export model. It's useful to override the default if you have more than one instance of the same model (e.g. export to VWB parameterized with the dev environment URL, and another with the test environment URL).

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_NAME (Note 0 is the list index, so if you have 2 models, you will have 0 and 1 env vars.)`

*Example value:* `VWB_FILE_IMPORT_TO_DEV`



## Feature Flags
Enable and disable specific features.

### tanagra.feature.activityLogEnabled
**optional**

When true, we store activity log events in the application database. This is intended to support auditing requirements.

*Default value:* `false`

### tanagra.feature.artifactStorageEnabled
**optional**

When true, artifacts can be created, updated and deleted. Artifacts include studies, cohorts, concept sets, reviews, and annotations.

*Default value:* `false`



## Application Database
Configure the application database.

### tanagra.db.password
**required**

Password for the application database.

*Environment variable:* `TANAGRA_DB_PASSWORD`

*Example value:* `dbpwd`

### tanagra.db.uri
**required**

URI of the application database.

*Environment variable:* `TANAGRA_DB_URI`

*Example value:* `jdbc:postgresql://127.0.0.1:5432/tanagra_db`

### tanagra.db.username
**required**

Username for the application database.

*Environment variable:* `TANAGRA_DB_USERNAME`

*Example value:* `dbuser`

### tanagra.db.cloudSqlInstance
**optional**

Name of the Cloud SQL instance `**project:region:instance**`. Required to configure a CloudSQL connector (e.g. when deployed in AppEngine). More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).

### tanagra.db.driverClassName
**optional**

Name of the driver class. Required to configure a CloudSQL connector (e.g. when deployed in AppEngine). More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).

*Environment variable:* `TANAGRA_DB_DRIVER_CLASS_NAME`

*Example value:* `com.mysql.cj.jdbc.Driver`

### tanagra.db.initializeOnStart
**optional**

When true, the application database will be wiped on service startup.

*Default value:* `false`

### tanagra.db.ipTypes
**optional**

Comma separated list of preferred IP types. Used to configure a CloudSQL connector (e.g. when deployed in AppEngine). Not required to use a CloudSQL connector. Leave empty to use GCP's default. More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).

*Environment variable:* `TANAGRA_DB_DRIVER_IP_TYPES`

*Example value:* `PUBLIC,PRIVATE`

### tanagra.db.socketFactory
**optional**

Name of the socket factory class. Required to configure a CloudSQL connector (e.g. when deployed in AppEngine). More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).

*Environment variable:* `TANAGRA_DB_SOCKET_FACTORY`

*Example value:* `com.google.cloud.sql.mysql.SocketFactory`

### tanagra.db.upgradeOnStart
**optional**

When true, the application database will have Liquibase changesets applied on service startup.

*Default value:* `false`



## Underlays
Configure the underlays served.

### tanagra.underlay.files
**required**

Comma-separated list of service configurations. Use the name of the service configuration file only, no extension or path.

*Environment variable:* `TANAGRA_UNDERLAY_FILES`

*Example value:* `cmssynpuf_broad,aouSR2019q4r4_broad`



