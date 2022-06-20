package bio.terra.tanagra.service.underlay;

import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.Relationship;
import com.google.common.io.Resources;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Utilities for testing with a common nautical underlay. */
public final class NauticalUnderlayUtils {
  private NauticalUnderlayUtils() {}

  private static final String NAUTICAL_PB_TEXT_FILE = "underlays/nautical.pbtext";

  public static Underlay loadNauticalUnderlay() {
    try {
      return UnderlayConversion.convert(loadNauticalUnderlayProto());
    } catch (IOException e) {
      throw new InternalServerErrorException("Unable to load nautical underlay.", e);
    }
  }

  public static bio.terra.tanagra.proto.underlay.Underlay loadNauticalUnderlayProto()
      throws IOException {
    bio.terra.tanagra.proto.underlay.Underlay.Builder builder =
        bio.terra.tanagra.proto.underlay.Underlay.newBuilder();
    TextFormat.merge(
        Resources.toString(Resources.getResource(NAUTICAL_PB_TEXT_FILE), StandardCharsets.UTF_8),
        builder);
    return builder.build();
  }

  public static final String NAUTICAL_UNDERLAY_NAME = "nautical_underlay";
  public static final String NAUTICAL_CRITERIA_CONFIGS = "[{\"type\":\"concept\",\"title\":\"Conditions\",\"defaultName\":\"Contains Conditions Codes\",\"plugin\":{\"columns\":[{\"key\":\"concept_name\",\"width\":\"100%\",\"title\":\"Concept Name\"},{\"key\":\"concept_id\",\"width\":120,\"title\":\"Concept ID\"},{\"key\":\"standard_concept\",\"width\":180,\"title\":\"Source/Standard\"},{\"key\":\"vocabulary_id\",\"width\":120,\"title\":\"Vocab\"},{\"key\":\"concept_code\",\"width\":120,\"title\":\"Code\"}],\"entities\":[{\"name\":\"condition\",\"selectable\":true,\"hierarchical\":true}]}},{\"type\":\"concept\",\"title\":\"Procedures\",\"defaultName\":\"Contains Procedures Codes\",\"plugin\":{\"columns\":[{\"key\":\"concept_name\",\"width\":\"100%\",\"title\":\"Concept Name\"},{\"key\":\"concept_id\",\"width\":120,\"title\":\"Concept ID\"},{\"key\":\"standard_concept\",\"width\":180,\"title\":\"Source/Standard\"},{\"key\":\"vocabulary_id\",\"width\":120,\"title\":\"Vocab\"},{\"key\":\"concept_code\",\"width\":120,\"title\":\"Code\"}],\"entities\":[{\"name\":\"procedure\",\"selectable\":true,\"hierarchical\":true}]}},{\"type\":\"concept\",\"title\":\"Observations\",\"defaultName\":\"Contains Observations Codes\",\"plugin\":{\"columns\":[{\"key\":\"concept_name\",\"width\":\"100%\",\"title\":\"Concept Name\"},{\"key\":\"concept_id\",\"width\":120,\"title\":\"Concept ID\"},{\"key\":\"standard_concept\",\"width\":180,\"title\":\"Source/Standard\"},{\"key\":\"vocabulary_id\",\"width\":120,\"title\":\"Vocab\"},{\"key\":\"concept_code\",\"width\":120,\"title\":\"Code\"}],\"entities\":[{\"name\":\"observation\",\"selectable\":true}]}},{\"type\":\"concept\",\"title\":\"Drugs\",\"defaultName\":\"Contains Drugs Codes\",\"plugin\":{\"columns\":[{\"key\":\"concept_name\",\"width\":\"100%\",\"title\":\"Concept Name\"},{\"key\":\"concept_id\",\"width\":120,\"title\":\"Concept ID\"},{\"key\":\"standard_concept\",\"width\":180,\"title\":\"Source/Standard\"},{\"key\":\"vocabulary_id\",\"width\":120,\"title\":\"Vocab\"},{\"key\":\"concept_code\",\"width\":120,\"title\":\"Code\"}],\"entities\":[{\"name\":\"ingredient\",\"selectable\":true,\"hierarchical\":true},{\"name\":\"brand\",\"sourceConcepts\":true,\"attributes\":[\"concept_name\",\"concept_id\",\"standard_concept\",\"concept_code\"],\"listChildren\":{\"entity\":\"ingredient\",\"idPath\":\"relationshipFilter.filter.binaryFilter.attributeValue\",\"filter\":{\"relationshipFilter\":{\"outerVariable\":\"ingredient\",\"newVariable\":\"brand\",\"newEntity\":\"brand\",\"filter\":{\"binaryFilter\":{\"attributeVariable\":{\"variable\":\"brand\",\"name\":\"concept_id\"},\"operator\":\"EQUALS\",\"attributeValue\":{\"int64Val\":0}}}}}}}]}},{\"type\":\"attribute\",\"title\":\"Ethnicity\",\"defaultName\":\"Contains Ethnicity Codes\",\"plugin\":{\"attribute\":\"ethnicity_concept_id\"}},{\"type\":\"attribute\",\"title\":\"Gender Identity\",\"defaultName\":\"Contains Gender Identity Codes\",\"plugin\":{\"attribute\":\"gender_concept_id\"}},{\"type\":\"attribute\",\"title\":\"Race\",\"defaultName\":\"Contains Race Codes\",\"plugin\":{\"attribute\":\"race_concept_id\"}},{\"type\":\"attribute\",\"title\":\"Sex Assigned at Birth\",\"defaultName\":\"Contains Sex Assigned at Birth Codes\",\"plugin\":{\"attribute\":\"sex_at_birth_concept_id\"}},{\"type\":\"attribute\",\"title\":\"Year at Birth\",\"defaultName\":\"Contains Year at Birth Values\",\"plugin\":{\"attribute\":\"year_of_birth\"}}]";
  public static final Entity SAILOR =
      Entity.builder().underlay(NAUTICAL_UNDERLAY_NAME).name("sailors").build();
  public static final Entity BOAT =
      Entity.builder().underlay(NAUTICAL_UNDERLAY_NAME).name("boats").build();
  public static final Entity RESERVATION =
      Entity.builder().underlay(NAUTICAL_UNDERLAY_NAME).name("reservations").build();
  public static final Entity BOAT_ENGINE =
      Entity.builder().underlay(NAUTICAL_UNDERLAY_NAME).name("boat_engines").build();
  public static final Entity BOAT_ELECTRIC_ANCHOR =
      Entity.builder().underlay(NAUTICAL_UNDERLAY_NAME).name("boat_electric_anchors").build();

