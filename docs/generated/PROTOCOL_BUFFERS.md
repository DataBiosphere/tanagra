# Protocol Documentation
<a name="top"></a>

## Table of Contents

- [criteriaselector/configschema/attribute.proto](#criteriaselector_configschema_attribute-proto)
    - [Attribute](#tanagra-configschema-Attribute)
  
- [criteriaselector/configschema/biovu.proto](#criteriaselector_configschema_biovu-proto)
    - [BioVU](#tanagra-configschema-BioVU)
  
- [criteriaselector/configschema/entity_group.proto](#criteriaselector_configschema_entity_group-proto)
    - [EntityGroup](#tanagra-configschema-EntityGroup)
    - [EntityGroup.Column](#tanagra-configschema-EntityGroup-Column)
    - [EntityGroup.EntityGroupConfig](#tanagra-configschema-EntityGroup-EntityGroupConfig)
  
- [criteriaselector/configschema/multi_attribute.proto](#criteriaselector_configschema_multi_attribute-proto)
    - [MultiAttribute](#tanagra-configschema-MultiAttribute)
  
- [criteriaselector/configschema/output_unfiltered.proto](#criteriaselector_configschema_output_unfiltered-proto)
    - [OutputUnfiltered](#tanagra-configschema-OutputUnfiltered)
  
- [criteriaselector/configschema/text_search.proto](#criteriaselector_configschema_text_search-proto)
    - [TextSearch](#tanagra-configschema-TextSearch)
  
- [criteriaselector/configschema/unhinted_value.proto](#criteriaselector_configschema_unhinted_value-proto)
    - [UnhintedValue](#tanagra-configschema-UnhintedValue)
    - [UnhintedValue.AttributeList](#tanagra-configschema-UnhintedValue-AttributeList)
    - [UnhintedValue.AttributesEntry](#tanagra-configschema-UnhintedValue-AttributesEntry)
  
- [criteriaselector/data_range.proto](#criteriaselector_data_range-proto)
    - [DataRange](#tanagra-DataRange)
  
- [criteriaselector/dataschema/attribute.proto](#criteriaselector_dataschema_attribute-proto)
    - [Attribute](#tanagra-dataschema-Attribute)
    - [Attribute.Selection](#tanagra-dataschema-Attribute-Selection)
  
- [criteriaselector/dataschema/biovu.proto](#criteriaselector_dataschema_biovu-proto)
    - [BioVU](#tanagra-dataschema-BioVU)
  
    - [BioVU.SampleFilter](#tanagra-dataschema-BioVU-SampleFilter)
  
- [criteriaselector/dataschema/entity_group.proto](#criteriaselector_dataschema_entity_group-proto)
    - [EntityGroup](#tanagra-dataschema-EntityGroup)
    - [EntityGroup.Selection](#tanagra-dataschema-EntityGroup-Selection)
  
- [criteriaselector/dataschema/multi_attribute.proto](#criteriaselector_dataschema_multi_attribute-proto)
    - [MultiAttribute](#tanagra-dataschema-MultiAttribute)
  
- [criteriaselector/dataschema/output_unfiltered.proto](#criteriaselector_dataschema_output_unfiltered-proto)
    - [OutputUnfiltered](#tanagra-dataschema-OutputUnfiltered)
  
- [criteriaselector/dataschema/text_search.proto](#criteriaselector_dataschema_text_search-proto)
    - [TextSearch](#tanagra-dataschema-TextSearch)
    - [TextSearch.Selection](#tanagra-dataschema-TextSearch-Selection)
  
- [criteriaselector/dataschema/unhinted_value.proto](#criteriaselector_dataschema_unhinted_value-proto)
    - [UnhintedValue](#tanagra-dataschema-UnhintedValue)
  
    - [UnhintedValue.ComparisonOperator](#tanagra-dataschema-UnhintedValue-ComparisonOperator)
  
- [criteriaselector/key.proto](#criteriaselector_key-proto)
    - [Key](#tanagra-Key)
  
- [criteriaselector/value_config.proto](#criteriaselector_value_config-proto)
    - [ValueConfig](#tanagra-ValueConfig)
  
- [criteriaselector/value_data.proto](#criteriaselector_value_data-proto)
    - [ValueData](#tanagra-ValueData)
    - [ValueData.Selection](#tanagra-ValueData-Selection)
  
- [sort_order.proto](#sort_order-proto)
    - [SortOrder](#tanagra-SortOrder)
  
    - [SortOrder.Direction](#tanagra-SortOrder-Direction)
  
- [value.proto](#value-proto)
    - [Value](#tanagra-Value)
  
- [viz/viz_config.proto](#viz_viz_config-proto)
    - [VizConfig](#tanagra-viz-VizConfig)
    - [VizConfig.Source](#tanagra-viz-VizConfig-Source)
    - [VizConfig.Source.Attribute](#tanagra-viz-VizConfig-Source-Attribute)
    - [VizConfig.Source.Attribute.NumericBucketing](#tanagra-viz-VizConfig-Source-Attribute-NumericBucketing)
    - [VizConfig.Source.Attribute.NumericBucketing.Intervals](#tanagra-viz-VizConfig-Source-Attribute-NumericBucketing-Intervals)
    - [VizConfig.Source.Join](#tanagra-viz-VizConfig-Source-Join)
    - [VizConfig.Source.Join.Aggregation](#tanagra-viz-VizConfig-Source-Join-Aggregation)
  
    - [VizConfig.Source.Join.Aggregation.AggregationType](#tanagra-viz-VizConfig-Source-Join-Aggregation-AggregationType)
  
- [Scalar Value Types](#scalar-value-types)



<a name="criteriaselector_configschema_attribute-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/configschema/attribute.proto



<a name="tanagra-configschema-Attribute"></a>

### Attribute
A criteria based on a categorical (i.e. an enum) or numeric attribute of the
primary entity.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| attribute | [string](#string) |  | The attribute of interest. |
| multiRange | [bool](#bool) |  | Whether multiple ranges can be simultaneously specified for the attribute within a single criteria. |
| unit | [string](#string) | optional | An optional unit to show in the criteria UI. |





 

 

 

 



<a name="criteriaselector_configschema_biovu-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/configschema/biovu.proto



<a name="tanagra-configschema-BioVU"></a>

### BioVU



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| plasmaFilter | [bool](#bool) |  |  |





 

 

 

 



<a name="criteriaselector_configschema_entity_group-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/configschema/entity_group.proto



<a name="tanagra-configschema-EntityGroup"></a>

### EntityGroup
A criteria based on one or more entity groups. This allows the selection of
primary entities which are related to one or more of another entity which
match certain characteristics (e.g. people related to condition_occurrences
which have condition_name of &#34;Diabetes&#34;).


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| columns | [EntityGroup.Column](#tanagra-configschema-EntityGroup-Column) | repeated | Columns displayed in the list view. |
| hierarchy_columns | [EntityGroup.Column](#tanagra-configschema-EntityGroup-Column) | repeated | Columns displayed in the hierarchy view. |
| name_column_index | [int32](#int32) |  | This has been replaced by nameAttribute for determining stored names. Now this only determines which is the primary column for checkboxes, etc. |
| classification_entity_groups | [EntityGroup.EntityGroupConfig](#tanagra-configschema-EntityGroup-EntityGroupConfig) | repeated | Entity groups where the related entity is what is selected (e.g. condition when filtering condition_occurrences). |
| grouping_entity_groups | [EntityGroup.EntityGroupConfig](#tanagra-configschema-EntityGroup-EntityGroupConfig) | repeated | Entity groups where the related entity is not what is selected (e.g. brands when filtering ingredients or genotyping platforms when filtering people). |
| multi_select | [bool](#bool) |  | Whether a single click selects a value or multiple values can be selected and then confirmed. |
| value_configs | [tanagra.ValueConfig](#tanagra-ValueConfig) | repeated | Optional configuration of a categorical or numeric value associated with the selection (e.g. a measurement value). Applied to the entire selection so generally not compatible with multi_select. Currently only one is supported. |
| default_sort | [tanagra.SortOrder](#tanagra-SortOrder) |  | The sort order to use in the list view, or in hierarchies where no sort order has been specified. |
| limit | [int32](#int32) | optional | Number of values to display in the list view for each entity group. Otherwise, a default value is applied. |
| nameAttribute | [string](#string) | optional | The attribute used to name selections if not the first column. This can be used to include extra context with the selected values that&#39;s not visible in the table view. |






<a name="tanagra-configschema-EntityGroup-Column"></a>

### EntityGroup.Column
Defines a column in the UI.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | A unique key for the column. By default, used to look up attributes in the displayed data. |
| width_string | [string](#string) |  | Passed directly to the style of the column. &#34;100%&#34; can be used to take up space remaining after laying out fixed columns. |
| width_double | [double](#double) |  | Units used by the UI library to standardize dimensions. |
| title | [string](#string) |  | The visible title of the column. |
| sortable | [bool](#bool) |  | Whether the column supports sorting. |
| filterable | [bool](#bool) |  | Whether the column supports filtering. |






<a name="tanagra-configschema-EntityGroup-EntityGroupConfig"></a>

### EntityGroup.EntityGroupConfig
Multiple entity groups can be shown within the same criteria. Typically
they would filter over the same entity, or at least very similar entities,
since much of the configuration (e.g. columns) is shared. Both types of
configs can be combined though they are displayed separately.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| id | [string](#string) |  | The id of the entity group. |
| sort_order | [tanagra.SortOrder](#tanagra-SortOrder) |  | The sort order applied to this entity group when displayed in the hierarchy view. |





 

 

 

 



<a name="criteriaselector_configschema_multi_attribute-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/configschema/multi_attribute.proto



<a name="tanagra-configschema-MultiAttribute"></a>

### MultiAttribute
A criteria based on one or more categorical (i.e. an enum) or numeric
attribute of an entity. Can be configured to show all attributes or switch
between them.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| entity | [string](#string) |  | The entity to filter on. |
| single_value | [bool](#bool) |  | Whether the user selects a single attribute to filter on or filters on all of them simultaneously. |
| value_configs | [tanagra.ValueConfig](#tanagra-ValueConfig) | repeated | Configuration for each filterable attribute. |





 

 

 

 



<a name="criteriaselector_configschema_output_unfiltered-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/configschema/output_unfiltered.proto



<a name="tanagra-configschema-OutputUnfiltered"></a>

### OutputUnfiltered
A UI-less criteria intended primarily for prepackaged data features that
include entire entities (e.g. demographics).


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| entities | [string](#string) | repeated | The entities to include. |





 

 

 

 



<a name="criteriaselector_configschema_text_search-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/configschema/text_search.proto



<a name="tanagra-configschema-TextSearch"></a>

### TextSearch
A criteria that allows searching for text across categorized items.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| entity | [string](#string) |  | The entity to search. |
| searchAttribute | [string](#string) | optional | The attribute to search within. Defaults to the search configuration in the entity definition. |
| categoryAttribute | [string](#string) | optional | An optional categorical attribute to additionally filter on. |





 

 

 

 



<a name="criteriaselector_configschema_unhinted_value-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/configschema/unhinted_value.proto



<a name="tanagra-configschema-UnhintedValue"></a>

### UnhintedValue



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| group_by_count | [bool](#bool) |  |  |
| attributes | [UnhintedValue.AttributesEntry](#tanagra-configschema-UnhintedValue-AttributesEntry) | repeated |  |






<a name="tanagra-configschema-UnhintedValue-AttributeList"></a>

### UnhintedValue.AttributeList



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| values | [string](#string) | repeated |  |






<a name="tanagra-configschema-UnhintedValue-AttributesEntry"></a>

### UnhintedValue.AttributesEntry



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  |  |
| value | [UnhintedValue.AttributeList](#tanagra-configschema-UnhintedValue-AttributeList) |  |  |





 

 

 

 



<a name="criteriaselector_data_range-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/data_range.proto



<a name="tanagra-DataRange"></a>

### DataRange



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| id | [string](#string) |  |  |
| min | [double](#double) |  |  |
| max | [double](#double) |  |  |





 

 

 

 



<a name="criteriaselector_dataschema_attribute-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/dataschema/attribute.proto



<a name="tanagra-dataschema-Attribute"></a>

### Attribute
Data for an attribute criteria is a list of categorical values or ranges.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| selected | [Attribute.Selection](#tanagra-dataschema-Attribute-Selection) | repeated |  |
| data_ranges | [tanagra.DataRange](#tanagra-DataRange) | repeated | Contains one or more numeric ranges when referencing a numeric value (e.g. age). |






<a name="tanagra-dataschema-Attribute-Selection"></a>

### Attribute.Selection
A single selected categorical value (e.g. {value: 1234, name: &#34;Diabetes&#34;}.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [tanagra.Value](#tanagra-Value) |  | The value used to build queries. |
| name | [string](#string) |  | The visible name for the selection. This is stored to avoid extra lookups when rendering. |





 

 

 

 



<a name="criteriaselector_dataschema_biovu-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/dataschema/biovu.proto



<a name="tanagra-dataschema-BioVU"></a>

### BioVU



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| sample_filter | [BioVU.SampleFilter](#tanagra-dataschema-BioVU-SampleFilter) |  |  |
| exclude_compromised | [bool](#bool) |  |  |
| exclude_internal | [bool](#bool) |  |  |
| plasma | [bool](#bool) |  |  |





 


<a name="tanagra-dataschema-BioVU-SampleFilter"></a>

### BioVU.SampleFilter


| Name | Number | Description |
| ---- | ------ | ----------- |
| SAMPLE_FILTER_UNKNOWN | 0 |  |
| SAMPLE_FILTER_ANY | 1 |  |
| SAMPLE_FILTER_ONE_HUNDRED | 2 |  |
| SAMPLE_FILTER_FIVE_HUNDRED | 3 |  |


 

 

 



<a name="criteriaselector_dataschema_entity_group-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/dataschema/entity_group.proto



<a name="tanagra-dataschema-EntityGroup"></a>

### EntityGroup
Data for an entity group criteria is a list of selected values.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| selected | [EntityGroup.Selection](#tanagra-dataschema-EntityGroup-Selection) | repeated |  |
| value_data | [tanagra.ValueData](#tanagra-ValueData) |  | Data for an additional categorical or numeric value associated with the selection (e.g. a measurement value). |






<a name="tanagra-dataschema-EntityGroup-Selection"></a>

### EntityGroup.Selection



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [tanagra.Key](#tanagra-Key) |  | The key of the selected value, which references a related entity (e.g. condition for a condition_occurrence). |
| name | [string](#string) |  | The visible name for the selection. This is stored to avoid extra lookups when rendering. |
| entityGroup | [string](#string) |  | The entity group is stored to differentiate between them when multiple are configured within a single criteria. |





 

 

 

 



<a name="criteriaselector_dataschema_multi_attribute-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/dataschema/multi_attribute.proto



<a name="tanagra-dataschema-MultiAttribute"></a>

### MultiAttribute
Data for a multi attribute criteria is a list of categorical or numeric
values.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value_data | [tanagra.ValueData](#tanagra-ValueData) | repeated |  |





 

 

 

 



<a name="criteriaselector_dataschema_output_unfiltered-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/dataschema/output_unfiltered.proto



<a name="tanagra-dataschema-OutputUnfiltered"></a>

### OutputUnfiltered



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| entities | [string](#string) | repeated |  |





 

 

 

 



<a name="criteriaselector_dataschema_text_search-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/dataschema/text_search.proto



<a name="tanagra-dataschema-TextSearch"></a>

### TextSearch
Data for a text search criteria is a list of selected categories and teh text
to search for.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| categories | [TextSearch.Selection](#tanagra-dataschema-TextSearch-Selection) | repeated |  |
| query | [string](#string) |  | The text to search for. |






<a name="tanagra-dataschema-TextSearch-Selection"></a>

### TextSearch.Selection
A single selected category (e.g. {value: 1234, name: &#34;Intake form&#34;}.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [tanagra.Value](#tanagra-Value) |  | The value used to build queries. |
| name | [string](#string) |  | The visible name for the selection. This is stored to avoid extra lookups when rendering. |





 

 

 

 



<a name="criteriaselector_dataschema_unhinted_value-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/dataschema/unhinted_value.proto



<a name="tanagra-dataschema-UnhintedValue"></a>

### UnhintedValue



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| operator | [UnhintedValue.ComparisonOperator](#tanagra-dataschema-UnhintedValue-ComparisonOperator) |  |  |
| min | [double](#double) |  |  |
| max | [double](#double) |  |  |





 


<a name="tanagra-dataschema-UnhintedValue-ComparisonOperator"></a>

### UnhintedValue.ComparisonOperator


| Name | Number | Description |
| ---- | ------ | ----------- |
| COMPARISON_OPERATOR_UNKNOWN | 0 |  |
| COMPARISON_OPERATOR_EQUAL | 1 |  |
| COMPARISON_OPERATOR_BETWEEN | 2 |  |
| COMPARISON_OPERATOR_LESS_THAN_EQUAL | 3 |  |
| COMPARISON_OPERATOR_GREATER_THAN_EQUAL | 4 |  |


 

 

 



<a name="criteriaselector_key-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/key.proto



<a name="tanagra-Key"></a>

### Key



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| string_key | [string](#string) |  |  |
| int64_key | [int64](#int64) |  |  |





 

 

 

 



<a name="criteriaselector_value_config-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/value_config.proto



<a name="tanagra-ValueConfig"></a>

### ValueConfig
Configuration for a value that can be selected.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| attribute | [string](#string) |  | The attribute of interest. |
| title | [string](#string) |  | The visble title to show for this value. |
| unit | [string](#string) | optional | An optional unit to show for this value. |





 

 

 

 



<a name="criteriaselector_value_data-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## criteriaselector/value_data.proto



<a name="tanagra-ValueData"></a>

### ValueData
Configuration for a value that has been selected.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| attribute | [string](#string) |  | The attribute that was selected. |
| numeric | [bool](#bool) |  | Whether this is a numeric or categorical value. Stored to avoid extra lookups when rendering. |
| selected | [ValueData.Selection](#tanagra-ValueData-Selection) | repeated |  |
| range | [DataRange](#tanagra-DataRange) |  | The selected numeric range when referencing a numeric value (e.g. age). |






<a name="tanagra-ValueData-Selection"></a>

### ValueData.Selection
A single selected categorical value (e.g. {value: 1234, name: &#34;Diabetes&#34;}.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [Value](#tanagra-Value) |  | The value used to build queries. |
| name | [string](#string) |  | The visible name for the selection. This is stored to avoid extra lookups when rendering. |





 

 

 

 



<a name="sort_order-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## sort_order.proto



<a name="tanagra-SortOrder"></a>

### SortOrder



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| attribute | [string](#string) |  |  |
| direction | [SortOrder.Direction](#tanagra-SortOrder-Direction) |  |  |





 


<a name="tanagra-SortOrder-Direction"></a>

### SortOrder.Direction


| Name | Number | Description |
| ---- | ------ | ----------- |
| SORT_ORDER_DIRECTION_UNKNOWN | 0 |  |
| SORT_ORDER_DIRECTION_ASCENDING | 1 |  |
| SORT_ORDER_DIRECTION_DESCENDING | 2 |  |


 

 

 



<a name="value-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## value.proto



<a name="tanagra-Value"></a>

### Value



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| string_value | [string](#string) |  |  |
| int64_value | [int64](#int64) |  |  |
| bool_value | [bool](#bool) |  |  |
| timestamp_value | [google.protobuf.Timestamp](#google-protobuf-Timestamp) |  |  |





 

 

 

 



<a name="viz_viz_config-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## viz/viz_config.proto



<a name="tanagra-viz-VizConfig"></a>

### VizConfig
The configuration of a underlay or cohort level visualization.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| sources | [VizConfig.Source](#tanagra-viz-VizConfig-Source) | repeated | The visualization must have one or two sources of data to display. |






<a name="tanagra-viz-VizConfig-Source"></a>

### VizConfig.Source



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| criteriaSelector | [string](#string) |  | The criteria selector (e.g. condition) determines which entities the data is being pulled from. |
| selectionData | [string](#string) | optional | Configuration of the specified criteria selection (e.g. to select conditions under diabetes). |
| entity | [string](#string) | optional | For criteria selectors that return more than one entity. |
| joins | [VizConfig.Source.Join](#tanagra-viz-VizConfig-Source-Join) | repeated | To visualize data from different entities, the data must be joined to a common entity. Each source must specify a series of joins that ends up at the same entity if it does not already come from that entity. |
| attributes | [VizConfig.Source.Attribute](#tanagra-viz-VizConfig-Source-Attribute) | repeated | Which attributes should be returned from the selected data source (e.g. condition_name from condition_occurrence or age from person). |






<a name="tanagra-viz-VizConfig-Source-Attribute"></a>

### VizConfig.Source.Attribute



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| attribute | [string](#string) |  | The attribute to read. |
| numeric_bucketing | [VizConfig.Source.Attribute.NumericBucketing](#tanagra-viz-VizConfig-Source-Attribute-NumericBucketing) |  |  |
| sort_order | [tanagra.SortOrder](#tanagra-SortOrder) | optional | How to sort the data for display. |
| limit | [int64](#int64) | optional | Whether a limited amount of data should be returned (e.g. 10 most common conditions). |






<a name="tanagra-viz-VizConfig-Source-Attribute-NumericBucketing"></a>

### VizConfig.Source.Attribute.NumericBucketing
Converts a continuous numeric range into ids with count as the value.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| thresholds | [double](#double) | repeated | Buckets can be specified as either a list of thresholds or a range and number of buckets. For thresholds [18, 45, 65], results in two buckets [18, 45), and [45, 65). Lesser and greater buckets can be added if desired. |
| intervals | [VizConfig.Source.Attribute.NumericBucketing.Intervals](#tanagra-viz-VizConfig-Source-Attribute-NumericBucketing-Intervals) | optional |  |
| includeLesser | [bool](#bool) | optional | Whether to create buckets for values outside of the explicitly specified ones or ignore them. |
| includeGreater | [bool](#bool) | optional |  |






<a name="tanagra-viz-VizConfig-Source-Attribute-NumericBucketing-Intervals"></a>

### VizConfig.Source.Attribute.NumericBucketing.Intervals
For intervals {min:1, max:5, count: 2}, creates two buckets [1, 3)
and [3, 5).


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| min | [double](#double) |  |  |
| max | [double](#double) |  |  |
| count | [int64](#int64) |  |  |






<a name="tanagra-viz-VizConfig-Source-Join"></a>

### VizConfig.Source.Join



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| entity | [string](#string) |  | The next entity to join to in order to eventually get to the entity the visualization is displaying (e.g. person when joining condition_occurences to age). |
| aggregation | [VizConfig.Source.Join.Aggregation](#tanagra-viz-VizConfig-Source-Join-Aggregation) | optional | When joining an entity with an N:1 relationship (e.g. multiple weight values to a person), an aggregation is often required to make the data visualizable. For example, to visualize weight vs. race, each person needs to have a single weight value associated with them, such as the average or most recent. |






<a name="tanagra-viz-VizConfig-Source-Join-Aggregation"></a>

### VizConfig.Source.Join.Aggregation



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| type | [VizConfig.Source.Join.Aggregation.AggregationType](#tanagra-viz-VizConfig-Source-Join-Aggregation-AggregationType) |  | The type of aggregation being performed. |
| attribute | [string](#string) | optional | The output is always ids and values but aggregation may occur over another field (e.g. date to find the most recent value). |





 


<a name="tanagra-viz-VizConfig-Source-Join-Aggregation-AggregationType"></a>

### VizConfig.Source.Join.Aggregation.AggregationType


| Name | Number | Description |
| ---- | ------ | ----------- |
| UNKNOWN | 0 |  |
| MIN | 1 |  |
| MAX | 2 |  |
| AVERAGE | 3 |  |


 

 

 



## Scalar Value Types

| .proto Type | Notes | C++ | Java | Python | Go | C# | PHP | Ruby |
| ----------- | ----- | --- | ---- | ------ | -- | -- | --- | ---- |
| <a name="double" /> double |  | double | double | float | float64 | double | float | Float |
| <a name="float" /> float |  | float | float | float | float32 | float | float | Float |
| <a name="int32" /> int32 | Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint32 instead. | int32 | int | int | int32 | int | integer | Bignum or Fixnum (as required) |
| <a name="int64" /> int64 | Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint64 instead. | int64 | long | int/long | int64 | long | integer/string | Bignum |
| <a name="uint32" /> uint32 | Uses variable-length encoding. | uint32 | int | int/long | uint32 | uint | integer | Bignum or Fixnum (as required) |
| <a name="uint64" /> uint64 | Uses variable-length encoding. | uint64 | long | int/long | uint64 | ulong | integer/string | Bignum or Fixnum (as required) |
| <a name="sint32" /> sint32 | Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int32s. | int32 | int | int | int32 | int | integer | Bignum or Fixnum (as required) |
| <a name="sint64" /> sint64 | Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int64s. | int64 | long | int/long | int64 | long | integer/string | Bignum |
| <a name="fixed32" /> fixed32 | Always four bytes. More efficient than uint32 if values are often greater than 2^28. | uint32 | int | int | uint32 | uint | integer | Bignum or Fixnum (as required) |
| <a name="fixed64" /> fixed64 | Always eight bytes. More efficient than uint64 if values are often greater than 2^56. | uint64 | long | int/long | uint64 | ulong | integer/string | Bignum |
| <a name="sfixed32" /> sfixed32 | Always four bytes. | int32 | int | int | int32 | int | integer | Bignum or Fixnum (as required) |
| <a name="sfixed64" /> sfixed64 | Always eight bytes. | int64 | long | int/long | int64 | long | integer/string | Bignum |
| <a name="bool" /> bool |  | bool | boolean | boolean | bool | bool | boolean | TrueClass/FalseClass |
| <a name="string" /> string | A string must always contain UTF-8 encoded or 7-bit ASCII text. | string | String | str/unicode | string | string | string | String (UTF-8) |
| <a name="bytes" /> bytes | May contain any arbitrary sequence of bytes. | string | ByteString | str | []byte | ByteString | string | String (ASCII-8BIT) |

