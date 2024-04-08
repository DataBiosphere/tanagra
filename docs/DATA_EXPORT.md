# Configure Data Export

## Varied data export models
Data export models will vary across deployments of Tanagra (e.g. Verily, AoU, VUMC).
For this reason, we want to make supporting a new export model straightforward and configurable.
We expect one common pattern will be to write a file of some format, then redirect to another URL (e.g. VWB, CWB)
with a link to the file.

Users can export data from the Dataset Preview page, by selecting one or more cohorts, data features, and values.
This export is implemented as a function call to an implementation class of the `DataExport` interface.
The service can be configured to call any class that implements this interface.

There are 4 steps to configuring Tanagra to use a new data export model:
1. **Write a class that implements the `bio.terra.tanagra.service.export.DataExport` interface.**
    - Existing implementation classes live in the `bio.terra.tanagra.service.export.impl` package. Please put yours there also.
    - You can add any additional dependencies directly to the `service/build.gradle` file.
2. **Add a new value to `DataExport.Type` enum.**
    - This is the enum value you will use to specify your data export model in the application configuration.
3. **Change the application configuration to point to your class and supply any parameters.**
    - Set `type` to the enum value defined in the previous step.
    - The `name`, `display-name`, `redirect-away-url`, and `params` config properties are all optional. They will be 
      passed to your implementation class in the `initialize` method, so you can use or ignore them.
```
tanagra:
  export:
    models:
      -
        name: VWB_FILE_IMPORT_DEVEL
        displayName: Import to VWB (devel)
        type: VWB_FILE_IMPORT
        redirect-away-url: https://terra-devel-ui-terra.api.verily.com/import?urlList=${tsvFileUrl}&returnUrl=${redirectBackUrl}&returnApp=Tanagra
        params: []
```
4. **[Optional] Add a new test method to the `DataExportServiceTest` class.**
    - It should call the `DataExportService.run` method, specifying your implementation class and validating the results.
    - If it needs specific credentials or other configuration that you don't want to check into the main GH repo, then
      you can mark the test `@Disabled`.
    - This is intended to help with debugging any problems.

## Multiple export models per deployment
A data export **model** = A data export **type/implementation class** + any parameters.
Each deployment may have >1 export models (e.g. zip of csv files, ipynb file to CWB) and >1 export models of the same
type (e.g. csv files to VWB dev, csv files to VWB prod). If you have >1 export models of the same type (i.e. two
instances of an implementation class with different parameters), then you must override the default `name` and `displayName`
parameters, to distinguish between them.

## Shared configuration across models
The export configuration contains a `shared` section with parameters that are made available to all data export
models. Currently, this section contains references to GCS buckets where data export implementation classes
can write files. In the future, we may add other cloud resource pointers here (e.g AWS S3 bucket). You must define
these properties if any of the data export models you define need them.
```
  export:
    shared:
      gcs-bucket-project-id: broad-tanagra-dev
      gcs-bucket-names: broad-tanagra-dev-bq-export
```

## Client library dependencies
The code for each data export implementation lives in a separate class.
So while not technically impossible, we don't expect shared logic or deep integrations with the rest of the codebase.

Since all implementation classes are part of the Tanagra service codebase, they share a build file `service/build.gradle`.
This means that dependencies for each of the data export implementations (e.g. client library for calling a separate
service) all have to coexist. So far, there haven't been any dependency conflicts, and we're not expecting many more of
these implementations in the near term (say < 5).

If we do run into dependency conflicts in the future, we should first try to resolve them, so that building Tanagra is
as consistent across deployments as possible. If we can't resolve them for whatever reason (e.g. two client libraries
require incompatible versions of some other library), or there's a data export implementation that we don't want to
check into this main Tanagra GH repo, then we could allow data export implementation classes in a separate JAR that
gets added to the classpath at runtime. We'd need to update the `export.models.type` application property to take a
classname instead of using the `DataExport.Type` enum, and then we can load that class using reflection at runtime.

## Set config with env vars
Tanagra's core service uses the Spring application framework, which allows overriding application properties with
environment variables. Note this is a Spring feature, not specific to Tanagra. It's helpful at deploy-time for setting  
properties with information you don't want checked into this GH repo.

e.g. For a deployment that uses the individual file download export model only:
```
export TANAGRA_EXPORT_SHARED_GCS_BUCKET_PROJECT_ID=broad-tanagra-dev
export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=broad-tanagra-dev-bq-export

export TANAGRA_EXPORT_MODELS_0_TYPE=INDIVIDUAL_FILE_DOWNLOAD
```


## Data export implementations
So far, there are 4 data export implementations in the `bio.terra.tanagra.service.export.impl` package.

### Individual file download
Download individual files for each query result and cohort annotation data. The implementation writes these files
to a GCS bucket and generates a signed URL for each. The signed URLs timeout after 30 minutes.
```
tanagra:
  export:
    shared:
      gcs-bucket-project-id: broad-tanagra-dev
      gcs-bucket-names: broad-tanagra-dev-bq-export
    models:
      -
        type: INDIVIDUAL_FILE_DOWNLOAD
```

### Verily Workbench file import
Import individual files for each query result and cohort annotation data to a Verily Workbench workspace. The
implementation writes these files to a GCS bucket and generates a signed URL for each. Then it generates another file
that contains a list of all the signed URLs. It writes this file to GCS and generates a signed URL for it, too. All
signed URLs timeout after 30 minutes. Finally, it generates a redirect URL to Verily Workbench that includes this last
signed URL.

`redirect-away-url` is a template with substitution parameters. The implementation class will set the `tsvFileUrl` and
`redirectBackUrl` and return the hydrated URL to the caller.
```
tanagra:
  export:
    shared:
      gcs-bucket-project-id: broad-tanagra-dev
      gcs-bucket-names: broad-tanagra-dev-bq-export
    models:
      -
        name: VWB_FILE_IMPORT_DEVEL
        displayName: Import to VWB (devel)
        type: VWB_FILE_IMPORT
        redirect-away-url: https://terra-devel-ui-terra.api.verily.com/import?urlList=${tsvFileUrl}&returnUrl=${redirectBackUrl}&returnApp=Tanagra
```

### Ipynb file download
Download a notebook file (`.ipynb` format) with an embedded SQL query for all primary entity instances (e.g. all `person`s)
in the selected cohorts. The implementation writes this file to a GCS bucket and generates a signed URL for it.
```
tanagra:
  export:
    shared:
      gcs-bucket-project-id: broad-tanagra-dev
      gcs-bucket-names: broad-tanagra-dev-bq-export
    models:
      -
        type: IPYNB_FILE_DOWNLOAD
```

### Regression test file download
Download a regression test file (`.json` format) with the definitions of all selected cohorts and data feature sets,
and the total number of rows that would be generated for each output table. The implementation writes this file to a 
GCS bucket and generates a signed URL for it. Read about the [regression testing framework](./REGRESSION_TESTING.md) 
for more information.
```
tanagra:
  export:
    shared:
      gcs-bucket-project-id: broad-tanagra-dev
      gcs-bucket-names: broad-tanagra-dev-bq-export
    models:
      -
        type: REGRESSION_TEST
```
