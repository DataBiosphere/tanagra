# Deployment Configuration

This file lists all the configuration properties available for a deployment of the service.
You can set the properties either with an `application.yaml` file or with environment variables.
This documentation is generated from annotations in the configuration classes.

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



