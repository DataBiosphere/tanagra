package bio.terra.tanagra.underlayspecific.broad;

import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.ValueDisplay;
import bio.terra.tanagra.underlay.displayhint.EnumVal;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import bio.terra.tanagra.underlayspecific.BaseHintsTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("broad-underlays")
public class CmsSynpufHintsTest extends BaseHintsTest {

  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }

  @Test
  void personEntityLevel() {
    Map<String, DisplayHint> expectedHints =
        Map.of(
            "gender",
                buildEnumVals(
                    List.of(
                        new EnumVal(new ValueDisplay(new Literal(8_532L), "FEMALE"), 1_292_861),
                        new EnumVal(new ValueDisplay(new Literal(8_507L), "MALE"), 1_033_995))),
            "race",
                buildEnumVals(
                    List.of(
                        new EnumVal(
                            new ValueDisplay(new Literal(8_516L), "Black or African American"),
                            247_723),
                        new EnumVal(
                            new ValueDisplay(new Literal(0L), "No matching concept"), 152_425),
                        new EnumVal(new ValueDisplay(new Literal(8_527L), "White"), 1_926_708))),
            "ethnicity",
                buildEnumVals(
                    List.of(
                        new EnumVal(
                            new ValueDisplay(new Literal(38_003_563L), "Hispanic or Latino"),
                            54_453),
                        new EnumVal(
                            new ValueDisplay(new Literal(38_003_564L), "Not Hispanic or Latino"),
                            2_272_403))),
            "age", new NumericRange(40.0, 114.0),
            "year_of_birth", new NumericRange(1909.0, 1983.0));
    assertEntityLevelHintsMatch("person", expectedHints);
  }

  @Test
  void conditionOccurrenceEntityLevel() {
    Map<String, DisplayHint> expectedHints =
        Map.of(
            "age_at_occurrence",
            new NumericRange(24.0, 101.0),
            "stop_reason",
            buildEnumVals(List.of(new EnumVal(new ValueDisplay(new Literal(null), null), 0))));
    assertEntityLevelHintsMatch("condition_occurrence", expectedHints);
  }
}
