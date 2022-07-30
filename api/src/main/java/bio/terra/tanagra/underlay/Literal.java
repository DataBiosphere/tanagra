package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFLiteral;
import com.google.common.base.Strings;

public class Literal {
  /** Enum for the data types supported by Tanagra. */
  public enum DataType {
    INT64,
    STRING,
    BOOLEAN;
  }

  private Literal.DataType dataType;
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
    boolean stringValDefined = !Strings.isNullOrEmpty(serialized.stringVal);
    boolean int64ValDefined = serialized.int64Val != null;
    boolean booleanValDefined = serialized.booleanVal != null;

    if (stringValDefined) {
      if (int64ValDefined || booleanValDefined) {
        throw new IllegalArgumentException("More than one literal value defined");
      }
      return new Literal(serialized.stringVal);
    } else if (int64ValDefined) {
      if (stringValDefined || booleanValDefined) {
        throw new IllegalArgumentException("More than one literal value defined");
      }
      return new Literal(serialized.int64Val);
    } else if (booleanValDefined) {
      if (stringValDefined || int64ValDefined) {
        throw new IllegalArgumentException("More than one literal value defined");
      }
      return new Literal(serialized.booleanVal);
    }

    throw new IllegalArgumentException("No literal values defined");
  }
}
