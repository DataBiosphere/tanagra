package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.serialization.UFLiteral;
import com.google.common.base.Strings;

public class Literal implements SQLExpression {
  /** Enum for the data types supported by Tanagra. */
  public enum DataType {
    INT64,
    STRING,
    BOOLEAN
  }

  private final Literal.DataType dataType;
  private String stringVal;
  private long int64Val;
  private boolean booleanVal;

  public Literal(String stringVal) {
    this.dataType = DataType.STRING;
    this.stringVal = stringVal;
  }

  public Literal(long int64Val) {
    this.dataType = DataType.INT64;
    this.int64Val = int64Val;
  }

  public Literal(boolean booleanVal) {
    this.dataType = DataType.BOOLEAN;
    this.booleanVal = booleanVal;
  }

  public static Literal fromSerialized(UFLiteral serialized) {
    boolean stringValDefined = !Strings.isNullOrEmpty(serialized.getStringVal());
    boolean int64ValDefined = serialized.getInt64Val() != null;
    boolean booleanValDefined = serialized.getBooleanVal() != null;

    if (stringValDefined) {
      if (int64ValDefined || booleanValDefined) {
        throw new IllegalArgumentException("More than one literal value defined");
      }
      return new Literal(serialized.getStringVal());
    } else if (int64ValDefined) {
      if (stringValDefined || booleanValDefined) {
        throw new IllegalArgumentException("More than one literal value defined");
      }
      return new Literal(serialized.getInt64Val());
    } else if (booleanValDefined) {
      if (stringValDefined || int64ValDefined) {
        throw new IllegalArgumentException("More than one literal value defined");
      }
      return new Literal(serialized.getBooleanVal());
    }

    throw new IllegalArgumentException("No literal values defined");
  }

  @Override
  public String renderSQL() {
    switch (dataType) {
      case STRING:
        return "'" + stringVal + "'";
      case INT64:
        return String.valueOf(int64Val);
      case BOOLEAN:
        return String.valueOf(booleanVal);
      default:
        throw new IllegalArgumentException("Unknown Literal data type");
    }
  }

  public String getStringVal() {
    return dataType.equals(DataType.STRING) ? stringVal : null;
  }

  public Long getInt64Val() {
    return dataType.equals(DataType.INT64) ? int64Val : null;
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "NP_BOOLEAN_RETURN_NULL",
      justification =
          "This value will be used in constructing a SQL string, not used directly in a Java conditional")
  public Boolean getBooleanVal() {
    return dataType.equals(DataType.BOOLEAN) ? booleanVal : null;
  }
}
