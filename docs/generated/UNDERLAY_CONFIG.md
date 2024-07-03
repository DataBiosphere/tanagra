# Underlay Configuration

This file lists all the configuration properties available for an underlay, including defining the data mapping and the indexing and service deployment data pointers. 
This documentation is generated from annotations in the configuration classes.

* [SZAttribute](#szattribute)
* [SZBigQuery](#szbigquery)
* [SZCorePlugin](#szcoreplugin)
* [SZCriteriaOccurrence](#szcriteriaoccurrence)
* [SZCriteriaRelationship](#szcriteriarelationship)
* [SZCriteriaSelector](#szcriteriaselector)
* [SZCriteriaSelectorDisplay](#szcriteriaselectordisplay)
* [SZCriteriaSelectorModifier](#szcriteriaselectormodifier)
* [SZDataType](#szdatatype)
* [SZDataflow](#szdataflow)
* [SZEntity](#szentity)
* [SZGroupItems](#szgroupitems)
* [SZHierarchy](#szhierarchy)
* [SZIndexData](#szindexdata)
* [SZIndexer](#szindexer)
* [SZMetadata](#szmetadata)
* [SZOccurrenceEntity](#szoccurrenceentity)
* [SZPrepackagedCriteria](#szprepackagedcriteria)
* [SZPrimaryCriteriaRelationship](#szprimarycriteriarelationship)
* [SZPrimaryRelationship](#szprimaryrelationship)
* [SZService](#szservice)
* [SZSourceData](#szsourcedata)
* [SZSourceQuery](#szsourcequery)
* [SZTemporalQuery](#sztemporalquery)
* [SZTextSearch](#sztextsearch)
* [SZUnderlay](#szunderlay)
* [SZVisualization](#szvisualization)

## SZAttribute
Attribute or property of an entity.

Define an attribute for each column you want to display (e.g. `condition.vocabulary_id`) or filter on (e.g. `conditionOccurrence.person_id`).

### SZAttribute.dataType
**required** [SZDataType](#szdatatype)

Data type of the attribute.

### SZAttribute.displayFieldName
**optional** String

Field or column name in the [all instances SQL file](#szentityallinstancessqlfile) that maps to the display string of this attribute. If unset, we assume the attribute has only a value, no separate display.

A separate display field is useful for enum-type attributes, which often use a foreign-key to another table to get a readable string from a code (e.g. in OMOP, `person.gender_concept_id` and `concept.concept_name`).

### SZAttribute.displayHintRangeMax
**optional** Double

The maximum value to display when filtering on this attribute. This is useful when the underlying data has outliers that we want to exclude from the display, but not from the available data.

e.g. A person has an invalid date of birth that produces an age range that spans very large numbers. This causes the slider when filtering by age to span very large numbers also. Setting this property sets the right end of the slider. It does not remove the person with the invalid date of birth from the table. So if they have asthma, they would still show up in a cohort filtering on this condition.

The #szattributedisplayhintrangemin may be set as well, but they are not required to be set together. The #szattributeiscomputedisplayhint is also independent of this property. You can still calculate the actual maximum in the data, if you set this property.

### SZAttribute.displayHintRangeMin
**optional** Double

The minimum value to display when filtering on this attribute. This is useful when the underlying data has outliers that we want to exclude from the display, but not from the available data.

e.g. A person has an invalid date of birth that produces an age range that spans negative numbers. This causes the slider when filtering by age to span negative numbers also. Setting this property sets the left end of the slider. It does not remove the person with the invalid date of birth from the table. So if they have asthma, they would still show up in a cohort filtering on this condition.

The #szattributedisplayhintrangemax may be set as well, but they are not required to be set together. The #szattributeiscomputedisplayhint is also independent of this property. You can still calculate the actual minimum in the data, if you set this property.

### SZAttribute.isComputeDisplayHint
**optional** boolean

When set to true, an indexing job will try to compute a display hint for this attribute (e.g. set of enum values and counts, range of numeric values). Not all data types are supported by the indexing job, yet.

*Default value:* `false`

### SZAttribute.isSuppressedForExport
**optional** boolean

True if this attribute is suppressed for export (i.e. not available for selection in data feature sets).

*Default value:* `false`

### SZAttribute.name
**required** String

Name of the attribute.

This is the unique identifier for the attribute. In a single entity, the attribute names cannot overlap.

Name may not include spaces or special characters, only letters and numbers. The first character must be a letter.

### SZAttribute.runtimeDataType
**optional** [SZDataType](#szdatatype)

Data type of the attribute at runtime.

If the [runtime SQL wrapper](#szattributeruntimesqlfunctionwrapper) is set, this field must also be set. The data type at runtime may be different from the data type at rest when the column is passed to a function at runtime. Otherwise, the data type at runtime will always match the attribute [data type](#szdatatype), so no need to specify it again here.

### SZAttribute.runtimeSqlFunctionWrapper
**optional** String

SQL function to apply at runtime (i.e. when running the query), instead of at indexing time. Useful for attributes we expect to be updated dynamically (e.g. a person's age).

For a simple function call that just wraps the column (e.g. `UPPER(column)`), you can specify just the function name (e.g. `UPPER`). For a more complicated function call, put `${fieldSql}` where the column name should be substituted (e.g. `CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), ${fieldSql}, DAY) / 365.25) AS INT64)`).

Note that BigQuery disallows query caching and pagination for [certain non-deterministic functions](https://cloud.google.com/bigquery/docs/cached-results#cache-exceptions). This negatively impacts query performance and prevents certain application behaviors that we want to support. So we workaround this for certain functions (CURRENT_DATE and CURRENT_TIMESTAMP) commonly used in runtime SQL wrappers by replacing the function with the current date/timestamp literal at runtime. You can include these functions normally in this property; the replacement will happen automatically.

### SZAttribute.sourceQuery
**optional** [SZSourceQuery](#szsourcequery)

How to generate a query against the source data that includes this attribute.

If unspecified and exporting queries against the source data is supported for this entity is enabled (i.e. #szentitysourcequerytablename is specified), we assume the field name in the source table (#szentitysourcequerytablename) corresponding to this attribute is the same as the #szattributevaluefieldname.

### SZAttribute.valueFieldName
**optional** String

Field or column name in the [all instances SQL file](#szentityallinstancessqlfile) that maps to the value of this attribute. If unset, we assume the field name is the same as the attribute name.



## SZBigQuery
Pointers to the source and index BigQuery datasets.

### SZBigQuery.dataLocation
**required** String

Valid locations for BigQuery are listed in the GCP [documentation](https://cloud.google.com/bigquery/docs/locations).

### SZBigQuery.exportBucketNames
**optional** List [ String ]

Comma separated list of all GCS bucket names that all export models can use. Only include the bucket name, not the gs:// prefix. Required if there are any export models that need to write to GCS.

These buckets must live in the [query project](#szbigqueryqueryprojectid) specified above.

You can also specify these export buckets per-deployment, instead of per-underlay, by using the service application properties.

*Example value:* `bq-export-uscentral1,bq-export-useast1`

### SZBigQuery.exportDatasetIds
**optional** List [ String ]

Comma separated list of all BQ dataset ids that all export models can use. Required if there are any export models that need to export from BQ to GCS.

These datasets must live in the [query project](#szbigqueryqueryprojectid) specified above.

You can also specify these export datasets per-deployment, instead of per-underlay, by using the service application properties.

*Example value:* `service_export_us,service_export_uscentral1`

### SZBigQuery.indexData
**required** [SZIndexData](#szindexdata)

Pointer to the index BigQuery dataset.

### SZBigQuery.queryProjectId
**required** String

Queries will be run in this project.

This is the project that will be billed for running queries. For the indexer, this project is also where the Dataflow jobs will be kicked off. Often this project will be the same project as the one where the index and/or source datasets live.

However, sometimes it will be different. For example, the source dataset may be a public dataset that we don't have billing access to. In that case, the indexer configuration must specify a different query project id. As another example, the source and index datasets may live in a project that is shared across service deployments. In that case, the service configurations may specify a different query project id for each deployment.

### SZBigQuery.sourceData
**required** [SZSourceData](#szsourcedata)

Pointer to the source BigQuery dataset.



## SZCorePlugin
Names of core plugins in the criteria selector and prepackaged criteria definitions.

### SZCorePlugin.ATTRIBUTE
**required** [SZCorePlugin](#szcoreplugin)

Use `plugin: "attribute"`.

### SZCorePlugin.ENTITY_GROUP
**required** [SZCorePlugin](#szcoreplugin)

Use `plugin: "entityGroup"`.

### SZCorePlugin.MULTI_ATTRIBUTE
**required** [SZCorePlugin](#szcoreplugin)

Use `plugin: "multiAttribute"`.

### SZCorePlugin.OUTPUT_UNFILTERED
**required** [SZCorePlugin](#szcoreplugin)

Use `plugin: "outputUnfiltered"`.

### SZCorePlugin.TEXT_SEARCH
**required** [SZCorePlugin](#szcoreplugin)

Use `plugin: "search"`.

### SZCorePlugin.UNHINTED_VALUE
**required** [SZCorePlugin](#szcoreplugin)

Use `plugin: "unhinted-value"`.



## SZCriteriaOccurrence
Criteria-Occurrence entity group configuration.

Define a version of this file for each entity group of this type. This entity group type defines a relationship between three entities. For each criteria entity instance and primary entity instance, there are one or more occurrence entity instances.

### SZCriteriaOccurrence.criteriaEntity
**required** String

Name of the criteria entity.

### SZCriteriaOccurrence.name
**required** String

Name of the entity group.

This is the unique identifier for the entity group. In a single underlay, the entity group names of any group type cannot overlap. Name may not include spaces or special characters, only letters and numbers. The first character must be a letter.

### SZCriteriaOccurrence.occurrenceEntities
**required** Set [ SZCriteriaOccurrence$OccurrenceEntity ]

Set of occurrence entity configurations.

Most entity groups of this type will have a single occurrence entity (e.g. SNOMED condition code only maps to condition occurrences), but we also support the case of multiple (e.g. ICD9-CM condition code maps to condition, measurement, observation and procedure occurrences).

### SZCriteriaOccurrence.primaryCriteriaRelationship
**required** [SZPrimaryCriteriaRelationship](#szprimarycriteriarelationship)

Relationship or join between the primary and criteria entities.



## SZCriteriaRelationship
Relationship or join between an occurrence entity and the criteria entity (e.g. condition occurrence and ICD9-CM).

### SZCriteriaRelationship.criteriaEntityIdFieldName
**optional** String

Name of the field or column name that maps to the criteria entity id. Required if the [id pairs SQL](#szcriteriarelationshipidpairssqlfile) is defined.

*Example value:* `criteria_id`

### SZCriteriaRelationship.foreignKeyAttributeOccurrenceEntity
**optional** String

Attribute of the occurrence entity that is a foreign key to the id attribute of the criteria entity. If this property is set, then the [id pairs SQL](#szcriteriarelationshipidpairssqlfile) must be unset.

### SZCriteriaRelationship.idPairsSqlFile
**optional** String

Name of the occurrence entity - criteria entity id pairs SQL file. File must be in the same directory as the entity group file. Name includes file extension. If this property is set, then the [foreign key attribute](#szcriteriarelationshipforeignkeyattributeoccurrenceentity) must be unset.

There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), but the occurrence and criteria entity ids are required.

*Example value:* `occurrenceCriteria.sql`

### SZCriteriaRelationship.occurrenceEntityIdFieldName
**optional** String

Name of the field or column name that maps to the occurrence entity id. Required if the [id pairs SQL](#szcriteriarelationshipidpairssqlfile) is defined.

*Example value:* `occurrence_id`



## SZCriteriaSelector
Criteria selector configuration.

Define a version of this file for each set of UI plugins + configuration.

### SZCriteriaSelector.display
**required** [SZCriteriaSelectorDisplay](#szcriteriaselectordisplay)

Display information.

### SZCriteriaSelector.displayName
**required** String

Display name.

### SZCriteriaSelector.filterBuilder
**required** String

Name of a Java class that implements the `FilterBuilder` interface. This class will take in the selector configuration and user selections and produce an `EntityFilter` on either the primary entity (for a cohort) or another entity (for a data feature).

### SZCriteriaSelector.isEnabledForCohorts
**required** boolean

True if this criteria selector should be displayed in the cohort builder.

### SZCriteriaSelector.isEnabledForDataFeatureSets
**required** boolean

True if this criteria selector should be displayed in the data feature set builder.

### SZCriteriaSelector.modifiers
**required** List [ SZCriteriaSelector$Modifier ]

Configuration for modifiers.

### SZCriteriaSelector.name
**required** String

Name of the criteria selector.

This is the unique identifier for the selector. The selector names cannot overlap within an underlay.

Name may not include spaces or special characters, only letters and numbers.

This name is stored in the application database for cohorts and data feature sets, so once there are artifacts associated with a criteria selector, you can't change the selector name.

### SZCriteriaSelector.plugin
**required** String

Name of the primary UI display plugin. (e.g. selector for condition, not any of the modifiers).

This plugin name is stored in the application database, so once there are cohorts or data features that use this selector, you can't change the plugin names.

The plugin can either be one of the core plugins (e.g. core/attribute, all possibilities are listed [here](#szcoreplugin), or a dataset-specific plugin (e.g. sd/biovu).

### SZCriteriaSelector.pluginConfig
**required** String

Serialized configuration of the primary UI display plugin e.g. "{"attribute":"gender"}".

### SZCriteriaSelector.pluginConfig
**required** String

Name of the file that contains the serialized configuration of the primary UI display plugin.

This file should be in the same directory as the criteria selector (e.g. `gender.json`).

If this property is specified, the value of the `pluginConfig` property is ignored.



## SZCriteriaSelectorDisplay
Criteria selector display configuration.

### SZCriteriaSelectorDisplay.category
**required** String

Category that the criteria selector is listed under when a user goes to 

add a new criteria. (e.g. "Vitals")

### SZCriteriaSelectorDisplay.tags
**required** List [ String ]

Tags that the criteria selector should match when a user uses the dropdown in the add new

criteria page. (e.g. "Source Codes")



## SZCriteriaSelectorModifier
Criteria selector display configuration.

### SZCriteriaSelectorModifier.displayName
**required** String

Display name.

### SZCriteriaSelectorModifier.name
**required** String

Name of the criteria selector modifier.

This is the unique identifier for the modifier. The modifier names cannot overlap within a selector.

Name may not include spaces or special characters, only letters and numbers.

This name is stored in the application database for cohorts and data feature sets, so once there are artifacts associated with a modifier, you can't change the modifier name.

### SZCriteriaSelectorModifier.plugin
**required** String

Name of the modifier UI display plugin. (e.g. selector for condition visit type).

This plugin name is stored in the application database, so once there are cohorts or data features that use this modifier, you can't change the plugin names.

The plugin can either be one of the core plugins (e.g. core/attribute, all possibilities are listed [here](#szcoreplugin), or a dataset-specific plugin (e.g. sd/biovu).

### SZCriteriaSelectorModifier.pluginConfig
**required** String

Serialized configuration of the modifier UI display plugin e.g. "{"attribute":"visitType"}".

### SZCriteriaSelectorModifier.pluginConfig
**required** String

Name of the file that contains the serialized configuration of the modifier UI display plugin.

This file should be in the same directory as the criteria selector (e.g. `visitType.json`).

If this property is specified, the value of the `pluginConfig` property is ignored.



## SZDataType
Supported data types. Each type corresponds to one or more data types in the underlying database.

### SZDataType.BOOLEAN
**required** [SZDataType](#szdatatype)

Maps to BigQuery `BOOLEAN` data type.

### SZDataType.DATE
**required** [SZDataType](#szdatatype)

Maps to BigQuery `DATE` data type.

### SZDataType.DOUBLE
**required** [SZDataType](#szdatatype)

Maps to BigQuery `NUMERIC` and `FLOAT` data types.

### SZDataType.INT64
**required** [SZDataType](#szdatatype)

Maps to BigQuery `INTEGER` data type.

### SZDataType.STRING
**required** [SZDataType](#szdatatype)

Maps to BigQuery `STRING` data type.

### SZDataType.TIMESTAMP
**required** [SZDataType](#szdatatype)

Maps to BigQuery `TIMESTAMP` data type.



## SZDataflow
Properties to pass to Dataflow when kicking off jobs.

### SZDataflow.dataflowLocation
**required** String

Location where the Dataflow runners will be launched.

This must be compatible with the location of the source and index BigQuery datasets. Note the valid locations for [BigQuery](https://cloud.google.com/bigquery/docs/locations) and [Dataflow](https://cloud.google.com/dataflow/docs/resources/locations) are not identical. In particular, BigQuery has multi-regions (e.g. `US`) and Dataflow does not. If the BigQuery datasets are located in a region, the Dataflow location must match. If the BigQuery datasets are located in a multi-region, the Dataflow location must be one of the sub-regions (e.g. `US` for BigQuery, `us-central1` for Dataflow).

### SZDataflow.gcsTempDirectory
**optional** String

GCS directory where the Dataflow runners will write temporary files.

The bucket location must match the [Dataflow location](#szdataflowdataflowlocation). This cannot be a path to a top-level bucket, it must contain at least one directory (e.g. `gs://mybucket/temp/` not `gs://mybucket`. If this property is unset, Dataflow will attempt to create a bucket in the correct location. This may fail if the credentials don't have permissions to create buckets. More information in the Dataflow pipeline basic options [documentation](https://cloud.google.com/dataflow/docs/reference/pipeline-options#basic_options) and other [related documentation](https://cloud.google.com/dataflow/docs/guides/setting-pipeline-options).

### SZDataflow.serviceAccountEmail
**required** String

Email of the service account that the Dataflow runners will use.

The credentials used to kickoff the indexing must have the `iam.serviceAccounts.actAs` permission on this service account. More details in the [GCP documentation](https://cloud.google.com/iam/docs/service-accounts-actas).

### SZDataflow.usePublicIps
**optional** boolean

Specifies whether the Dataflow runners use external IP addresses.

If set to false, make sure that [Private Google Access](https://cloud.google.com/vpc/docs/configure-private-google-access#configuring_access_to_google_services_from_internal_ips) is enabled for the VPC sub-network that the Dataflow runners will use. More information in the Dataflow pipeline security and networking options [documentation](https://cloud.google.com/dataflow/docs/reference/pipeline-options#security_and_networking). We have seen noticeable improvements in speed of running indexing jobs with this set to `false`.

*Default value:* `true`

### SZDataflow.vpcSubnetworkName
**optional** String

Specifies which VPC sub-network the Dataflow runners use.

This property is the name of the sub-network (e.g. mysubnetwork), not the full URL path to it (e.g. https://www.googleapis.com/compute/v1/projects/my-cloud-project/regions/us-central1/subnetworks/mysubnetwork). If this property is unset, Dataflow will try to use a VPC network called "default".

If you have a custom-mode VPC network, you must set this property. Dataflow can only choose the sub-network automatically for auto-mode VPC networks. More information in the Dataflow network and subnetwork [documentation](https://cloud.google.com/dataflow/docs/guides/specifying-networks#network_parameter).

*Default value:* `true`

### SZDataflow.workerMachineType
**optional** String

Machine type of the Dataflow runners.

The available options are [documented](https://cloud.google.com/compute/docs/machine-resource) for GCP Compute Engine. If this property is unset, Dataflow will choose a machine type. More information in the Dataflow pipeline worker-level options [documentation](https://cloud.google.com/dataflow/docs/reference/pipeline-options#worker-level_options).

We have been using the `n1-standard-4` machine type for all underlays so far. Given that the machine type Dataflow will choose may not remain the same in the future, recommend setting this property.



## SZEntity
Entity configuration.

Define a version of this file for each entity.

### SZEntity.allInstancesSqlFile
**required** String

Name of the all instances SQL file.

File must be in the same directory as the entity file. Name includes file extension.

*Example value:* `all.sql`

### SZEntity.attributes
**required** List [ SZAttribute ]

List of all the entity attributes.

The generated index table will preserve the order of the attributes as defined here. The list must include the id attribute.

### SZEntity.description
**optional** String

Description of the entity.

### SZEntity.displayName
**optional** String

Display name for the entity.

Unlike the entity [name](#szentityname), it may include spaces and special characters.

### SZEntity.hierarchies
**optional** Set [ SZHierarchy ]

List of hierarchies.

While the code supports multiple hierarchies, we currently only have examples with zero or one hierarchy.

### SZEntity.idAttribute
**required** String

Name of the id attribute.

This must be a unique identifier for each entity instance. It must also have the `INT64` [data type](#szdatatype).

### SZEntity.name
**required** String

Name of the entity.

This is the unique identifier for the entity. In a single underlay, the entity names cannot overlap.

Name may not include spaces or special characters, only letters and numbers. The first character must be a letter.

### SZEntity.optimizeGroupByAttributes
**optional** List [ String ]

List of attributes to optimize for group by queries.

The typical use case for this is to optimize cohort breakdown queries on the primary entity. For example, to optimize breakdowns by age, race, gender, specify those attributes here. Order matters.

You can currently specify a maximum of four attributes, because we implement this using BigQuery clustering which has this [limitation](https://cloud.google.com/bigquery/docs/clustered-tables#limitations).

### SZEntity.sourceQueryTableName
**optional** String

Full name of the table to use when exporting a query against the source data.

SQL substitutions are supported in this table name.

If unspecified, exporting a query against the source data is unsupported.

*Example value:* `${omopDataset}.condition_occurrence`

### SZEntity.temporalQuery
**optional** [SZTemporalQuery](#sztemporalquery)

How to generate a temporal query for this entity.

If unspecified, temporal queries that include this output entity are not allowed.

### SZEntity.textSearch
**optional** [SZTextSearch](#sztextsearch)

Text search configuration.

This is used when filtering a list of instances of this entity (e.g. list of conditions) by text. If unset, filtering by text is unsupported.



## SZGroupItems
Group-Items entity group configuration.

Define a version of this file for each entity group of this type. This entity group type defines a one-to-many relationship between two entities. For each group entity instance, there are one or more items entity instances.

### SZGroupItems.foreignKeyAttributeItemsEntity
**optional** String

Attribute of the items entity that is a foreign key to the id attribute of the group entity.

If this property is set, then the [id pairs SQL](#szgroupitemsidpairssqlfile) must be unset.

### SZGroupItems.groupEntity
**required** String

Name of the group entity.

### SZGroupItems.groupEntityIdFieldName
**optional** String

Name of the field or column name that maps to the group entity id.

Required if the [id pairs SQL](#szgroupitemsidpairssqlfile) is defined.

*Example value:* `group_id`

### SZGroupItems.idPairsSqlFile
**optional** String

Name of the group entity - items entity id pairs SQL file.

If this property is set, then the [id pairs SQL](#szgroupitemsidpairssqlfile) must be unset. File must be in the same directory as the entity group file. Name includes file extension.

There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), but the group and items entity ids are required. If this property is set, then the [foreign key atttribute](#szgroupitemsforeignkeyattributeitemsentity) must be unset.

*Example value:* `idPairs.sql`

### SZGroupItems.itemsEntity
**required** String

Name of the items entity.

### SZGroupItems.itemsEntityIdFieldName
**optional** String

Name of the field or column name that maps to the items entity id.

Required if the [id pairs SQL](#szgroupitemsidpairssqlfile) is defined.

*Example value:* `items_id`

### SZGroupItems.name
**required** String

Name of the entity group.

This is the unique identifier for the entity group. In a single underlay, the entity group names of any group type cannot overlap. Name may not include spaces or special characters, only letters and numbers. The first character must be a letter.



## SZHierarchy
Hierarchy for an entity.

### SZHierarchy.childIdFieldName
**required** String

Name of the field or column name in the [child parent id pairs SQL](#szhierarchychildparentidpairssqlfile) that maps to the child id.

*Example value:* `child`

### SZHierarchy.childParentIdPairsSqlFile
**required** String

Name of the child parent id pairs SQL file.

File must be in the same directory as the entity file. Name includes file extension.

There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), but the child and parent ids are required.

*Example value:* `childParent.sql`

### SZHierarchy.cleanHierarchyNodesWithZeroCounts
**optional** boolean

When false, indexing jobs will not clean hierarchy nodes with both a zero item and rollup counts. When true, indexing jobs will clean hierarchy nodes with both a zero item and rollup counts.

*Default value:* `false`

### SZHierarchy.keepOrphanNodes
**optional** boolean

An orphan node has no parents or children. When false, indexing jobs will filter out orphan nodes. When true, indexing jobs skip this filtering step and we keep the orphan nodes in the hierarchy.

*Default value:* `false`

### SZHierarchy.maxDepth
**required** int

Maximum depth of the hierarchy. If there are branches of the hierarchy that are deeper than the number specified here, they will be truncated.

### SZHierarchy.name
**optional** String

Name of the hierarchy.

This is the unique identifier for the hierarchy. In a single entity, the hierarchy names cannot overlap. Name may not include spaces or special characters, only letters and numbers. The first character must be a letter.

If there is only one hierarchy, the name is optional and, if unspecified, will be set to `default`. If there are multiple hierarchies, the name is required for each one.

*Default value:* `default`

### SZHierarchy.parentIdFieldName
**required** String

Name of the field or column name in the [child parent id pairs SQL](#szhierarchychildparentidpairssqlfile) that maps to the parent id.

*Example value:* `parent`

### SZHierarchy.rootIdFieldName
**optional** String

Name of the field or column name that maps to the root id.

If the [root node ids SQL](#szhierarchyrootnodeidssqlfile) is defined, then this property is required. If the [root node ids set](#szhierarchyrootnodeids) is defined, then this property must be unset.

*Example value:* `root_id`

### SZHierarchy.rootNodeIds
**optional** Set [ Long ]

Set of root ids. Indexing jobs will filter out any hierarchy root nodes that are not in this set. If the [root node ids SQL](#szhierarchyrootnodeidssqlfile) is defined, then this property must be unset.

### SZHierarchy.rootNodeIdsSqlFile
**optional** String

Name of the root id SQL file. File must be in the same directory as the entity file. Name includes file extension.

There can be other columns selected in the SQL file (e.g. `SELECT * FROM roots`), but the root id is required. Indexing jobs will filter out any hierarchy root nodes that are not returned by this query. If the [root node ids set](#szhierarchyrootnodeids) is defined, then this property must be unset.

*Example value:* `rootNode.sql`



## SZIndexData
Pointer to the index BigQuery dataset.

### SZIndexData.datasetId
**required** String

Dataset id of the index BigQuery dataset.

### SZIndexData.projectId
**required** String

Project id of the index BigQuery dataset.

### SZIndexData.tablePrefix
**optional** String

Prefix for the generated index tables.

An underscore will be inserted between this prefix and the table name (e.g. prefix `T` will generate a table called "T_ENT_person"). The prefix may not include spaces or special characters, only letters and numbers. The first character must be a letter. This can be useful when the index tables will be written to a dataset that includes other non-Tanagra tables.



## SZIndexer
Indexer configuration.

Define a version of this file for each place you will run indexing. If you later copy the index dataset to other places, you do not need a separate configuration for those.

### SZIndexer.bigQuery
**required** [SZBigQuery](#szbigquery)

Pointers to the source and index BigQuery datasets.

### SZIndexer.dataflow
**required** [SZDataflow](#szdataflow)

Dataflow configuration.

Required for indexing jobs that use batch processing (e.g. computing the ancestor-descendant pairs for a hierarchy).

### SZIndexer.underlay
**required** String

Name of the underlay to index.

Name is specified in the underlay file, and also matches the name of the config/underlay sub-directory in the underlay sub-project resources.

*Example value:* `cmssynpuf`



## SZMetadata
Metadata for the underlay.

Information in this object is not used in the operation of the indexer or service, it is for display purposes only.

### SZMetadata.description
**optional** String

Description of the underlay.

### SZMetadata.displayName
**required** String

Display name for the underlay.

Unlike the underlay [name](#szunderlayname), it may include spaces and special characters.

### SZMetadata.properties
**optional** Map [ String, String ]

Key-value map of underlay properties.

Keys may not include spaces or special characters, only letters and numbers.



## SZOccurrenceEntity
Occurrence entity configuration.

### SZOccurrenceEntity.attributesWithInstanceLevelHints
**required** Set [ String ]

Names of attributes that we want to calculate instance-level hints for.

Instance-level hints are ranges of possible values for a particular criteria instance. They are used to support criteria-specific modifiers (e.g. range of values for measurement code "glucose test").

### SZOccurrenceEntity.criteriaRelationship
**required** [SZCriteriaRelationship](#szcriteriarelationship)

Relationship or join between this occurrence entity and the criteria entity (e.g. condition occurrence and ICD9-CM).

### SZOccurrenceEntity.occurrenceEntity
**required** String

Name of occurrence entity.

### SZOccurrenceEntity.primaryRelationship
**required** [SZPrimaryRelationship](#szprimaryrelationship)

Relationship or join between this occurrence entity and the primary entity (e.g. condition occurrence and person).



## SZPrepackagedCriteria
Prepackaged criteria configuration.

### SZPrepackagedCriteria.criteriaSelector
**required** String

Name of the criteria selector this criteria is associated with.

The criteria selector must be defined for the underlay. (e.g. The condition selector must be defined in order to define a prepackaged data feature for condition = Type 2 Diabetes.)

### SZPrepackagedCriteria.displayName
**required** String

Display name.

### SZPrepackagedCriteria.name
**required** String

Name of the prepackaged criteria.

This is the unique identifier for the criteria. The criteria names cannot overlap within an underlay.

Name may not include spaces or special characters, only letters and numbers.

This name is stored in the application database for data feature sets, so once there are artifacts associated with a prepackaged criteria, you can't change the criteria name.

### SZPrepackagedCriteria.pluginData
**required** String

Serialized data for the UI display plugin e.g. "{"conceptId":"201826"}".

### SZPrepackagedCriteria.pluginDataFile
**required** String

Name of the file that contains the serialized data for the UI display plugin.

This file should be in the same directory as the prepackaged criteria (e.g. `condition.json`).

If this property is specified, the value of the `pluginData` property is ignored.



## SZPrimaryCriteriaRelationship
Relationship or join between the primary and criteria entities (e.g. condition and person).

### SZPrimaryCriteriaRelationship.criteriaEntityIdFieldName
**required** String

Name of the field or column name that maps to the criteria entity id.

*Example value:* `criteria_id`

### SZPrimaryCriteriaRelationship.idPairsSqlFile
**required** String

Name of the primary entity - criteria entity id pairs SQL file. File must be in the same directory as the entity group file. Name includes file extension. There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), but the primary and criteria entity ids are required.

*Example value:* `primaryCriteria.sql`

### SZPrimaryCriteriaRelationship.primaryEntityIdFieldName
**required** String

Name of the field or column name that maps to the primary entity id.

*Example value:* `primary_id`



## SZPrimaryRelationship
Relationship or join between an occurrence entity and the primary entity (e.g. condition occurrence and person).

### SZPrimaryRelationship.foreignKeyAttributeOccurrenceEntity
**optional** String

Attribute of the occurrence entity that is a foreign key to the id attribute of the primary entity. If this property is set, then the [id pairs SQL](#szprimaryrelationshipidpairssqlfile) must be unset.

### SZPrimaryRelationship.idPairsSqlFile
**optional** String

Name of the occurrence entity - primary entity id pairs SQL file. File must be in the same directory as the entity group file. Name includes file extension. If this property is set, then the [foreign key attribute](#szprimaryrelationshipforeignkeyattributeoccurrenceentity) must be unset.

There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), but the occurrence and primary entity ids are required.

*Example value:* `occurrencePrimary.sql`

### SZPrimaryRelationship.occurrenceEntityIdFieldName
**optional** String

Name of the field or column name that maps to the occurrence entity id. Required if the [id pairs SQL](#szprimaryrelationshipidpairssqlfile) is defined.

*Example value:* `occurrence_id`

### SZPrimaryRelationship.primaryEntityIdFieldName
**optional** String

Name of the field or column name that maps to the primary entity id. Required if the [id pairs SQL](#szprimaryrelationshipidpairssqlfile) is defined.

*Example value:* `primary_id`



## SZService
Service configuration.

Define a version of this file for each place you will deploy the service. If you share the same index dataset across multiple service deployments, you need a separate configuration for each.

### SZService.bigQuery
**required** [SZBigQuery](#szbigquery)

Pointers to the source and index BigQuery datasets.

### SZService.underlay
**required** String

Name of the underlay to make available in the service deployment.

If a single deployment serves multiple underlays, you need a separate configuration for each. Name is specified in the underlay file, and also matches the name of the config/underlay sub-directory in the underlay sub-project resources.

*Example value:* `cmssynpuf`



## SZSourceData
Pointer to the source BigQuery dataset.

### SZSourceData.datasetId
**required** String

Dataset id of the source BigQuery dataset.

### SZSourceData.projectId
**required** String

Project id of the source BigQuery dataset.

### SZSourceData.sqlSubstitutions
**optional** Map [ String, String ]

Key-value map of substitutions to make in the input SQL files.

Wherever the keys appear in the input SQL files wrapped in braces and preceded by a dollar sign, they will be substituted by the values before running the queries. For example, [key] `omopDataset` -> [value] `bigquery-public-data.cms_synthetic_patient_data_omop` means `${omopDataset}` in any of the input SQL files will be replaced by `bigquery-public-data.cms_synthetic_patient_data_omop`.

Keys may not include spaces or special characters, only letters and numbers. This is simple string substitution logic and does not handle more complicated cases, such as nested substitutions.



## SZSourceQuery
Information to generate a SQL query against the source dataset for a given attribute.

This query isn't actually run by the service, only generated as an export option (e.g. as part of a notebook file).

### SZSourceQuery.displayFieldName
**optional** String

Name of the field to use for the attribute display in the source dataset.

If unspecified, exporting a query with this attribute against the source data will not include a separate display field.

The table can optionally be specified in #szsourcequerydisplayfieldtable.

*Example value:* `concept_name`

### SZSourceQuery.displayFieldTable
**optional** String

Full name of the table to JOIN with the main table (#szentitysourcequerytablename) to get the attribute display field in the source dataset.

SQL substitutions are supported in this table name.

If unspecified, and #szsourcequerydisplayfieldname is specified, then we assume that the source display field is also in the main table, same as the source value field.

The #szsourcequerydisplayfieldtablejoinfieldname is required if this property is specified.

*Example value:* `${omopDataset}.concept`

### SZSourceQuery.displayFieldTableJoinFieldName
**optional** String

Name of the field in the display table (#szsourcequerydisplayfieldtable) that is used to JOIN to the main table (#szentitysourcequerytablename) using the source value field (#szsourcequeryvaluefieldname).

This is required if the #szsourcequerydisplayfieldtable is specified.

*Example value:* `concept_id`

### SZSourceQuery.valueFieldName
**optional** String

Name of the field to use for the attribute value in the source dataset table (#szentitysourcequerytablename).

If unspecified, we assume the field name in the source table (#szentitysourcequerytablename) corresponding to this attribute is the same as the #szattributevaluefieldname.

*Example value:* `condition_concept_id`



## SZTemporalQuery
Information to build a temporal query with this entity.

### SZTemporalQuery.visitDateAttribute
**required** String

Name of the attribute to use for the visit date in a temporal query.

*Example value:* `start_date`

### SZTemporalQuery.visitIdAttribute
**required** String

Name of the attribute to use for the visit (occurrence) id in a temporal query.

*Example value:* `visit_occurrence_id`



## SZTextSearch
Text search configuration for an entity.

### SZTextSearch.attributes
**optional** Set [ String ]

Set of attributes to allow text search on. Text search on attributes not included here is unsupported.

### SZTextSearch.idFieldName
**optional** String

Name of the field or column name that maps to the entity id. If the [id text pairs SQL](#sztextsearchidtextpairssqlfile) is defined, then this property is required.

*Example value:* `id`

### SZTextSearch.idTextPairsSqlFile
**optional** String

Name of the id text pairs SQL file. File must be in the same directory as the entity file. Name includes file extension.

There can be other columns selected in the SQL file (e.g. `SELECT * FROM synonyms`), but the entity id and text string is required. The SQL query may return multiple rows per entity id.

*Example value:* `textSearch.sql`

### SZTextSearch.textFieldName
**optional** String

Name of the field or column name that maps to the text search string. If the [id text pairs SQL](#sztextsearchidtextpairssqlfile) is defined, then this property is required.

*Example value:* `text`



## SZUnderlay
Underlay configuration.

Define a version of this file for each dataset. If you index and/or serve a dataset in multiple places or deployments, you only need one version of this file.

### SZUnderlay.criteriaOccurrenceEntityGroups
**required** Set [ String ]

List of paths of `criteria-occurrence` type entity groups.

A `criteria-occurrence` type entity group defines a relationship between three entities.

Path consists of two parts: [Data-Mapping Group]/[Entity Group Name] (e.g. `omop/conditionPerson`).

[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory in the underlay sub-project resources (e.g. `omop`).

[Entity Group Name] is specified in the entity group file, and also matches the name of the sub-directory of the config/datamapping/[Data-Mapping Group]/entitygroup sub-directory in the underlay sub-project resources (e.g. `conditionPerson`).

Using the path here instead of just the entity group name allows us to share entity group definitions across underlays. For example, the `omop` data-mapping group contains template entity group definitions for standing up a new underlay.

### SZUnderlay.criteriaSelectors
**required** List [ String ]

List of paths of all the criteria selectors.

A criteria selector is an option for defining a filter on an entity (e.g. select a condition). It corresponds to one or more UI display plugins. (e.g. condition selector uses the entity group plugin for selecting the condition, the attribute plugin for selecting the visit type modifier, and the unhinted-value plugin for selecting the occurrence count modifier).

Path consists of two parts: [Display Group]/[Criteria Selector Name] (e.g. `omop/gender`).

[Display Group] is the name of a sub-directory of the config/display/ sub-directory in the underlay sub-project resources (e.g. `omop`).

[Criteria Selector Name] is specified in the selector file, and also matches the name of the sub-directory of the config/display/[Display Group]/criteriaselector sub-directory in the underlay sub-project resources (e.g. `gender`).

Using the path here instead of just the selector name allows us to share selector definitions across underlays. For example, the `omop` display group contains template selector definitions for standing up a new underlay.

### SZUnderlay.entities
**required** Set [ String ]

List of paths of all the entities.

An entity is any object that the UI might show a list of (e.g. list of persons, conditions, condition occurrences). The list must include the primary entity.

Path consists of two parts: [Data-Mapping Group]/[Entity Name] (e.g. `omop/condition`).

[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory in the underlay sub-project resources (e.g. `omop`).

[Entity Name] is specified in the entity file, and also matches the name of the sub-directory of the config/datamapping/[Data-Mapping Group]/entity sub-directory in the underlay sub-project resources (e.g. `condition`).

Using the path here instead of just the entity name allows us to share entity definitions across underlays. For example, the `omop` data-mapping group contains template entity definitions for standing up a new underlay.

### SZUnderlay.groupItemsEntityGroups
**required** Set [ String ]

List of paths of `group-items` type entity groups.

A `group-items` type entity group defines a relationship between two entities.

Path consists of two parts: [Data-Mapping Group]/[Entity Group Name] (e.g. `omop/brandIngredient`).

[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory in the underlay sub-project resources (e.g. `omop`).

[Entity Group Name] is specified in the entity group file, and also matches the name of the sub-directory of the config/datamapping/[Data-Mapping Group]/entitygroup sub-directory in the underlay sub-project resources (e.g. `brandIngredient`).

Using the path here instead of just the entity group name allows us to share entity group definitions across underlays. For example, the `omop` data-mapping group contains template entity group definitions for standing up a new underlay.

### SZUnderlay.metadata
**required** [SZMetadata](#szmetadata)

Metadata for the underlay.

### SZUnderlay.name
**required** String

Name of the underlay.

This is the unique identifier for the underlay. If you serve multiple underlays in a single service deployment, the underlay names cannot overlap. Name may not include spaces or special characters, only letters and numbers.

This name is stored in the application database for cohorts and data feature sets, so once there are artifacts associated with an underlay, you can't change the underlay name.

### SZUnderlay.prepackagedDataFeatures
**required** Set [ String ]

List of paths of all the prepackaged data features.

A prepackaged data feature is a predefined data feature for exporting data (e.g. demographics). It contains data for zero or more UI display plugins. (e.g. type 2 diabetes data feature defines data for the entity group plugin).

Path consists of two parts: [Display Group]/[Prepackaged Data Feature Name] (e.g. `omop/demographics`).

[Display Group] is the name of a sub-directory of the config/display/ sub-directory in the underlay sub-project resources (e.g. `omop`).

[Prepackaged Data Feature Name] is specified in the prepackaged file, and also matches the name of the sub-directory of the config/display/[Display Group]/prepackagedcriteria sub-directory in the underlay sub-project resources (e.g. `demographics`).

Using the path here instead of just the prepackaged criteria name allows us to share criteria definitions across underlays. For example, the `omop` display group contains template criteria definitions for standing up a new underlay.

### SZUnderlay.primaryEntity
**required** String

Name of the primary entity.

A cohort contains instances of the primary entity (e.g. persons).

### SZUnderlay.uiConfigFile
**required** String

Name of the UI config file.

File must be in the same directory as the underlay file. Name includes file extension.

*Example value:* `ui.json`

### SZUnderlay.visualizations
**required** List [ String ]

List of paths of all the visualizations.

A visualization contains all of the configuration to display a underlay or cohort level visualization in the UI.

Path consists of two parts: [Display Group]/[Visualization Name] (e.g. `omop/peopleByAge`).

[Display Group] is the name of a sub-directory of the config/ui/ sub-directory in the underlay sub-project resources (e.g. `omop`).

[Visualization Name] is specified in the visualization file, and also matches the name of the sub-directory of the config/ui/[Display Group]/viz sub-directory in the underlay sub-project resources (e.g. `peopleByAge`).

Using the path here instead of just the visualization name allows us to share visualization definitions across underlays. For example, the `omop` visualization group contains template visualization definitions for standing up a new underlay.



## SZVisualization
Configuration for a single visualization.

### SZVisualization.dataConfig
**required** String

Serialized configuration of the visualization. VizConfig protocol buffer as JSON.

### SZVisualization.dataConfigFile
**required** String

Name of the file that contains the serialized configuration of the visualization.

This file should be in the same directory as the visualization (e.g. `gender.json`).

If this property is specified, the value of the `config` property is ignored.

### SZVisualization.name
**required** String

Name of the visualization.

This is the unique identifier for the vizualization. The vizualization names cannot overlap within an underlay.

Name may not include spaces or special characters, only letters and numbers.

### SZVisualization.plugin
**required** String

Name of the visualization UI plugin.

### SZVisualization.pluginConfig
**required** String

Serialized configuration of the visualization UI plugin as JSON.

### SZVisualization.pluginConfigFile
**required** String

Name of the file that contains the serialized configuration of the visualization UI plugin.

This file should be in the same directory as the visualization (e.g. `gender.json`).

If this property is specified, the value of the `pluginConfig` property is ignored.

### SZVisualization.title
**required** String

Visible title of the visualization.



