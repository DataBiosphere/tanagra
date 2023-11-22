# Deployment Configuration

This file lists all the configuration properties available for a deployment of the service.
You can set the properties either with an `application.yaml` file or with environment variables.
This documentation is generated from annotations in the configuration classes.

## Underlays
Configure the underlays served by the deployment.

### tanagra.underlay.files
**required**

Comma-separated list of service configurations. Use the name of the service configuration file only, no extension or path.

*Environment variable:* `TANAGRA_UNDERLAY_FILES`

*Example value:* `cmssynpuf_broad,aouSR2019q4r4_broad`