  public static final Attribute SAILOR_ID =
      Attribute.builder().name("id").dataType(DataType.INT64).entity(SAILOR).build();
  public static final Attribute SAILOR_NAME =
      Attribute.builder().name("name").dataType(DataType.STRING).entity(SAILOR).build();
  public static final Attribute SAILOR_RATING =
      Attribute.builder().name("rating").dataType(DataType.INT64).entity(SAILOR).build();

  public static final Attribute BOAT_ID =
      Attribute.builder().name("id").dataType(DataType.INT64).entity(BOAT).build();
  public static final Attribute BOAT_NAME =
      Attribute.builder().name("name").dataType(DataType.STRING).entity(BOAT).build();
  public static final Attribute BOAT_COLOR =
      Attribute.builder().name("color").dataType(DataType.STRING).entity(BOAT).build();
  public static final Attribute BOAT_TYPE_ID =
      Attribute.builder().name("type_id").dataType(DataType.INT64).entity(BOAT).build();
  public static final Attribute BOAT_TYPE_NAME =
      Attribute.builder().name("type_name").dataType(DataType.STRING).entity(BOAT).build();
  public static final Attribute BOAT_T_PATH_TYPE_ID =
      Attribute.builder()
          .name("t_path_type_id")
          .dataType(DataType.STRING)
          .entity(BOAT)
          .isGenerated(true)
          .build();
  public static final Attribute BOAT_T_NUMCHILDREN_TYPE_ID =
      Attribute.builder()
          .name("t_numChildren_type_id")
          .dataType(DataType.INT64)
          .entity(BOAT)
          .isGenerated(true)
          .build();

  public static final Attribute RESERVATION_ID =
      Attribute.builder().name("id").dataType(DataType.INT64).entity(RESERVATION).build();
  public static final Attribute RESERVATION_S_ID =
      Attribute.builder().name("sailors_id").dataType(DataType.INT64).entity(RESERVATION).build();
  public static final Attribute RESERVATION_B_ID =
      Attribute.builder().name("boats_id").dataType(DataType.INT64).entity(RESERVATION).build();
  public static final Attribute RESERVATION_DAY =
      Attribute.builder().name("day").dataType(DataType.STRING).entity(RESERVATION).build();

  public static final Attribute BOAT_ENGINE_ID =
      Attribute.builder().name("id").dataType(DataType.INT64).entity(BOAT_ENGINE).build();
  public static final Attribute BOAT_ENGINE_NAME =
      Attribute.builder().name("name").dataType(DataType.STRING).entity(BOAT_ENGINE).build();

  public static final Attribute BOAT_ELECTRIC_ANCHOR_ID =
      Attribute.builder().name("id").dataType(DataType.INT64).entity(BOAT_ELECTRIC_ANCHOR).build();
  public static final Attribute BOAT_ELECTRIC_ANCHOR_NAME =
      Attribute.builder()
          .name("name")
          .dataType(DataType.STRING)
          .entity(BOAT_ELECTRIC_ANCHOR)
          .build();

