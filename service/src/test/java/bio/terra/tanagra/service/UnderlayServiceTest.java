package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.tanagra.app.Main;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
public class UnderlayServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayServiceTest.class);

  @Autowired private UnderlaysService underlaysService;

  @Test
  void listAllOrSelectedUnderlays() {}

  @Test
  void invalid() {
    // Get an invalid underlay.

    // Get an invalid entity.
  }
}
