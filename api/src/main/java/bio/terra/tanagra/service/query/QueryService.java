package bio.terra.tanagra.service.query;

import bio.terra.tanagra.service.underlay.UnderlayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// DO NOT SUBMIT comment me.
@Component
public class QueryService {

  private final UnderlayService underlayService;

  @Autowired
  public QueryService(UnderlayService underlayService) {
    this.underlayService = underlayService;
  }
}
