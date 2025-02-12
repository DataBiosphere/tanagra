package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import java.util.List;

@AnnotatedClass(name = "SZVisualization", markdown = "Configuration for a single visualization.")
public class SZVisualization {
  @AnnotatedField(
      name = "SZVisualization.name",
      markdown =
          "Name of the visualization.\n\n"
              + "This is the unique identifier for the vizualization. The vizualization names "
              + "cannot overlap within an underlay.\n\n"
              + "Name may not include spaces or special characters, only letters and numbers.")
  public String name;

  @AnnotatedField(name = "SZVisualization.title", markdown = "Visible title of the visualization.")
  public String title;

  @AnnotatedField(
      name = "SZVisualization.dataConfigObj",
      markdown =
          "Deserialized configuration of the visualization. VizDataConfig protocol buffer as Java object.")
  public SZVisualizationDataConfig dataConfigObj;

  @AnnotatedField(
      name = "SZVisualization.dataConfig",
      markdown =
          "Serialized configuration of the visualization. VizDataConfig protocol buffer as JSON.")
  public String dataConfig;

  @AnnotatedField(
      name = "SZVisualization.dataConfigFile",
      markdown =
          "Name of the file that contains the serialized configuration of the visualization.\n\n"
              + "This file should be in the same directory as the visualization (e.g. `gender.json`).\n\n"
              + "If this property is specified, the value of the `config` property is ignored.")
  public String dataConfigFile;

  @AnnotatedField(
      name = "SZVisualization.plugin",
      markdown = "Name of the visualization UI plugin.")
  public String plugin;

  @AnnotatedField(
      name = "SZVisualization.pluginConfig",
      markdown = "Serialized configuration of the visualization UI plugin as JSON.")
  public String pluginConfig;

  @AnnotatedField(
      name = "SZVisualization.pluginConfigFile",
      markdown =
          "Name of the file that contains the serialized configuration of the visualization UI plugin.\n\n"
              + "This file should be in the same directory as the visualization (e.g. `gender.json`).\n\n"
              + "If this property is specified, the value of the `pluginConfig` property is ignored.")
  public String pluginConfigFile;

  @AnnotatedClass(
      name = "SZVisualizationDataConfig",
      markdown = "Configuration for a single visualization data config.")
  public static class SZVisualizationDataConfig {
    @AnnotatedField(
        name = "SZVisualizationDataConfig.sources",
        markdown = "List of data config sources for a single visualization.")
    public List<SZVisualizationDCSource> sources;

    @AnnotatedClass(
        name = "SZVisualizationDCSource",
        markdown = "Configuration for a single visualization data config source.")
    public static class SZVisualizationDCSource {
      @AnnotatedField(
          name = "SZVisualizationDCSource.criteriaSelector",
          markdown = "Name of the criteriaSelector.")
      public String criteriaSelector;

      @AnnotatedField(
          name = "SZVisualizationDCSource.selectionData",
          markdown = "Specified criteria selection. (e.g. to select conditions under diabetes)",
          optional = true)
      public String selectionData;

      @AnnotatedField(
          name = "SZVisualizationDCSource.entity",
          markdown =
              "Name of the entity (for criteria selectors that return more than one entity).",
          optional = true)
      public String entity;

      @AnnotatedField(
          name = "SZVisualizationDCSource.joins",
          markdown =
              "To visualize data from different entities, the data must be joined to a "
                  + "common entity. Each source must specify a series of joins that ends up at"
                  + "the same entity if it does not already come from that entity.")
      public List<SZVisualizationDCSJoin> joins;

      @AnnotatedField(
          name = "SZVisualizationDCSource.attributes",
          markdown =
              "Attributes to be returned from the selected data source "
                  + "(e.g. condition_name from condition_occurrence or age from person). ")
      public List<SZVisualizationDCSAttribute> attributes;

      @AnnotatedClass(name = "SZVisualizationDCSJoin", markdown = "Join configuration.")
      public static class SZVisualizationDCSJoin {

        @AnnotatedField(
            name = "SZVisualizationDCSJoin.entity",
            markdown =
                "The next entity to join to in order to eventually get to the entity the "
                    + "visualization is displaying (e.g. person when joining condition_occurences "
                    + "to age). ")
        public String entity;

        @AnnotatedField(
            name = "SZVisualizationDCSJoin.aggregation",
            markdown =
                "When joining an entity with an N:1 relationship (e.g. multiple weight "
                    + "values to a person), an aggregation is often required to make the data "
                    + "visualizable. For example to visualize weight vs. race, each person needs to "
                    + "have a single weight value associated with them, such as the average or most "
                    + "recent. For simple cases, simply counting unique instances of a related entity "
                    + " may be sufficient (e.g. to count people with related condition occurrences).")
        public SZVisualizationDCSJAggregation aggregation;

        @AnnotatedClass(
            name = "SZVisualizationDCSJAggregation",
            markdown = "Aggregation configuration.")
        public static class SZVisualizationDCSJAggregation {
          @AnnotatedField(
              name = "SZVisualizationDCSJAggregation.type",
              markdown = "The type of aggregation being performed.",
              optional = true)
          public SZVisualizationDCSJAType type;

