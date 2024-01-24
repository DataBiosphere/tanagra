# Data Patterns

## SQL schema agnostic
Tanagra supports data patterns, instead of specific SQL schemas (e.g. OMOP).
A data pattern is a set of `JOIN`s between one or more related SQL tables.
Each `JOIN` can use a foreign key on one table or the other, or an intermediate table that contains pairs of ids.
An underlay typically uses several data patterns, and may use each pattern one or more times.

Supporting data patterns instead of specific SQL schemas helps us maximize code reuse across different datasets and
makes us tolerant to some common variations across otherwise similar datasets (e.g. two OMOP datasets with
different `JOIN` types for a particular relationship).

## Considerations when configuring a new dataset
The data patterns that we support today all appear in OMOP datasets, but they will also appear in datasets with other
schemas. For example, the 1000 Genomes dataset doesn't include EMR-type data the way that an EMR-type dataset does. It
has multiple samples per `Family_ID`, which fits the "set of records" data pattern that we use in OMOP to model
multiple blood pressure readings per `person_id`.

When configuring a new dataset, first look to see if we already support the relevant data patterns. If so, then you
only need to write config files to index and deploy the dataset, no code changes required. We are much more likely
to already support the relevant patterns when the new dataset contains similar types of data to datasets we've already 
seen (e.g. another dataset with EMR-type data, even if it doesn't use the OMOP schema).

## Supported patterns

Below is a list of the currently supported patterns with examples.

The examples refer to a list of something (e.g. ICD9-CM codes) living in one "table". The config files 
actually require a SQL query instead of a table name. The indexer turns this query into an actual SQL table.
So if your list of something does not exactly correspond to a single SQL table, as long as you can list it using a SQL
query, that will work. e.g. `SELECT * FROM concept WHERE domain_id='ICD9-cm'` is a table for Tanagra's purposes that 
is a subset of the `concept` SQL table.

### Set of Records
**Example: Person has a set of blood pressure readings**
- List of people lives in one table (e.g. `person`).
- List of blood pressure readings lives in another table (e.g. `blood_pressure`).
- The `JOIN` between the two tables involves either a foreign key field in the blood pressure reading table (e.g. 
`blood_pressure.person_id`) or an intermediate table with pairs of person and blood pressure reading ids (e.g. 
`intermediate_table.person_id`, `intermediate_table.blood_pressure_id`).

#### Config files
- Entity definition for the person entity (e.g. [sd/person](../underlay/src/main/resources/config/datamapping/sd/entity/person)).
- Entity definition for the blood pressure entity (e.g. [sd/bloodPressure](../underlay/src/main/resources/config/datamapping/sd/entity/bloodPressure)).
- Group-items entity group definition for the relationship between the two entities (e.g. [sd/bloodPressurePerson](../underlay/src/main/resources/config/datamapping/sd/entitygroup/bloodPressurePerson)).

#### Queries
- List the people with any blood pressure readings with a `GroupHasItems` filter. Useful for defining a cohort.
- List the blood pressure readings for one or more people with a `ItemInGroup` filter. Useful for exporting data.


### Set of Keyed Records
**Example: Person has a set of diagnosis events keyed to the SNOMED vocabulary diagnosis codes**
- List of people lives in one table (e.g. `person`).
- List of diagnosis events, each keyed to a single SNOMED code, lives in another table (e.g. `condition_occurrence`).
- List of SNOMED diagnosis codes lives in another table (e.g. `concept`).
- The `JOIN` between the person and diagnosis event tables involves either a foreign key field in the diagnosis event
table (e.g. `condition_occurrence.person_id`) or an intermediate table with pairs of person and diagnosis event ids 
(e.g. `intermediate_table.person_id`, `intermediate_table.condition_occurrence_id`).
- The `JOIN` between the SNOMED code and diagnosis event tables involves either a foreign key field in the diagnosis 
event table (e.g. `condition_occurrence.condition_concept_id`) or an intermediate table with pairs of SNOMED code and 
diagnosis event ids (e.g. `intermediate_table.condition_concept_id`, `intermediate_table.condition_occurrence_id`).

#### Config files
- Entity definition for the person entity (e.g. [omop/person](../underlay/src/main/resources/config/datamapping/omop/entity/person)).
- Entity definition for the condition occurrence entity (e.g. [omop/conditionOccurrence](../underlay/src/main/resources/config/datamapping/omop/entity/conditionOccurrence)).
- Entity definition for the SNOMED condition entity (e.g. [omop/condition](../underlay/src/main/resources/config/datamapping/omop/entity/condition)).
- Criteria-occurrence entity group definition for the relationship between the three entities (e.g. [omop/conditionPerson](../underlay/src/main/resources/config/datamapping/omop/entitygroup/conditionPerson)).

#### Queries
- List the people with any diagnosis events for one or more SNOMED codes with a `PrimaryWithCriteria` filter. Useful for
defining a cohort.
- List the diagnosis events for one or more people with an `OccurrenceForPrimary` filter. Useful for exporting data.
- Fetch the rollup count of the number of people that have at least one diagnosis event for a particular SNOMED code 
with the `RelatedEntityIdCount` field. Useful for ranking or ordering SNOMED codes in a display.


### Hierarchy of Codes
**Example: Vocabulary of diagnosis codes are organized into a hierarchy**
- List of ICD9-CM diagnosis codes lives in one table (e.g. `concept`).
- List of parent-child relationships between the ICD9-CM codes live in another table (e.g. `concept_relationship`).

#### Config files
- Entity definition for the ICD9-CM entity (e.g. [omop/icd9cm](../underlay/src/main/resources/config/datamapping/omop/entity/icd9cm))
that includes a hierarchy definition.

#### Queries
- List the ICD9-CM hierarchy root nodes with a `HierarchyIsRoot` filter. Useful for showing an unexpanded hierarchy view.
- List the immediate children of an ICD9-CM code with a `HierarchyHasParent` filter. Useful for expanding a parent code
in a hierarchy view.
- List all descendants of an ICD9-CM code with a `HierarchyHasAncestor` filter. In conjunction with a 
`PrimaryForCriteria` filter, useful for defining a cohort of people with diagnoses of a particular ICD9-CM code or any
of its descendants.
- Fetch a flag that indicates whether a code is a member of the hierarchy with the `HierarchyIsMember` field. Useful
for determining whether to show an icon to open a hierarchy view for a particular code.
- Fetch the number of immediate children a code has with the `HierarchyNumChildren` field. Useful for determining
whether to show an expand icon in the hierarchy view for a particular code.
- Fetch a path from a code to any root node with the `HierarchyPath` field. If there are multiple possible paths from
a code to a root node, the path returned may be any one of them. Useful for expanding a hierarchy view down to a 
particular node.


### Group of Items
**Example: Drug brand contains a group of ingredients**
- List of brands lives in one table (e.g. `brand`).
- List of ingredients lives in another table (e.g. `ingredient`).
- The `JOIN` between the brand and ingredient tables involves either a foreign key field in the ingredient table
(e.g. `ingredient.brand_concept_id`) or an intermediate table with pairs of brand and ingredient ids (e.g. 
`intermediate_table.brand_id`, `intermediate_table.ingredient_id`).

#### Config files
- Entity definition for the brand entity (e.g. [omop/brand](../underlay/src/main/resources/config/datamapping/omop/entity/brand)).
- Entity definition for the ingredient entity (e.g. [omop/ingredient](../underlay/src/main/resources/config/datamapping/omop/entity/ingredient)).
- Group-items entity group definition for the relationship between the two entities (e.g. [omop/brandIngredient](../underlay/src/main/resources/config/datamapping/omop/entitygroup/brandIngredient)).

#### Queries
- List the ingredients for a particular brand with a `ItemInGroup` filter. Useful for expanding a brand to show a list
of its ingredients.
- Fetch the number of ingredients for a brand with the `RelatedEntityIdCount` field. Useful for determining whether
to show an expand icon for the brand.
