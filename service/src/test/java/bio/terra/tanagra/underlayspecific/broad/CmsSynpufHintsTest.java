package bio.terra.tanagra.underlayspecific.broad;

import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.api2.query.hint.HintInstance;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlayspecific.BaseHintsTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("broad-underlays")
public class CmsSynpufHintsTest extends BaseHintsTest {

  @Override
  protected String getUnderlayName() {
    return "cmssynpuf";
  }

  @Test
  void personEntityLevel() {
    Entity entity = underlayService.getUnderlay(getUnderlayName()).getEntity("person");
    List<HintInstance> expectedHints =
        List.of(
            new HintInstance(
                entity.getAttribute("gender"),
                Map.of(
                    new ValueDisplay(new Literal(8_532L), "FEMALE"),
                    1_292_861L,
                    new ValueDisplay(new Literal(8_507L), "MALE"),
                    1_033_995L)),
            new HintInstance(
                entity.getAttribute("race"),
                Map.of(
                    new ValueDisplay(new Literal(8_516L), "Black or African American"),
                    247_723L,
                    new ValueDisplay(new Literal(0L), "No matching concept"),
                    152_425L,
                    new ValueDisplay(new Literal(8_527L), "White"),
                    1_926_708L)),
            new HintInstance(
                entity.getAttribute("ethnicity"),
                Map.of(
                    new ValueDisplay(new Literal(38_003_563L), "Hispanic or Latino"),
                    54_453L,
                    new ValueDisplay(new Literal(38_003_564L), "Not Hispanic or Latino"),
                    2_272_403L)),
            new HintInstance(entity.getAttribute("age"), 40.0, 114.0),
            new HintInstance(entity.getAttribute("year_of_birth"), 1909.0, 1983.0));
    assertEntityLevelHintsMatch("person", expectedHints);
  }

  @Test
  void conditionOccurrenceEntityLevel() {
    Entity entity = underlayService.getUnderlay(getUnderlayName()).getEntity("conditionOccurrence");
    List<HintInstance> expectedHints =
        List.of(
            new HintInstance(entity.getAttribute("age_at_occurrence"), 24.0, 101.0),
            new HintInstance(
                entity.getAttribute("stop_reason"),
                Map.of(new ValueDisplay(new Literal(null), null), 0L)));
    assertEntityLevelHintsMatch("conditionOccurrence", expectedHints);
  }
}
