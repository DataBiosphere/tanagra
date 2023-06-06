# Configure Access Control

## Varied access control models
Access control models will vary across deployments of Tanagra (e.g. Verily, AoU, VUMC).
We expect one common pattern will be to forward access control checks to another service (e.g. SAM, CWB, VUMC admin).
For this reason, we want to make supporting a new access control model straightforward and configurable.

Each service endpoint checks that the user is allowed to perform the operation before calling any internal business logic.
This check is implemented as a function call to an implementation class of the `AccessControl` interface.
The service can be configured to call any class that implements this interface.

There are 4 steps to configuring Tanagra to use a new access control model:
1. **Write a class that implements the `bio.terra.tanagra.service.accesscontrol.AccessControl` interface.**
   - Existing implementation classes live in the `bio.terra.tanagra.service.accesscontrol.impl` package. Please put yours there also.
   - You can add any additional dependencies directly to the `service/build.gradle` file.
2. **Add a new value to `AccessControl.Model` enum.**
   - This is the enum value you will use to specify your access control model in the application configuration.
3. **Change the application configuration to point to your class and supply any parameters.**
   - Set `model` to the enum value defined in the previous step.
   - The `params`, `base-path`, and `oauth-client-id` config properties are all optional. They will be passed to your
     implementation class in the `initialize` method, so you can use or ignore them.
```
tanagra:
    access-control:
        model: OPEN_ACCESS
        params: []
        base-path:
        oauth-client-id:
```
4. **[Optional] Add a new `@Disabled` test method to the `AccessControlImplTest` class.**
   - It should instantiate and initialize your implementation class, and maybe call some of its methods.
   - This is intended to help with debugging any problems.

## Client library dependencies
The code for each access control implementation lives in a separate class. 
So while not technically impossible, we don't expect shared logic or deep integrations with the rest of the codebase.

Since all implementation classes are part of the Tanagra service codebase, they share a build file `service/build.gradle`.
This means that dependencies for each of the access control implementations (e.g. client library for calling a separate 
service) all have to coexist. So far, there haven't been any dependency conflicts, and we're not expecting many more of 
these implementations in the near term (say < 5).

If we do run into dependency conflicts in the future, we should first try to resolve them, so that building Tanagra is
as consistent across deployments as possible. If we can't resolve them for whatever reason (e.g. two client libraries 
require incompatible versions of some other library), or there's an access control implementation that we don't want to 
check into this main Tanagra GH repo, then we could allow access control implementation classes in a separate JAR that 
gets added to the classpath at runtime. We'd need to update the `access-control.model` application property to take a 
classname instead of using the `AccessControl.Model` enum, and then we can load that class using reflection at runtime. 

## Access control implementations
So far, there are 3 access control implementations in the `bio.terra.tanagra.service.accesscontrol.impl` package.

### Open Access Control
Allow everything for everyone. This is helpful when developing locally and running tests.
```
tanagra:
    access-control:
        model: OPEN_ACCESS
        params: []
        base-path:
        oauth-client-id:
```

### VumcAdmin Access Control
Access control is enforced on studies only. For studies and their child artifacts (e.g. cohorts), send a request to
the VUMC admin service to check access on the study. For underlays, allow everything. This was written to support the
SDD deployment.
```
tanagra:
    access-control:
        model: VUMC_ADMIN
        params: []
        base-path: https://tanagra-test.app.vumc.org
        oauth-client-id: 12345.apps.googleusercontent.com    <--- Example value only. Get this from the Cloud Console.
```

### VerilyGroups Access Control
Access control is enforced on underlays only. For underlays, send a request to the VerilyGroups API to check membership.
For studies and their child artifacts, allow everything. This was written to support the Verily dev deployment.

`params` is a list of `underlayName,verilyGroupName` pairs. Each underlay can have a different set of allowed users. 
The special `ALL_ACCESS` key means that members of the accompanying group have access to all underlays.
```
tanagra:
    access-control:
        model: VERILY_GROUP
        params: [ALL_ACCESS,tanagra-dev-all, sdd_refresh0323,tanagra-dev-sdd, cms_synpuf,tanagra-dev-public]
        base-path: https://www.verilygroups.com
        oauth-client-id: 12345.apps.googleusercontent.com    <--- Example value only. Get this from the VerilyGroups docs.
```
