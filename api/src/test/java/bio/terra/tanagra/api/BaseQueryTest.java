package bio.terra.tanagra.api;

import bio.terra.tanagra.testing.BaseSpringUnitTest;
import bio.terra.tanagra.underlay.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseQueryTest extends BaseSpringUnitTest {
  protected static int DEFAULT_LIMIT = 30;

  @Autowired protected UnderlaysService underlaysService;
  @Autowired protected QuerysService querysService;

  private Entity entity;

  @BeforeEach
  void setup() {
    entity = underlaysService.getEntity(getUnderlayName(), getEntityName());
  }

  protected abstract String getUnderlayName();

  protected abstract String getEntityName();

  protected Entity getEntity() {
    return entity;
  }
}
