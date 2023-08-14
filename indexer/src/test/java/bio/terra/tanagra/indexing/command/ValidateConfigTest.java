package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.ValidationUtils;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import org.junit.Test;

public class ValidateConfigTest {
  @Test
  public void duplicateRelationshipDefinition() throws IOException {
    Underlay underlay = Underlay.fromJSON("underlay/DuplicateRelationshipDefinition.json");
    ValidationUtils.validateRelationships(underlay);
  }
}
