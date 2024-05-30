package bio.terra.tanagra.testing;

import bio.terra.tanagra.app.Main;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Base class for Spring unit tests. */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class BaseSpringUnitTest {}
