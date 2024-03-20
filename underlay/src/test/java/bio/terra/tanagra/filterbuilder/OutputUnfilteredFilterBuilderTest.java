package bio.terra.tanagra.filterbuilder;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.filterbuilder.impl.core.OutputUnfilteredFilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFOutputUnfiltered;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTOutputUnfiltered;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.serialization.SZCorePlugin;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OutputUnfilteredFilterBuilderTest {
  private Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("cmssynpuf_broad");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  @Test
  void cohortFilter() {
    CFOutputUnfiltered.OutputUnfiltered config =
        CFOutputUnfiltered.OutputUnfiltered.newBuilder().addEntities("person").build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "demographics",
            false,
            true,
            "core.OutputUnfilteredFilterBuilder",
            SZCorePlugin.OUTPUT_UNFILTERED.getIdInConfig(),
            serializeToJson(config),
            List.of());
    OutputUnfilteredFilterBuilder filterBuilder =
        new OutputUnfilteredFilterBuilder(criteriaSelector);
    assertThrows(
        UnsupportedOperationException.class,
        () -> filterBuilder.buildForCohort(underlay, List.of()));
  }

  @Test
  void singleEntityDataFeatureFilter() {
    CFOutputUnfiltered.OutputUnfiltered config =
        CFOutputUnfiltered.OutputUnfiltered.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "demographics",
            false,
            true,
            "core.OutputUnfilteredFilterBuilder",
            SZCorePlugin.OUTPUT_UNFILTERED.getIdInConfig(),
            serializeToJson(config),
            List.of());
    OutputUnfilteredFilterBuilder filterBuilder =
        new OutputUnfilteredFilterBuilder(criteriaSelector);

    DTOutputUnfiltered.OutputUnfiltered data =
        DTOutputUnfiltered.OutputUnfiltered.newBuilder().addEntities("person").build();
    SelectionData selectionData = new SelectionData(null, serializeToJson(data));

    List<EntityOutput> dataFeatureOutputs =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    EntityOutput expectedDataFeatureOutput = EntityOutput.unfiltered(underlay.getPrimaryEntity());
    assertEquals(expectedDataFeatureOutput, dataFeatureOutputs.get(0));
  }

  @Test
  void multipleEntityDataFeatureFilter() {
    CFOutputUnfiltered.OutputUnfiltered config =
        CFOutputUnfiltered.OutputUnfiltered.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "conditionsAndProcedures",
            false,
            true,
            "core.OutputUnfilteredFilterBuilder",
            SZCorePlugin.OUTPUT_UNFILTERED.getIdInConfig(),
            serializeToJson(config),
            List.of());
    OutputUnfilteredFilterBuilder filterBuilder =
        new OutputUnfilteredFilterBuilder(criteriaSelector);

    DTOutputUnfiltered.OutputUnfiltered data =
        DTOutputUnfiltered.OutputUnfiltered.newBuilder()
            .addEntities("conditionOccurrence")
            .addEntities("procedureOccurrence")
            .build();
    SelectionData selectionData = new SelectionData(null, serializeToJson(data));

    List<EntityOutput> dataFeatureOutputs =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(2, dataFeatureOutputs.size());
    EntityOutput expectedDataFeatureOutput1 =
        EntityOutput.unfiltered(underlay.getEntity("conditionOccurrence"));
    EntityOutput expectedDataFeatureOutput2 =
        EntityOutput.unfiltered(underlay.getEntity("procedureOccurrence"));
    assertTrue(dataFeatureOutputs.contains(expectedDataFeatureOutput1));
    assertTrue(dataFeatureOutputs.contains(expectedDataFeatureOutput2));
  }

  @Test
  void emptyCriteriaDataFeatureFilter() {
    CFOutputUnfiltered.OutputUnfiltered config =
        CFOutputUnfiltered.OutputUnfiltered.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "demographics",
            false,
            true,
            "core.OutputUnfilteredFilterBuilder",
            SZCorePlugin.OUTPUT_UNFILTERED.getIdInConfig(),
            serializeToJson(config),
            List.of());
    OutputUnfilteredFilterBuilder filterBuilder =
        new OutputUnfilteredFilterBuilder(criteriaSelector);

    // Null selection data.
    SelectionData selectionData = new SelectionData("demographics", null);
    List<EntityOutput> dataFeatureOutputs =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertTrue(dataFeatureOutputs.isEmpty());

    // Empty string selection data.
    selectionData = new SelectionData("demographics", "");
    dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertTrue(dataFeatureOutputs.isEmpty());
  }
}
