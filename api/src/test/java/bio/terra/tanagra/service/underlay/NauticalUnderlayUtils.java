package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.Entity;
import com.google.common.io.Resources;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Utilities for testing with a common nautical underlay. */
public final class NauticalUnderlayUtils {
  private NauticalUnderlayUtils() {}

  private static final String NAUTICAL_PB_TEXT_FILE = "underlays/sailors.pbtext";

  public static Underlay loadNauticalUnderlay() throws IOException {
    return UnderlayConversion.convert(loadNauticalUnderlayProto());
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

  public static final String NAUTICAL_UNDERLAY_NAME = "Nautical Underlay";
  public static final Entity SAILOR =
      Entity.builder().underlay(NAUTICAL_UNDERLAY_NAME).name("sailors").build();
  public static final Entity BOAT =
      Entity.builder().underlay(NAUTICAL_UNDERLAY_NAME).name("boats").build();
  public static final Entity RESERVATION =
      Entity.builder().underlay(NAUTICAL_UNDERLAY_NAME).name("reservations").build();

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

  public static final Attribute RESERVATION_ID =
      Attribute.builder().name("id").dataType(DataType.INT64).entity(RESERVATION).build();
  public static final Attribute RESERVATION_S_ID =
      Attribute.builder().name("sailors_id").dataType(DataType.INT64).entity(RESERVATION).build();
  public static final Attribute RESERVATION_B_ID =
      Attribute.builder().name("boats_id").dataType(DataType.INT64).entity(RESERVATION).build();
  public static final Attribute RESERVATION_DAY =
      Attribute.builder().name("day").dataType(DataType.STRING).entity(RESERVATION).build();

  public static final BigQueryDataset NAUTICAL_DATASET =
      BigQueryDataset.builder()
          .name("nautical")
          .projectId("my-project-id")
          .datasetId("nautical")
          .build();
  public static final Table SAILOR_TABLE = Table.create("sailors", NAUTICAL_DATASET);
  public static final Table BOAT_TABLE = Table.create("boats", NAUTICAL_DATASET);
  public static final Table RESERVATION_TABLE = Table.create("reservations", NAUTICAL_DATASET);

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
  public static final Column RESERVATION_ID_COL =
      Column.builder().name("r_id").dataType(DataType.INT64).table(RESERVATION_TABLE).build();
  public static final Column RESERVATION_S_ID_COL =
      Column.builder().name("s_id").dataType(DataType.INT64).table(RESERVATION_TABLE).build();
  public static final Column RESERVATION_B_ID_COL =
      Column.builder().name("b_id").dataType(DataType.INT64).table(RESERVATION_TABLE).build();
  public static final Column RESERVATION_DAY_COL =
      Column.builder().name("day").dataType(DataType.STRING).table(RESERVATION_TABLE).build();
}
