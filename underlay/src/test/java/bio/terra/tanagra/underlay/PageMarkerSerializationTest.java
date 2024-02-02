package bio.terra.tanagra.underlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.tanagra.api.query.PageMarker;
import org.junit.jupiter.api.Test;

public class PageMarkerSerializationTest {
  private static final String TOKEN =
      "BEZ2NPLKRUAQAAASAUIIBAEAAUNAMCH2AEIPUAJA77777777777767ZKABFJ6AQKOAFB2CQSOZSXE2LMPEWXIYLOMFTXEYJNMRSXMEJVQROZRIIAAAABEKK7GI2TKMJUGJSDQYJQHFRTAZJZGUYDSOLGGY3GCOJTGI3TINJQMUZTCOBZGMZWIYZRDISDGOBRGY2TOYTEFUYGKZLCFU2GKZTFFU4TQNTEFUZGKNJWMY3TQMZSGM2DCESEMFXG63TDGZRTQMJTGU3DIZJQGVSGGNZRGA4TIYTGMJQWCZDEGI4DEODFGIYDCNBUMFSTINZWG43DKZJUMEZDGNLCMRQWKNTCMEYGENTEMMZWKGTFMM3GGOBRGM2TMNDFGA2WIYZXGEYDSNDCMZRGCYLEMQZDQMRYMUZDAMJUGRQWKNBXGY3TMNLFGRQTEMZVMJSGCZJWMJQTAYRWMRRTGZJDGZRTQYZSMRQTSLLGG43WKLJUMZSWILLBGFRTELJQGE2GMNJTGQ2GGZRRGI======";
  private static final String SERIALIZED = "{\"pageToken\":\"" + TOKEN + "\",\"offset\":null}";

  @Test
  void deserialize() {
    PageMarker pageMarker = PageMarker.deserialize(SERIALIZED);
    assertNotNull(pageMarker);
    assertEquals(TOKEN, pageMarker.getPageToken());
    assertNull(pageMarker.getOffset());
  }

  @Test
  void serialize() {
    PageMarker tokenMarker = PageMarker.forToken(TOKEN);
    assertEquals(SERIALIZED, tokenMarker.serialize());
  }
}
