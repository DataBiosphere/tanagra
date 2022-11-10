# Entity Model

Tanagra defines a custom object model on top of the underlying relational data. Tanagra's objects are called entities.

An underlay is a collection of entities and the relationships between them that map to an underlying relational dataset.
In the Tanagra UI, you work with one underlay at a time (e.g. building cohorts, concept sets, datasets).

## Entity
An entity is an object that is displayed in the UI somehow and makes sense to an end user. i.e. If you define an 
entity X, end users should be able to understand what a list of X is without knowing anything about the underlying 
database schema.

e.g. For OMOP, the entities include: `person`, `condition`, `procedure`, `condition_occurrence`, etc.

An entity mapping tells Tanagra how to get a list of the entity instances. Frequently, an entity mapping will just 
point to a single table. It can also point to a subset of a table or a raw SQL query or view.

e.g. For OMOP, the `person` and `condition_occurrence` entities each map to the table of the same name.
The `condition` entity maps to a subset of the rows of the `concept` table (`WHERE domain_id='Condition'`).
The `procedure` entity maps to a different subset of the rows of the `concept` table (`WHERE domain_id='Procedure'`).

### Primary Entity
There is a single primary entity for each underlay. This defines what makes up a cohort.

e.g. For OMOP, the primary entity is `person` and a cohort is a group of `person` instances.

### Attributes
Each entity has a list of attributes or properties. Attributes are used in the UI somehow: display, ordering, etc.

There are 2 types of attributes. A `SIMPLE` attribute has a single value and a `KEY_AND_DISPLAY` attribute has both a 
value and a display name.

### Hierarchies
Each entity can have one or more hierarchies. This is used by the UI to display instances in a tree view instead of
a list view. Currently the hierarchy depth is capped at 64 levels, but that's just an arbitrary limit and we could
raise it or allow users to override it in the future.

### Text search


## Entity Group
An entity group indicates a specific relationship between one or more entities. These relationships are pre-defined and
tell Tanagra how to convert entity queries into SQL and how to generate useful index tables.

There are currently 2 types of entity groups defined in Tanagra.

### `GROUP_ITEMS`
A `GROUP_ITEMS` entity group defines a relationship between 2 entities. There is a one-to-many relationship between
the `GROUP` entity and the `ITEMS` entity.

The UI will ask for instances of the `ITEMS` entity for a given `GROUP` instance.

e.g. For OMOP, the `GROUP_ITEMS` entity group defines the relationship between the `brand` and `ingredient`
entities. Each `brand` instance contains one or more `ingredient` instances. The UI will ask for instances of
`ingredient` for a given `brand` (e.g. ingredients in Tylenol PM).

### `CRITERIA_OCCURRENCE`
A `CRITERIA_OCCURRENCE` entity group defines a relationship between 3 entities. Each `OCCURRENCE` instance is 
associated with exactly one `CRITERIA` instance and one `PRIMARY` instance. The `PRIMARY` entity refers to the primary
entity of the underlay. There may be multiple `OCCURRENCE` instances for each `CRITERIA` and `PRIMARY` instance.

The UI will ask for:
* Instances of the `OCCURRENCE` entity for a given `PRIMARY` and/or `CRITERIA` instance.
* The number of `PRIMARY` instances that have at least one `OCCURRENCE` instance for a given `CRITERIA` instance.

e.g. For OMOP, the `CRITERIA_OCCURRENCE` entity group defines the relationship between the `person`, `condition`, and
`condition_occurrence` entities. The UI will ask for `condition_occurrence` instances for a given `person` instance
and/or `condition` instance. The UI will also ask for the number of `person` instances that have at least one
`condition_occurrence` instance for a given `condition` instance (e.g. number of people with at least one diagnosis
of diabetes).
