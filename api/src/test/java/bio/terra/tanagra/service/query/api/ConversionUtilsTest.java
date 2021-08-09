package bio.terra.tanagra.service.query.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.service.search.Variable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class ConversionUtilsTest {
  @Test
  void exactlyOneNonNull() {
    assertTrue(ConversionUtils.exactlyOneNonNull(null, null, new Object()));
    assertTrue(ConversionUtils.exactlyOneNonNull(new Object()));
    assertFalse(ConversionUtils.exactlyOneNonNull(new Object(), null, new Object()));
    assertFalse(ConversionUtils.exactlyOneNonNull());
  }

  @Test
  void createAndValidateVariable() {
    assertEquals(Variable.create("x2"), ConversionUtils.createAndValidateVariable("x2"));
    assertThrows(BadRequestException.class, () -> ConversionUtils.createAndValidateVariable("x;"));
  }
}