          @AnnotatedField(
              name = "SZVisualizationDCSJAggregation.attribute",
              markdown =
                  "The output is always ids and values but aggregation may occur over "
                      + "another field (e.g. date to find the most recent value).",
              optional = true)
          public String attribute;

          @AnnotatedClass(name = "SZVisualizationDCSJAType", markdown = "Aggregation type.")
          public enum SZVisualizationDCSJAType {
            @AnnotatedField(name = "SZVisualizationDCSJAType.UNIQUE", markdown = "Unique.")
            UNIQUE,
            @AnnotatedField(name = "SZVisualizationDCSJAType.MIN", markdown = "Minimum.")
            MIN,
            @AnnotatedField(name = "SZVisualizationDCSJAType.MAX", markdown = "Maximum.")
            MAX,
            @AnnotatedField(name = "SZVisualizationDCSJAType.AVERAGE", markdown = "Average.")
            AVERAGE
          }
        }
      }

      @AnnotatedClass(name = "SZVisualizationDCSAttribute", markdown = "Attribute configuration.")
      public static class SZVisualizationDCSAttribute {

        @AnnotatedField(
            name = "SZVisualizationDCSAttribute.attribute",
            markdown = "The attribute to read.")
        public String attribute;

        @AnnotatedField(
            name = "SZVisualizationDCSAttribute.numericBucketing",
            markdown = "Numeric Bucketing.")
        public SZVisualizationDCSANumericBucketing numericBucketing;

        @AnnotatedField(
            name = "SZVisualizationDCSAttribute.sortType",
            markdown = "How to sort this attribute for display. Defaults to NAME.",
            optional = true,
            defaultValue = "NAME")
        public SZVisualizationDCSASortType sortType;

        @AnnotatedField(
            name = "SZVisualizationDCSAttribute.sortDescending",
            markdown = "Whether to sort in descending order.",
            optional = true)
        public boolean sortDescending;

        @AnnotatedField(
            name = "SZVisualizationDCSAttribute.limit",
            markdown =
                "Whether a limited amount of data should be returned (e.g. 10 most "
                    + "common conditions).",
            optional = true)
        public int limit;

        @AnnotatedClass(
            name = "SZVisualizationDCSANumericBucketing",
            markdown = "Converts a continuous numeric range into ids with count as the value.")
        public static final class SZVisualizationDCSANumericBucketing {
          @AnnotatedField(
              name = "SZVisualizationDCSANumericBucketing.thresholds",
              markdown =
                  "Buckets can be specified as either a list of thresholds or a range "
                      + "and number of buckets. For thresholds [18, 45, 65], results in two buckets "
                      + "[18, 45), and [45, 65). Lesser and greater buckets can be added if desired.")
          public List<Double> thresholds;

          @AnnotatedField(
              name = "SZVisualizationDCSANumericBucketing.intervals",
              markdown = "Intervals",
              optional = true)
          public SZVisualizationDCSANBIntervals intervals;

          @AnnotatedField(
              name = "SZVisualizationDCSANumericBucketing.includeLesser",
              markdown =
                  "Whether to create buckets for values lesser than the explicitly "
                      + "specified ones or ignore them",
              optional = true)
          public boolean includeLesser;

          @AnnotatedField(
              name = "SZVisualizationDCSANumericBucketing.includeLesser",
              markdown =
                  "Whether to create buckets for values greater than the explicitly "
                      + "specified ones or ignore them",
              optional = true)
          public boolean includeGreater;

          @AnnotatedClass(
              name = "SZVisualizationDCSANBIntervals",
              markdown = "Intervals {min:1, max:5, count: 2}, creates buckets [1, 3) and [3, 5).")
          public static class SZVisualizationDCSANBIntervals {
            @AnnotatedField(name = "SZVisualizationDCSANBIntervals.min", markdown = "Minimum")
            public double min;

            @AnnotatedField(name = "SZVisualizationDCSANBIntervals.max", markdown = "Maximum")
            public double max;

            @AnnotatedField(name = "SZVisualizationDCSANBIntervals.count", markdown = "Count")
            public int count;
          }
        }

        @AnnotatedClass(name = "SZVisualizationDCSASortType", markdown = "Sort type.")
        public enum SZVisualizationDCSASortType {
          @AnnotatedField(name = "SZVisualizationDCSASortType.UNKNOWN", markdown = "Unknown.")
          UNKNOWN("UNKNOWN"),
          @AnnotatedField(name = "SZVisualizationDCSASortType.NAME", markdown = "Name.")
          NAME("NAME"),
          @AnnotatedField(name = "SZVisualizationDCSASortType.VALUE", markdown = "Value.")
          VALUE("VALUE");
          private final String sortType;

          SZVisualizationDCSASortType(String sortType) {
            this.sortType = sortType;
          }

          public String sortType() {
            return sortType;
          }
        }
      }
    }
  }
}
