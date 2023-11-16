- [Config Files](#config-files)
  * [Data Mapping](#data-mapping)
    + [Underlay](#underlay)
    + [Entity](#entity)
    + [Entity Group](#entity-group)
  * [Runtime](#data-mapping)
    + [Indexer](#indexer)
    + [Service](#service)

# Config Files
**The mapping from the source data** to Tanagra's [entity model](ENTITY_MODEL.md) along with the location of the 
underlying BigQuery datasets can all be configured. We separate the data mapping configuration from the runtime 
configuration. Data mapping configuration can be reused across multiple indexing runs or service deployments.

- All config files live in the [main resources directory](../underlay/src/main/resources/config/) of the `underlay` Gradle sub-project.
- The config schemas are defined in Java classes in the [serialization package](../underlay/src/main/java/bio/terra/tanagra/underlay/serialization/).
- Documentation for all available properties in the config schemas are generated as Javadoc [here](https://htmlpreview.github.io/?https://github.com/DataBiosphere/tanagra/blob/main/docs/generated/underlay_config/allclasses-index.html).

## Data Mapping
These config files specify the mapping from the source data to Tanagra's entity model.

### Underlay
The top-level config file defines the list of entities and entity groups, along with metadata. Each entity and entity
group has its own set of config files.

- Example underlay config file for the public CMS SynPUF dataset is [here](../underlay/src/main/resources/config/underlay/cmssynpuf/underlay.json).
- Documentation for all available properties in the underlay config schema is [here](https://htmlpreview.github.io/?https://github.com/DataBiosphere/tanagra/blob/main/docs/generated/underlay_config/bio/terra/tanagra/underlay/serialization/SZUnderlay.html).

### Entity
This config file defines the list of attributes, hierarchies, and text search. These files can be shared across underlays
(e.g. the definition of an ICD9-CM code can be shared across the SD and CMS SynPUF underlays). The main config file
references SQL files in the same directory. These SQL files should never include direct dataset references; instead
they should use SQL substitutions to preserve their reusability across underlays and dataset locations.

- Example entity config files for the public CMS SynPUF dataset are [here](../underlay/src/main/resources/config/datamapping/omop/entity/condition/).
- Documentation for all available properties in the entity config schema is [here](https://htmlpreview.github.io/?https://github.com/DataBiosphere/tanagra/blob/main/docs/generated/underlay_config/bio/terra/tanagra/underlay/serialization/SZEntity.html).

### Entity Group
This config file defines the relationships between multiple entities. These files can be shared across underlays (e.g. the
definition of the brand-ingredient relationship can be shared across the SD and CMS SynPUF underlays). The main config
file references SQL files in the same directory. These SQL files should never include direct dataset references; instead
they should use SQL substitutions to preserver their reusability across underlays and dataset locations.

There are currently two types of entity groups: `GroupItems` and `CriteriaOccurrence`. Each has a different config file schema.

- Example `GroupItems` entity group config files for the public CMS SynPUF dataset are [here](../underlay/src/main/resources/config/datamapping/omop/entitygroup/brandIngredient/).
- Documentation for all available properties in the `GroupItems` config schema is [here](https://htmlpreview.github.io/?https://github.com/DataBiosphere/tanagra/blob/main/docs/generated/underlay_config/bio/terra/tanagra/underlay/serialization/SZGroupItems.html).


- Example `CriteriaOccurrence` entity group config files for the public CMS SynPUF dataset are [here](../underlay/src/main/resources/config/datamapping/omop/entitygroup/conditionPerson/).
- Documentation for all available properties in the `CriteriaOccurrence` config schema is [here](https://htmlpreview.github.io/?https://github.com/DataBiosphere/tanagra/blob/main/docs/generated/underlay_config/bio/terra/tanagra/underlay/serialization/SZCriteriaOccurrence.html).

## Runtime
These config files specify pointers to where the data lives and configuration for the indexer and service.

### Indexer
This config file specifies the underlay to be indexed, pointers to the source and index datasets, and Dataflow config
properties. A single underlay can be referenced by multiple indexer configs. This is useful when you want to index
the same underlay in two places (e.g. SD dataset is indexed on both Verily and VUMC infrastructure).

- Example indexer config file for the public CMS SynPUF dataset on Verily infrastructure is [here](../underlay/src/main/resources/config/indexer/cmssynpuf_verily.json).
- Documentation for all available properties in the indexer config schema is [here](https://htmlpreview.github.io/?https://github.com/DataBiosphere/tanagra/blob/main/docs/generated/underlay_config/bio/terra/tanagra/underlay/serialization/SZIndexer.html).

### Service
This config file specifies the underlay to be served, pointers to the source and index datasets, and UI config.
A single underlay can be referenced by multiple service configs. This is useful when you want to index an underlay
once, but serve it in two deployments (e.g. SD dataset is indexed in one project, and used by both the dev and
staging environments).

- Example service config file for the public CMS SynPUF dataset on Verily infrastructure is [here](../underlay/src/main/resources/config/service/cmssynpuf_verily.json).
- Documentation for all available properties in the service config schema is [here](https://htmlpreview.github.io/?https://github.com/DataBiosphere/tanagra/blob/main/docs/generated/underlay_config/bio/terra/tanagra/underlay/serialization/SZService.html).
