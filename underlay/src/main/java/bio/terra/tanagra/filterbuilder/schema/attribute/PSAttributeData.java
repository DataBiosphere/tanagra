package bio.terra.tanagra.filterbuilder.schema.attribute;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.ValueDisplay;
import java.util.List;

public class PSAttributeData {
  public List<ValueDisplay> enumVals;
  public List<Range> ranges;

  public static class Range {
    public String id;
    public Literal min;
    public Literal max;
  }
}
