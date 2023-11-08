package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.query.Literal;
import java.util.Set;

public class SZEntity {
  public String name;
  public String displayName;
  public String description;
  public String allInstancesSqlFile;
  public Set<Attribute> attributes;
  public String idAttribute;
  public Set<String> optimizeGroupByAttributes;
  public Set<Hierarchy> hierarchies;
  public TextSearch textSearch;

  public static class Attribute {
    public String name;
    public Literal.DataType dataType;
    public String valueFieldName;
    public String displayFieldName;
    public String runtimeSqlFunctionWrapper;
    public Literal.DataType runtimeDataType;
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
}
