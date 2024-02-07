package bio.terra.tanagra.filterbuilder.schema.entitygroup;

import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.filterbuilder.schema.attribute.PSAttributeConfig;
import bio.terra.tanagra.filterbuilder.schema.groupbycount.PSGroupByCountConfig;
import java.util.List;

public class PSEntityGroupConfig {
  public List<Column> listColumns;
  public List<Column> hierarchyColumns;
  public List<String> classificationEntityGroups;
  public List<String> groupingEntityGroups;
  public boolean isMultiSelect;
  public List<ValueConfig> valueConfigs;
  public SortOrder defaultSort;
  public Integer limit;
  public List<AttributeModifier> attributeModifiers;
  public GroupByModifier groupByModifier;

  public static class Column {
    public String key;
    public String title;
    public String widthPercent;
    public Integer widthExact;
  }

  public static class ValueConfig {
    public String title;
    public String attribute;
  }

  public static class SortOrder {
    public String attribute;
    public OrderByDirection direction;
  }

  public static class AttributeModifier {
    public String name;
    public String title;
    public PSAttributeConfig pluginConfig;
  }

  public static class GroupByModifier {
    public String name;
    public String title;
    public PSGroupByCountConfig pluginConfig;
  }
}
