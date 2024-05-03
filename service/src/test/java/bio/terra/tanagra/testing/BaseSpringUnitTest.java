package bio.terra.tanagra.testing;

import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.utils.RandomNumberGenerator;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Base class for Spring unit tests. */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class BaseSpringUnitTest {
  @MockBean protected RandomNumberGenerator randomNumberGenerator;

  private Random random;

  @BeforeEach
  public void beforeEach() {
    random = new Random(2022);
    Mockito.doAnswer(invocation -> Math.abs(random.nextInt(Integer.MAX_VALUE)))
        .when(randomNumberGenerator)
        .getNext();
  }
}