  public static final Relationship SAILOR_RESERVATION_RELATIONSHIP =
      Relationship.builder()
          .name("sailor_reservation")
          .entity1(SAILOR)
          .entity2(RESERVATION)
          .build();
  public static final Relationship BOAT_RESERVATION_RELATIONSHIP =
      Relationship.builder().name("boat_reservation").entity1(BOAT).entity2(RESERVATION).build();
  public static final Relationship SAILOR_BOAT_RELATIONSHIP =
      Relationship.builder().name("sailor_boat").entity1(SAILOR).entity2(BOAT).build();

  public static final BigQueryDataset NAUTICAL_DATASET =
      BigQueryDataset.builder()
          .name("nautical")
          .projectId("my-project-id")
          .datasetId("nautical")
          .build();
  public static final Table SAILOR_TABLE = Table.create("sailors", NAUTICAL_DATASET);
  public static final Table BOAT_TABLE = Table.create("boats", NAUTICAL_DATASET);
  public static final Table SAILORS_FAVORITE_BOATS_TABLE =
      Table.create("sailors_favorite_boats", NAUTICAL_DATASET);
  public static final Table RESERVATION_TABLE = Table.create("reservations", NAUTICAL_DATASET);
  public static final Table BOAT_TYPE_TABLE = Table.create("boat_types", NAUTICAL_DATASET);
  public static final Table BOAT_TYPE_DESCENDANTS_TABLE =
      Table.create("boat_types_descendants", NAUTICAL_DATASET);
  public static final Table BOAT_TYPE_CHILDREN_TABLE =
      Table.create("boat_types_children", NAUTICAL_DATASET);
  public static final Table BOAT_TYPE_PATHS_TABLE =
      Table.create("boat_types_paths", NAUTICAL_DATASET);
  public static final Table BOAT_PARTS_TABLE = Table.create("boat_parts", NAUTICAL_DATASET);

  public static final Column SAILOR_ID_COL =
      Column.builder().name("s_id").dataType(DataType.INT64).table(SAILOR_TABLE).build();
  public static final Column SAILOR_NAME_COL =
      Column.builder().name("s_name").dataType(DataType.STRING).table(SAILOR_TABLE).build();
  public static final Column SAILOR_RATING_COL =
      Column.builder().name("rating").dataType(DataType.INT64).table(SAILOR_TABLE).build();
  public static final Column BOAT_ID_COL =
      Column.builder().name("b_id").dataType(DataType.INT64).table(BOAT_TABLE).build();
  public static final Column BOAT_NAME_COL =
      Column.builder().name("b_name").dataType(DataType.STRING).table(BOAT_TABLE).build();
  public static final Column BOAT_COLOR_COL =
      Column.builder().name("color").dataType(DataType.STRING).table(BOAT_TABLE).build();
  public static final Column BOAT_BT_ID_COL =
      Column.builder().name("bt_id").dataType(DataType.INT64).table(BOAT_TABLE).build();
  public static final Column SAILORS_FAVORITE_BOATS_S_ID_COL =
      Column.builder()
          .name("s_id")
          .dataType(DataType.INT64)
          .table(SAILORS_FAVORITE_BOATS_TABLE)
          .build();
  public static final Column SAILORS_FAVORITE_BOATS_B_ID_COL =
      Column.builder()
          .name("b_id")
          .dataType(DataType.INT64)
          .table(SAILORS_FAVORITE_BOATS_TABLE)
          .build();
  public static final Column RESERVATION_ID_COL =
      Column.builder().name("r_id").dataType(DataType.INT64).table(RESERVATION_TABLE).build();
  public static final Column RESERVATION_S_ID_COL =
      Column.builder().name("s_id").dataType(DataType.INT64).table(RESERVATION_TABLE).build();
  public static final Column RESERVATION_B_ID_COL =
      Column.builder().name("b_id").dataType(DataType.INT64).table(RESERVATION_TABLE).build();
  public static final Column RESERVATION_DAY_COL =
      Column.builder().name("day").dataType(DataType.STRING).table(RESERVATION_TABLE).build();
  public static final Column BOAT_TYPE_ID_COL =
      Column.builder().name("bt_id").dataType(DataType.INT64).table(BOAT_TYPE_TABLE).build();
  public static final Column BOAT_TYPE_NAME_COL =
      Column.builder().name("bt_name").dataType(DataType.STRING).table(BOAT_TYPE_TABLE).build();
  public static final Column BOAT_TYPE_DESCENDANTS_ANCESTOR_COL =
      Column.builder()
          .name("bt_ancestor")
          .dataType(DataType.INT64)
          .table(BOAT_TYPE_DESCENDANTS_TABLE)
          .build();
  public static final Column BOAT_TYPE_DESCENDANTS_DESCENDANT_COL =
      Column.builder()
          .name("bt_descendant")
          .dataType(DataType.INT64)
          .table(BOAT_TYPE_DESCENDANTS_TABLE)
          .build();
  public static final Column BOAT_TYPE_CHILDREN_PARENT_COL =
      Column.builder()
          .name("btc_parent")
          .dataType(DataType.INT64)
          .table(BOAT_TYPE_CHILDREN_TABLE)
          .build();
  public static final Column BOAT_TYPE_CHILDREN_CHILD_COL =
      Column.builder()
          .name("btc_child")
          .dataType(DataType.INT64)
          .table(BOAT_TYPE_CHILDREN_TABLE)
          .build();
  public static final Column BOAT_TYPE_CHILDREN_IS_EXPIRED_COL =
      Column.builder()
          .name("btc_is_expired")
          .dataType(DataType.STRING)
          .table(BOAT_TYPE_CHILDREN_TABLE)
          .build();
  public static final Column BOAT_TYPE_PATHS_NODE_COL =
      Column.builder()
          .name("bt_node")
          .dataType(DataType.INT64)
          .table(BOAT_TYPE_PATHS_TABLE)
          .build();
  public static final Column BOAT_TYPE_PATHS_PATH_COL =
      Column.builder()
          .name("bt_path")
          .dataType(DataType.STRING)
          .table(BOAT_TYPE_PATHS_TABLE)
          .build();
  public static final Column BOAT_TYPE_PATHS_NUMCHILDREN_COL =
      Column.builder()
          .name("bt_num_children")
          .dataType(DataType.INT64)
          .table(BOAT_TYPE_PATHS_TABLE)
          .build();
  public static final Column BOAT_PARTS_ID_COL =
      Column.builder().name("bp_id").dataType(DataType.INT64).table(BOAT_PARTS_TABLE).build();
  public static final Column BOAT_PARTS_NAME_COL =
      Column.builder().name("bp_name").dataType(DataType.STRING).table(BOAT_PARTS_TABLE).build();
  public static final Column BOAT_PARTS_TYPE_COL =
      Column.builder().name("bp_type").dataType(DataType.STRING).table(BOAT_PARTS_TABLE).build();
  public static final Column BOAT_PARTS_REQUIRES_ELECTRICITY_COL =
      Column.builder()
          .name("bp_requires_electricity")
          .dataType(DataType.STRING)
          .table(BOAT_PARTS_TABLE)
          .build();

