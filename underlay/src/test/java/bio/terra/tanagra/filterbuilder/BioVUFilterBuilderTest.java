package bio.terra.tanagra.filterbuilder;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.filterbuilder.impl.sd.BioVUFilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFBioVU;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTBioVU;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BioVUFilterBuilderTest {
  private static final String PLUGIN_ID_IN_CONFIG = "sd/biovu";
  private Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("sd20230831_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  @Test
  void sampleFilterOnlyCohortFilter() {
    CFBioVU.BioVU config = CFBioVU.BioVU.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "biovu",
            true,
            false,
            "sd.BioVUFilterBuilder",
            PLUGIN_ID_IN_CONFIG,
            serializeToJson(config),
            List.of());
    BioVUFilterBuilder filterBuilder = new BioVUFilterBuilder(criteriaSelector);

    // Any filter.
    DTBioVU.BioVU data =
        DTBioVU.BioVU.newBuilder()
            .setSampleFilter(DTBioVU.BioVU.SampleFilter.SAMPLE_FILTER_ANY)
            .build();
    SelectionData selectionData = new SelectionData("biovu", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCohortFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("has_biovu_sample"),
            BinaryOperator.EQUALS,
            Literal.forBoolean(true));
    assertEquals(expectedCohortFilter, cohortFilter);

    // 100 yield filter.
    data =
        DTBioVU.BioVU.newBuilder()
            .setSampleFilter(DTBioVU.BioVU.SampleFilter.SAMPLE_FILTER_ONE_HUNDRED)
            .build();
    selectionData = new SelectionData("biovu", serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    expectedCohortFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("biovu_sample_dna_yield"),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            Literal.forInt64(100L));
    assertEquals(expectedCohortFilter, cohortFilter);

    // 500 yield filter.
    data =
        DTBioVU.BioVU.newBuilder()
            .setSampleFilter(DTBioVU.BioVU.SampleFilter.SAMPLE_FILTER_FIVE_HUNDRED)
            .build();
    selectionData = new SelectionData("biovu", serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    expectedCohortFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("biovu_sample_dna_yield"),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            Literal.forInt64(500L));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void excludeCompromisedOnlyCohortFilter() {
    CFBioVU.BioVU config = CFBioVU.BioVU.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "biovu",
            true,
            false,
            "sd.BioVUFilterBuilder",
            PLUGIN_ID_IN_CONFIG,
            serializeToJson(config),
            List.of());
    BioVUFilterBuilder filterBuilder = new BioVUFilterBuilder(criteriaSelector);

    DTBioVU.BioVU data = DTBioVU.BioVU.newBuilder().setExcludeCompromised(true).build();
    SelectionData selectionData = new SelectionData("biovu", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCohortFilter =
        new BooleanNotFilter(
            new AttributeFilter(
                underlay,
                underlay.getPrimaryEntity(),
                underlay.getPrimaryEntity().getAttribute("biovu_sample_is_compromised"),
                BinaryOperator.EQUALS,
                Literal.forBoolean(true)));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void excludeInternalOnlyCohortFilter() {
    CFBioVU.BioVU config = CFBioVU.BioVU.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "biovu",
            true,
            false,
            "sd.BioVUFilterBuilder",
            PLUGIN_ID_IN_CONFIG,
            serializeToJson(config),
            List.of());
    BioVUFilterBuilder filterBuilder = new BioVUFilterBuilder(criteriaSelector);

    DTBioVU.BioVU data = DTBioVU.BioVU.newBuilder().setExcludeInternal(true).build();
    SelectionData selectionData = new SelectionData("biovu", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCohortFilter =
        new BooleanNotFilter(
            new AttributeFilter(
                underlay,
                underlay.getPrimaryEntity(),
                underlay.getPrimaryEntity().getAttribute("biovu_sample_is_nonshippable"),
                BinaryOperator.EQUALS,
                Literal.forBoolean(true)));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void hasPlasmaOnlyCohortFilter() {
    CFBioVU.BioVU config = CFBioVU.BioVU.newBuilder().setPlasmaFilter(true).build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "biovu",
            true,
            false,
            "sd.BioVUFilterBuilder",
            PLUGIN_ID_IN_CONFIG,
            serializeToJson(config),
            List.of());
    BioVUFilterBuilder filterBuilder = new BioVUFilterBuilder(criteriaSelector);

    DTBioVU.BioVU data = DTBioVU.BioVU.newBuilder().build();
    SelectionData selectionData = new SelectionData("biovu", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCohortFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("biovu_sample_has_plasma"),
            BinaryOperator.EQUALS,
            Literal.forBoolean(true));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void allFlagsCohortFilter() {
    CFBioVU.BioVU config = CFBioVU.BioVU.newBuilder().setPlasmaFilter(true).build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "biovu",
            true,
            false,
            "sd.BioVUFilterBuilder",
            PLUGIN_ID_IN_CONFIG,
            serializeToJson(config),
            List.of());
    BioVUFilterBuilder filterBuilder = new BioVUFilterBuilder(criteriaSelector);

    DTBioVU.BioVU data =
        DTBioVU.BioVU.newBuilder()
            .setSampleFilter(DTBioVU.BioVU.SampleFilter.SAMPLE_FILTER_ANY)
            .setExcludeCompromised(true)
            .setExcludeInternal(true)
            .build();
    SelectionData selectionData = new SelectionData("biovu", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);

    // Order of sub-filters doesn't matter, but check that they're all there.
    assertTrue(cohortFilter instanceof BooleanAndOrFilter);
    BooleanAndOrFilter combinedFilter = (BooleanAndOrFilter) cohortFilter;
    assertEquals(BooleanAndOrFilter.LogicalOperator.AND, combinedFilter.getOperator());
    assertEquals(4, combinedFilter.getSubFilters().size());

    EntityFilter plasmaFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("biovu_sample_has_plasma"),
            BinaryOperator.EQUALS,
            Literal.forBoolean(true));
    assertTrue(combinedFilter.getSubFilters().contains(plasmaFilter));
    EntityFilter anySampleFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("has_biovu_sample"),
            BinaryOperator.EQUALS,
            Literal.forBoolean(true));
    assertTrue(combinedFilter.getSubFilters().contains(anySampleFilter));
    EntityFilter excludeCompromisedFilter =
        new BooleanNotFilter(
            new AttributeFilter(
                underlay,
                underlay.getPrimaryEntity(),
                underlay.getPrimaryEntity().getAttribute("biovu_sample_is_compromised"),
                BinaryOperator.EQUALS,
                Literal.forBoolean(true)));
    assertTrue(combinedFilter.getSubFilters().contains(excludeCompromisedFilter));
    EntityFilter excludeInternalFilter =
        new BooleanNotFilter(
            new AttributeFilter(
                underlay,
                underlay.getPrimaryEntity(),
                underlay.getPrimaryEntity().getAttribute("biovu_sample_is_nonshippable"),
                BinaryOperator.EQUALS,
                Literal.forBoolean(true)));
    assertTrue(combinedFilter.getSubFilters().contains(excludeInternalFilter));
  }

  @Test
  void noFlagsCohortFilter() {
    CFBioVU.BioVU config = CFBioVU.BioVU.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "biovu",
            true,
            false,
            "sd.BioVUFilterBuilder",
            PLUGIN_ID_IN_CONFIG,
            serializeToJson(config),
            List.of());
    BioVUFilterBuilder filterBuilder = new BioVUFilterBuilder(criteriaSelector);

    DTBioVU.BioVU data = DTBioVU.BioVU.newBuilder().build();
    SelectionData selectionData = new SelectionData("biovu", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNull(cohortFilter);
  }
}
