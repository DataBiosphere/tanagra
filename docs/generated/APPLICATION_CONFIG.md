# Application Configuration

This file lists all the configuration properties available for a deployment of the service application.
You can set the properties either with an `application.yaml` file or with environment variables.
This documentation is generated from annotations in the configuration classes.

* [Access Control](#access-control)
* [Application Database](#application-database)
* [Authentication](#authentication)
* [Export (Per Model)](#export-per-model)
* [Export (Shared)](#export-shared)
* [Feature Flags](#feature-flags)
* [Underlays](#underlays)

## Access Control
Configure the access control or authorization model.

### tanagra.access-control.basePath
**optional** String

URL of another service the access control model will call. e.g. Workbench URL.

*Environment variable:* `TANAGRA_ACCESS_CONTROL_BASE_PATH`

*Example value:* `https://www.workbench.com`

### tanagra.access-control.model
**optional** String

Pointer to the access control model Java class. Currently this must be one of the enum values in the`bio.terra.tanagra.service.accesscontrol.model.CoreModel` Java class, or the full name of a class that implements the `bio.terra.tanagra.service.accesscontrol.model.FineGrainedAccessControl` interface and is on the classpath.

*Environment variable:* `TANAGRA_ACCESS_CONTROL_MODEL`

*Default value:* `OPEN_ACCESS`

### tanagra.access-control.oauthClientId
**optional** String

OAuth client id of another service the access control model will call. e.g. Workbench client id.

*Environment variable:* `TANAGRA_ACCESS_CONTROL_OAUTH_CLIENT_ID`

*Example value:* `abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com`

### tanagra.access-control.params
**optional** List [ String ]

Map of parameters to pass to the access control model. Pass the map as a list e.g. key1,value1,key2,value2,... This is useful when you want to parameterize a model beyond just the base path and OAuth client id. e.g. Name of a Google Group you want to use to restrict access.

*Environment variable:* `TANAGRA_ACCESS_CONTROL_PARAMS`

*Example value:* `googleGroupName,admin-users@googlegroups.com`



## Application Database
Configure the application database.

### tanagra.db.password
**required** String

Password for the application database.

*Environment variable:* `TANAGRA_DB_PASSWORD`

*Example value:* `dbpwd`

### tanagra.db.uri
**required** String

URI of the application database.

*Environment variable:* `TANAGRA_DB_URI`

*Example value:* `jdbc:postgresql://127.0.0.1:5432/tanagra_db`

### tanagra.db.username
**required** String

Username for the application database.

*Environment variable:* `TANAGRA_DB_USERNAME`

*Example value:* `dbuser`

### tanagra.db.cloudSqlInstance
**optional** String

Name of the Cloud SQL instance `**project:region:instance**`. Required to configure a CloudSQL connector (e.g. when deployed in AppEngine). More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).

*Environment variable:* `TANAGRA_DB_CLOUD_SQL_INSTANCE`

### tanagra.db.driverClassName
**optional** String

Name of the driver class. Required to configure a CloudSQL connector (e.g. when deployed in AppEngine). More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).

*Environment variable:* `TANAGRA_DB_DRIVER_CLASS_NAME`

*Example value:* `com.mysql.cj.jdbc.Driver`

### tanagra.db.initializeOnStart
**optional** boolean

When true, the application database will be wiped on service startup.

*Environment variable:* `TANAGRA_DB_INITIALIZE_ON_START`

*Default value:* `false`

### tanagra.db.ipTypes
**optional** String

Comma separated list of preferred IP types. Used to configure a CloudSQL connector (e.g. when deployed in AppEngine). Not required to use a CloudSQL connector. Leave empty to use GCP's default. More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).

*Environment variable:* `TANAGRA_DB_IP_TYPES`

*Example value:* `PUBLIC,PRIVATE`

### tanagra.db.socketFactory
**optional** String

Name of the socket factory class. Required to configure a CloudSQL connector (e.g. when deployed in AppEngine). More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).

*Environment variable:* `TANAGRA_DB_SOCKET_FACTORY`

*Example value:* `com.google.cloud.sql.mysql.SocketFactory`

### tanagra.db.upgradeOnStart
**optional** boolean

When true, the application database will have Liquibase changesets applied on service startup.

*Environment variable:* `TANAGRA_DB_UPGRADE_ON_START`

*Default value:* `false`



## Authentication
Configure the authentication model.

There are five separate flags that control which model is used: `tanagra.auth.disableChecks`, `tanagra.auth.iapGkeJwt`, `tanagra.auth.iapAppEngineJwt`, `tanagra.auth.bearerToken`, `tanagra.auth.auth0Jwt`. In the future these will be combined into a single flag. For now, **you must set all five flags and only one should be true**. 

### tanagra.auth.accessToken
**required** boolean

When true, the service expects a JWT access token. The service verifies the access and decodes the user informationWhen this flag is set, you must also define the [Public key file](#tanagraauthaccessTokenpublicKeyFile) and [Issuer](#tanagraauthaccessTokenissuer). [Algorithm](#tanagraauthaccessTokenalgorithm) defaults to RSA256. 

*Environment variable:* `TANAGRA_AUTH_ACCESS_TOKEN`

### tanagra.auth.accessToken.algorithm
**required** String

The algorithm used to verify the JWT access token. Defaults to RSA256 

*Environment variable:* `TANAGRA_AUTH_ACCESS_TOKEN_ALGORITHM`

### tanagra.auth.accessToken.issuer
**required** String

The issuer of JWT access token used for its verification. 

*Environment variable:* `TANAGRA_AUTH_ACCESS_TOKEN_ISSUER`

### tanagra.auth.accessToken.publicKeyFile
**required** String

Name of the PEM public key file in the 'resources/keys' directory used to verify the JWT access token. 

*Environment variable:* `TANAGRA_AUTH_ACCESS_TOKEN_PUBLIC_KEY_FILE`

### tanagra.auth.bearerToken
**required** boolean

When true, the service expects a Google OAuth bearer token. The service calls Google's `https://www.googleapis.com/oauth2/v2/userinfo` endpoint to get the email address of the user from the token. More details in the [GCP documentation](https://developers.google.com/identity/openid-connect/openid-connect#obtaininguserprofileinformation).

*Environment variable:* `TANAGRA_AUTH_BEARER_TOKEN`

### tanagra.auth.disableChecks
**required** boolean

When true, authentication checks will be disabled. This is helpful during testing, especially testing a locally deployed service. It should never be used for a production service.

*Environment variable:* `TANAGRA_AUTH_DISABLE_CHECKS`

### tanagra.auth.gcpProjectId
**optional** String

The GCP project id, which is different from the project number. You can find this in the Cloud Console dashboard. More details in the [GCP documentation](https://cloud.google.com/resource-manager/docs/creating-managing-projects). Required when using the [IAP JWT GKE](#tanagraauthiapgkejwt) or [IAP JWT AppEngine](#tanagraauthiapappenginejwt) model.

*Environment variable:* `TANAGRA_AUTH_GCP_PROJECT_ID`

*Example value:* `tanagra-dev`

### tanagra.auth.gcpProjectNumber
**optional** String

The GCP project number, which is different from the project id. You can find this in the Cloud Console dashboard. More details in the [GCP documentation](https://cloud.google.com/resource-manager/docs/creating-managing-projects) and [IAP documentation](https://cloud.google.com/iap/docs/signed-headers-howto#verifying_the_jwt_payload). Required when using the [IAP JWT AppEngine](#tanagraauthiapappenginejwt) model.

*Environment variable:* `TANAGRA_AUTH_GCP_PROJECT_NUMBER`

*Example value:* `0123456789`

### tanagra.auth.gkeBackendServiceId
**optional** String

The GKE backend service id. You can find this in the Cloud Console. More details in the [IAP documentation](https://cloud.google.com/iap/docs/signed-headers-howto#verifying_the_jwt_payload). Required when using the [IAP JWT GKE](#tanagraauthiapgkejwt) model.

*Environment variable:* `TANAGRA_AUTH_GKE_BACKEND_SERVICE_ID`

*Example value:* `0123456789`

### tanagra.auth.iapAppEngineJwt
**required** boolean

When true, the service expects a JWT generated by Google IAP running in front of AppEngine. When this flag is set, you must also define the [GCP project number](#tanagraauthgcpprojectnumber) and the [GCP project id](#tanagraauthgcpprojectid). More details in the [GCP documentation](https://cloud.google.com/iap/docs/signed-headers-howto).

*Environment variable:* `TANAGRA_AUTH_IAP_APP_ENGINE_JWT`

### tanagra.auth.iapGkeJwt
**required** boolean

When true, the service expects a JWT generated by Google IAP running in front of GKE. When this flag is set, you must also define the [GKE backend service id](#tanagraauthgkebackendserviceid) and the [GCP project id](#tanagraauthgcpprojectid). More details in the [GCP documentation](https://cloud.google.com/iap/docs/signed-headers-howto).

*Environment variable:* `TANAGRA_AUTH_IAP_GKE_JWT`



## Export (Per Model)
Configure the export options for each model.

### tanagra.export.models.displayName
**optional** String

Displayed name of the export model. This is for display only and will be shown in the export dialog when the user initiates an export. Defaults to the display name provided by the export model. It's useful to override the default if you have more than one instance of the same model (e.g. export to workbench parameterized with the dev environment URL, and another parameterized with the test environment URL).

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_DISPLAY_NAME (Note 0 is the list index, so if you have 2 models, you'd have 0 and 1 env vars.)`

*Example value:* `Export File to Workbench (dev instance)`

### tanagra.export.models.name
**optional** String

Name of the export model. This must be unique across all models for a given deployment. Defaults to the name of the export model. It's useful to override the default if you have more than one instance of the same model (e.g. export to workbench parameterized with the dev environment URL, and another parameterized with the test environment URL).

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_NAME (Note 0 is the list index, so if you have 2 models, you'd have 0 and 1 env vars.)`

*Example value:* `VWB_FILE_IMPORT_TO_DEV`

### tanagra.export.models.numPrimaryEntityCap
**optional** String

Maximum number of primary entity instances to allow exporting (e.g. number of persons <= 10k). This is useful when you want to limit the amount of data a user can export e.g. to keep file sizes reasonable. The limit is inclusive, so 10k means <=10k is allowed. Note that this limit applies to the union of all selected cohorts, not each cohort individually. When unset, there is no default cap. This export model will always run, regardless of how many primary entity instances are included in the selected cohorts.

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_NUM_PRIMARY_ENTITY_CAP (Note 0 is the list index, so if you have 2 models, you'd have 0 and 1 env vars.)`

*Example value:* `10000`

### tanagra.export.models.params
**optional** List [ String ]

Map of parameters to pass to the export model. This is useful when you want to parameterize a model beyond just the redirect URL. e.g. A description for a generated notebook file.

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_PARAMS_0 (Note the first 0 is the list index of the export models, so if you have 2 models, you'd have 0 and 1 env vars. The second 0 is the list index of the parameters, so if you have 2 parameters, you'd need 0 and 1 env vars.)`

*Example value:* `Notebook file generated for Workbench v35`

### tanagra.export.models.redirectAwayUrl
**optional** String

URL to redirect the user to once the Tanagra export model has run. This is useful when you want to import a file to another site. e.g. Write the exported data to CSV files in GCS and then redirect to a workbench URL, passing the URL to the CSV files so the workbench can import them somewhere.

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_REDIRECT_AWAY_URL (Note 0 is the list index, so if you have 2 models, you'd have 0 and 1 env vars.)`

*Example value:* `https://terra-devel-ui-terra.api.verily.com/import?urlList=${tsvFileUrl}&returnUrl=${redirectBackUrl}&returnApp=Tanagra`

### tanagra.export.models.type
**optional** Type

Pointer to the data export model Java class. Currently this must be one of the enum values in the`bio.terra.tanagra.service.export.DataExport.Type` Java class. In the future, it will support arbitrary class names

*Environment variable:* `TANAGRA_EXPORT_MODELS_0_TYPE (Note 0 is the list index, so if you have 2 models, you'd have 0 and 1 env vars.)`

*Example value:* `IPYNB_FILE_DOWNLOAD`



## Export (Shared)
Configure the export options shared by all models.

### tanagra.export.shared.bqDatasetIds
**optional** List [ String ]

Comma separated list of all BQ dataset ids that all export models can use. Required if there are any export models that need to export from BQ to GCS.

*Environment variable:* `TANAGRA_EXPORT_SHARED_BQ_DATASET_IDS`

*Example value:* `service_export_us,service_export_uscentral1`

### tanagra.export.shared.gcpProjectId
**optional** String

GCP project id that contains the BQ dataset and GCS bucket(s) that all export models can use. Required if there are any export models that need to export from BQ to GCS.

*Environment variable:* `TANAGRA_EXPORT_SHARED_GCP_PROJECT_ID`

*Example value:* `broad-tanagra-dev`

### tanagra.export.shared.gcsBucketNames
**optional** List [ String ]

Comma separated list of all GCS bucket names that all export models can use. Only include the bucket name, not the gs:// prefix. Required if there are any export models that need to write to GCS.

*Environment variable:* `TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES`

*Example value:* `bq-export-uscentral1,bq-export-useast1`



## Feature Flags
Enable and disable specific features.

### tanagra.feature.activityLogEnabled
**optional** boolean

When true, we store activity log events in the application database. This is intended to support auditing requirements.

*Environment variable:* `TANAGRA_FEATURE_ACTIVITY_LOG_ENABLED`

*Default value:* `false`

### tanagra.feature.artifactStorageEnabled
**optional** boolean

When true, artifacts can be created, updated and deleted. Artifacts include studies, cohorts, concept sets, reviews, and annotations.

*Environment variable:* `TANAGRA_FEATURE_ARTIFACT_STORAGE_ENABLED`

*Default value:* `false`

### tanagra.feature.backendFiltersEnabled
**optional** boolean

When true, we generate filters from criteria selectors on the backend. This is intended to support a transition from frontend to backend filter building.

*Environment variable:* `TANAGRA_FEATURE_BACKEND_FILTERS_ENABLED`

*Default value:* `false`

### tanagra.feature.maxChildThreads
**optional** String

The maximum number of child threads a single request can spawn. The application will only use multi-threading where it could improve performance, so just configuring a specific number here is not a guarantee that exactly that many or even any child threads will be spawned for a given request.

 When unset, the application will default to using multi-threading where it could improve performance. When set to 0, the application will only run things serially. When set to some N > 0 (e.g. 2), the application may spawn at most N child threads.

 (For export, spawning a single child thread would not improve performance, so 0 and 1 cause identical behavior, i.e. run serially in same thread as request.)

*Environment variable:* `TANAGRA_FEATURE_MAX_CHILD_THREADS`



## Underlays
Configure the underlays served.

### tanagra.underlay.files
**required** List [ String ]

Comma-separated list of service configurations. Use the name of the service configuration file only, no extension or path.

*Environment variable:* `TANAGRA_UNDERLAY_FILES`

*Example value:* `cmssynpuf_broad,aouSR2019q4r4_broad`



