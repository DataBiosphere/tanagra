package bio.terra.tanagra.filterbuilder.impl.sd;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJson;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTBioVU;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.List;

public class BioVUFilterBuilder extends FilterBuilder {
  private static final String SAMPLE_HAS_PLASMA_ATTRIBUTE = "biovu_sample_has_plasma";
  private static final String HAS_SAMPLE_ATTRIBUTE = "has_biovu_sample";
  private static final String SAMPLE_DNA_YIELD_ATTRIBUTE = "biovu_sample_dna_yield";
  private static final String SAMPLE_IS_COMPROMISED_ATTRIBUTE = "biovu_sample_is_compromised";
  private static final String SAMPLE_IS_NONSHIPPABLE_ATTRIBUTE = "biovu_sample_is_nonshippable";

  public BioVUFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    if (selectionData.size() > 1) {
      throw new InvalidQueryException("Modifiers are not supported for the biovu plugin");
    }
    DTBioVU.BioVU bioVuSelectionData = deserializeData(selectionData.get(0).getPluginData());

    // Pull the plasma flag from the config.
    CFPlaceholder.Placeholder bioVuConfig = deserializeConfig();

    // Build the attribute filters on the primary entity.
    List<EntityFilter> filtersOnPrimaryEntity = new ArrayList<>();
    if (bioVuConfig.getPlasmaFilter()) {
      filtersOnPrimaryEntity.add(
          new AttributeFilter(
              underlay,
              underlay.getPrimaryEntity(),
              underlay.getPrimaryEntity().getAttribute(SAMPLE_HAS_PLASMA_ATTRIBUTE),
              BinaryOperator.EQUALS,
              Literal.forBoolean(true)));
    }
    switch (bioVuSelectionData.getSampleFilter()) {
      case SAMPLE_FILTER_ANY:
        filtersOnPrimaryEntity.add(
            new AttributeFilter(
                underlay,
                underlay.getPrimaryEntity(),
                underlay.getPrimaryEntity().getAttribute(HAS_SAMPLE_ATTRIBUTE),
                BinaryOperator.EQUALS,
                Literal.forBoolean(true)));
        break;
      case SAMPLE_FILTER_ONE_HUNDRED:
        filtersOnPrimaryEntity.add(
            new AttributeFilter(
                underlay,
                underlay.getPrimaryEntity(),
                underlay.getPrimaryEntity().getAttribute(SAMPLE_DNA_YIELD_ATTRIBUTE),
                BinaryOperator.GREATER_THAN_OR_EQUAL,
                Literal.forInt64(100L)));
        break;
      case SAMPLE_FILTER_FIVE_HUNDRED:
        filtersOnPrimaryEntity.add(
            new AttributeFilter(
                underlay,
                underlay.getPrimaryEntity(),
                underlay.getPrimaryEntity().getAttribute(SAMPLE_DNA_YIELD_ATTRIBUTE),
                BinaryOperator.GREATER_THAN_OR_EQUAL,
                Literal.forInt64(500L)));
        break;
      default:
        // Add no filter.
    }
    if (bioVuSelectionData.getExcludeCompromised()) {
      filtersOnPrimaryEntity.add(
          new BooleanNotFilter(
              new AttributeFilter(
                  underlay,
                  underlay.getPrimaryEntity(),
                  underlay.getPrimaryEntity().getAttribute(SAMPLE_IS_COMPROMISED_ATTRIBUTE),
                  BinaryOperator.EQUALS,
                  Literal.forBoolean(true))));
    }
    if (bioVuSelectionData.getExcludeInternal()) {
      filtersOnPrimaryEntity.add(
          new BooleanNotFilter(
              new AttributeFilter(
                  underlay,
                  underlay.getPrimaryEntity(),
                  underlay.getPrimaryEntity().getAttribute(SAMPLE_IS_NONSHIPPABLE_ATTRIBUTE),
                  BinaryOperator.EQUALS,
                  Literal.forBoolean(true))));
    }

    if (filtersOnPrimaryEntity.isEmpty()) {
      return null;
    } else if (filtersOnPrimaryEntity.size() == 1) {
      return filtersOnPrimaryEntity.get(0);
    } else {
      return new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.AND, filtersOnPrimaryEntity);
    }
  }

  @Override
  public List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    // This plugin filters on attributes of the primary entity, so there's no other entity to
    // output. Outputting the primary entity is already covered by demographics (core/attribute
    // plugin data feature).
    throw new InvalidQueryException("Data features unsupported for BioVU plugin");
  }

  @Override
  public CFPlaceholder.Placeholder deserializeConfig() {
    return deserializeFromJson(
            criteriaSelector.getPluginConfig(), CFPlaceholder.Placeholder.newBuilder())
        .build();
  }

  @Override
  public DTBioVU.BioVU deserializeData(String serialized) {
    return deserializeFromJson(serialized, DTBioVU.BioVU.newBuilder()).build();
  }
}
