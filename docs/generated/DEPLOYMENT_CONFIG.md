# Deployment Configuration

This file lists all the configuration properties available for a deployment of the service.
You can set the properties either with an `application.yaml` file or with environment variables.
This documentation is generated from annotations in the configuration classes.

## Access Control
Configure the access control or authorization model.

### tanagra.access-control.basePath
**optional**

URL of another service the access control model will call. e.g. Workbench URL.

*Environment variable:* `TANAGRA_ACCESS_CONTROL_BASE_PATH`

*Example value:* `https://www.workbench.com`

### tanagra.access-control.oauthClientId
**optional**

OAuth client id of another service the access control model will call. e.g. Workbench client id.

*Environment variable:* `TANAGRA_ACCESS_CONTROL_OAUTH_CLIENT_ID`

*Example value:* `abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com`

### tanagra.access-control.params
**optional**

Map of parameters to pass to the access control model. This is useful when you want to parameterize a model beyond just the base path and OAuth client id. e.g. Name of a Google Group you want to use to restrict access.

*Environment variable:* `TANAGRA_ACCESS_CONTROL_PARAMS`

*Example value:* `admin-users@googlegroups.com`

### tanagra.underlay.files
**optional**

Pointer to the access control model Java class. Currently this must be one of the enum values in the`bio.terra.tanagra.service.accesscontrol.AccessControl.Model` Java class. In the future, it will support arbitrary class names

*Default value:* `OPEN_ACCESS`



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

### tanagra.export.models.displayName
**optional**

Displayed name of the export model. This is for display only and will be shown in the export dialog when the user initiates an export. Defaults to the display name provided by the export model. It's useful to override the default if you have more than one instance of the same model (e.g. export to workbench parameterized with the dev environment URL, and another parameterized with the test environment URL).

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_DISPLAY_NAME (Note 0 is the list index, so if you have 2 models, you may have 0 and 1 env vars.)`

*Example value:* `Export File to Workbench (dev instance)`

### tanagra.export.models.name
**optional**

Name of the export model. This must be unique across all models for a given deployment. Defaults to the name of the export model. It's useful to override the default if you have more than one instance of the same model (e.g. export to workbench parameterized with the dev environment URL, and another parameterized with the test environment URL).

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_NAME (Note 0 is the list index, so if you have 2 models, you will have 0 and 1 env vars.)`

*Example value:* `VWB_FILE_IMPORT_TO_DEV`

### tanagra.export.models.params
**optional**

Map of parameters to pass to the export model. This is useful when you want to parameterize a model beyond just the redirect URL. e.g. A description for a generated notebook file.

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_PARAMS_0 (Note the first 0 is the list index of the export models, so if you have 2 models, you may have 0 and 1 env vars. The second 0 is the list index of the parameters, so if you have 2 parameters, you will need 0 and 1 env vars.)`

*Example value:* `Notebook file generated for Workbench v35`

### tanagra.export.models.redirectAwayUrl
**optional**

URL to redirect the user to once the Tanagra export model has run. This is useful when you want to import a file to another site. e.g. Write the exported data to CSV files in GCS and then redirect to a workbench URL, passing the URL to the CSV files so the workbench can import them somewhere.

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_REDIRECT_AWAY_URL (Note 0 is the list index, so if you have 2 models, you may have 0 and 1 env vars.)`

*Example value:* `https://terra-devel-ui-terra.api.verily.com/import?urlList=${tsvFileUrl}&returnUrl=${redirectBackUrl}&returnApp=Tanagra`

### tanagra.export.models.type
**optional**

Pointer to the access control model Java class. Currently this must be one of the enum values in the`bio.terra.tanagra.service.export.DataExport.Type` Java class. In the future, it will support arbitrary class names

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_TYPE (Note 0 is the list index, so if you have 2 models, you may have 0 and 1 env vars.)`

*Example value:* `IPYNB_FILE_DOWNLOAD`



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



