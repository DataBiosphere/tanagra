package bio.terra.tanagra.filterbuilder;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.filterbuilder.impl.core.PrimaryEntityFilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.DataRangeOuterClass.DataRange;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass.Value;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
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

public class PrimaryEntityFilterBuilderTest {
  private Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("cmssynpuf_broad");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  @Test
  void enumValCohortFilter() {
    CFPlaceholder.Placeholder config =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("gender").build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "gender",
            true,
            true,
            "core.PrimaryEntityFilterBuilder",
            SZCorePlugin.ATTRIBUTE.getIdInConfig(),
            serializeToJson(config),
            List.of());
    PrimaryEntityFilterBuilder filterBuilder = new PrimaryEntityFilterBuilder(criteriaSelector);

    // Single value.
    DTAttribute.Attribute data =
        DTAttribute.Attribute.newBuilder()
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(Value.newBuilder().setInt64Value(8_532L).build())
                    .setName("Female")
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("gender", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCohortFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("gender"),
            BinaryOperator.EQUALS,
            Literal.forInt64(8_532L));
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple values.
    data =
        DTAttribute.Attribute.newBuilder()
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(Value.newBuilder().setInt64Value(8_532L).build())
                    .setName("Female")
                    .build())
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(Value.newBuilder().setInt64Value(8_507L).build())
                    .setName("Male")
                    .build())
            .build();
    selectionData = new SelectionData("gender", serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    expectedCohortFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("gender"),
            NaryOperator.IN,
            List.of(Literal.forInt64(8_532L), Literal.forInt64(8_507L)));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void numericRangeCohortFilter() {
    CFPlaceholder.Placeholder config =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("age").build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "age",
            true,
            false,
            "core.PrimaryEntityFilterBuilder",
            SZCorePlugin.ATTRIBUTE.getIdInConfig(),
            serializeToJson(config),
            List.of());
    PrimaryEntityFilterBuilder filterBuilder = new PrimaryEntityFilterBuilder(criteriaSelector);

    DTAttribute.Attribute data =
        DTAttribute.Attribute.newBuilder()
            .addDataRanges(DataRange.newBuilder().setMin(0).setMax(89).build())
            .build();
    SelectionData selectionData = new SelectionData("age", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCohortFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("age"),
            NaryOperator.BETWEEN,
            List.of(Literal.forDouble(0.0), Literal.forDouble(89.0)));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void dataFeatureFilter() {
    CFPlaceholder.Placeholder config =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("gender").build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "gender",
            true,
            true,
            "core.PrimaryEntityFilterBuilder",
            SZCorePlugin.ATTRIBUTE.getIdInConfig(),
            serializeToJson(config),
            List.of());
    PrimaryEntityFilterBuilder filterBuilder = new PrimaryEntityFilterBuilder(criteriaSelector);

    List<EntityOutput> dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of());
    assertEquals(1, dataFeatureOutputs.size());
    EntityOutput expectedDataFeatureOutput = EntityOutput.unfiltered(underlay.getPrimaryEntity());
    assertEquals(expectedDataFeatureOutput, dataFeatureOutputs.get(0));
  }
}
