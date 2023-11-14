package bio.terra.tanagra.underlay.serialization;

import java.util.List;
import java.util.Set;

public class SZEntity {
  public String name;
  public String displayName;
  public String description;
  public String allInstancesSqlFile;
  public List<Attribute> attributes;
  public String idAttribute;
  public List<String> optimizeGroupByAttributes;
  public Set<Hierarchy> hierarchies;
  public TextSearch textSearch;

  public static class Attribute {
    public String name;
    public DataType dataType;
    public String valueFieldName;
    public String displayFieldName;
    public String runtimeSqlFunctionWrapper;
    public DataType runtimeDataType;
    public boolean isComputeDisplayHint;
  }

  public static class Hierarchy {
    public String name;
    public String childParentIdPairsSqlFile;
    public String childIdFieldName;
    public String parentIdFieldName;
    public Set<Long> rootNodeIds;
    public String rootNodeIdsSqlFile;
    public String rootIdFieldName;
    public int maxDepth;
    public boolean keepOrphanNodes;
  }

  public static class TextSearch {
    public Set<String> attributes;
    public String idTextPairsSqlFile;
    public String idFieldName;
    public String textFieldName;
  }

  public enum DataType {
    INT64,
    STRING,
    BOOLEAN,
    DATE,
    DOUBLE,
    TIMESTAMP
  }
}
