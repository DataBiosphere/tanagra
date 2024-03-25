# Tanagra

Tanagra is a project to build a configurable cohort builder and data explorer.
Our goal is to make it easy to set up a new dataset for exploring with little or no custom code required,
so everything we've built is configuration-driven.


## Overview
The project has three main pieces: **indexer**, **service**, **UI**.
All three pieces are highly interconnected and are not intended to be used or deployed separately,
so everything lives in this single GitHub repository.

The **indexer** takes the source dataset and produces a logical copy that's better suited to the types
of queries the UI needs to run. It denormalizes some data, precomputes some things, and reorganizes
tables. The goal is not to meet some query benchmark, only to have the UI not time out.

The **service** processes queries for the UI and manages the application database, which stores user-managed
artifacts like cohorts and data feature sets.

The **UI** includes the cohort builder, data feature set builder, export, and cohort review interfaces.


## Configure a new dataset
Tanagra supports data patterns, instead of specific SQL schemas.
Check the list of [currently supported patterns](./docs/DATA_PATTERNS.md) to see how they map to your dataset.

Tanagra defines a [custom object model](./docs/ENTITY_MODEL.md) on top of the underlying relational data.
The dataset configuration language is based on this object model, so it's helpful to be familiar with the main concepts.

A dataset configuration is spread across multiple files, to improve readability and allow easier sharing across datasets.
See an [overview of the different files and directory structure](./docs/CONFIG_FILES.md), as well as pointers to example files.
Check the full [dataset configuration schema documentation](./docs/generated/UNDERLAY_CONFIG.md) to lookup specific properties.


## Set up a new deployment
Choose a [deployment pattern](./docs/DEPLOYMENT_OVERVIEW.md) and configure the GCP project(s).

Once you've defined the configuration files for a dataset, [run the indexer](./docs/INDEXING.md).
Check the full [indexer CLI documentation](./docs/generated/indexer-cli/tanagra.adoc) to lookup specific commands.

Tanagra does not provide an API for managing access control for a population of users.
Instead, we provide an [interface for calling an external access control service](./docs/ACCESS_CONTROL.md).
(e.g. The [VUMC admin service](./docs/VUMC_ADMIN_SERVICE.md) serves as the external access control service for the SD deployment.)
Either reuse an existing access control implementation, or add your own.

We expect deployments to require varied methods of [exporting data](./docs/DATA_EXPORT.md).
Either reuse an existing export implementation, or add your own.

Check the full [application configuration documentation](./docs/generated/APPLICATION_CONFIG.md) to lookup specific 
deployment properties.


## Manage releases
Tanagra supports multiple deployments, all with different release cadences.
Each deployment needs to manage which release it's on, in a separate downstream GitHub repository.

Use this [tool to diff two release tags](./docs/DIFF_RELEASES.md), when you're planning on bumping a deployment to a 
newer version of the mainstream code.


## Contribute to the codebase
See an [overview of the codebase structure](./docs/CODEBASE_OVERVIEW.md),
and information [specifically about the UI](./docs/UI.md).

Check the [guidelines for developers](./docs/CONTRIBUTING.md), including instructions for getting things running 
locally on your machine.


## All documentation links
These are all linked in the sections above. This is just in list format if you already know what you're looking for.

Configure a new dataset
* [Data Patterns](./docs/DATA_PATTERNS.md)
* [Entity Model](./docs/ENTITY_MODEL.md)
* [Config Files](./docs/CONFIG_FILES.md)
* [Underlay Config Properties](./docs/generated/UNDERLAY_CONFIG.md)

Set up a new deployment
* [Deployment Overview](./docs/DEPLOYMENT_OVERVIEW.md)
* [Indexing](./docs/INDEXING.md)
* [Indexer CLI Manpage](./docs/generated/indexer-cli/tanagra.adoc)
* [Access Control](./docs/ACCESS_CONTROL.md)
* [VUMC Admin Service](./docs/VUMC_ADMIN_SERVICE.md)
* [Data Export](./docs/DATA_EXPORT.md)
* [Deployment Config Properties](./docs/generated/APPLICATION_CONFIG.md)

Manage releases
* [Diff Releases](./docs/DIFF_RELEASES.md)

Contribute to the codebase
* [Codebase Overview](./docs/CODEBASE_OVERVIEW.md)
* [UI](./docs/UI.md)
* [Contribute Code](./docs/CONTRIBUTING.md)


## Codebase test status

![Underlay Tests](https://github.com/DataBiosphere/tanagra/actions/workflows/underlay-test.yaml/badge.svg?branch=main)

![Indexer Tests](https://github.com/DataBiosphere/tanagra/actions/workflows/indexer-test.yaml/badge.svg?branch=main)

![Service (PostGres) Tests](https://github.com/DataBiosphere/tanagra/actions/workflows/service-test-postgres.yaml/badge.svg?branch=main)

![Service (MariaDB) Tests](https://github.com/DataBiosphere/tanagra/actions/workflows/service-test-mariadb.yaml/badge.svg?branch=main)

![UI Tests](https://github.com/DataBiosphere/tanagra/actions/workflows/ui-test.yaml/badge.svg?branch=main)

![UI Integration Tests](https://github.com/DataBiosphere/tanagra/actions/workflows/ui-integration-test.yaml/badge.svg?branch=main)

![Generated Files](https://github.com/DataBiosphere/tanagra/actions/workflows/generated-files.yaml/badge.svg?branch=main)