  public static final BinaryColumnFilterOperator BOAT_PARTS_TYPE_COL_OPERATOR =
      BinaryColumnFilterOperator.EQUALS;
  public static final ColumnValue BOAT_PARTS_TYPE_COL_ENGINE_VALUE =
      ColumnValue.builder().stringVal("engine").build();
  public static final ColumnValue BOAT_PARTS_TYPE_COL_ANCHOR_VALUE =
      ColumnValue.builder().stringVal("anchor").build();
  public static final ColumnValue BOAT_PARTS_REQUIRES_ELECTRICITY_COL_TRUE_VALUE =
      ColumnValue.builder().stringVal("true").build();

  public static final Hierarchy BOAT_TYPE_HIERARCHY =
      Hierarchy.builder()
          .descendantsTable(
              Hierarchy.DescendantsTable.builder()
                  .ancestor(BOAT_TYPE_DESCENDANTS_ANCESTOR_COL)
                  .descendant(BOAT_TYPE_DESCENDANTS_DESCENDANT_COL)
                  .build())
          .childrenTable(
              Hierarchy.ChildrenTable.builder()
                  .parent(BOAT_TYPE_CHILDREN_PARENT_COL)
                  .child(BOAT_TYPE_CHILDREN_CHILD_COL)
                  .tableFilter(
                      TableFilter.builder()
                          .binaryColumnFilter(
                              BinaryColumnFilter.builder()
                                  .column(BOAT_TYPE_CHILDREN_IS_EXPIRED_COL)
                                  .operator(BinaryColumnFilterOperator.EQUALS)
                                  .value(ColumnValue.builder().stringVal("false").build())
                                  .build())
                          .build())
                  .build())
          .pathsTable(
              Hierarchy.PathsTable.builder()
                  .node(BOAT_TYPE_PATHS_NODE_COL)
                  .path(BOAT_TYPE_PATHS_PATH_COL)
                  .numChildren(BOAT_TYPE_PATHS_NUMCHILDREN_COL)
                  .build())
          .build();

  public static final TextSearchInformation SAILORS_TEXT_SEARCH_INFORMATION =
      TextSearchInformation.builder()
          .textTable(
              TextSearchInformation.TextTable.builder()
                  .lookupTableKey(SAILOR_ID_COL)
                  .fullText(SAILOR_NAME_COL)
                  .build())
          .build();
}
