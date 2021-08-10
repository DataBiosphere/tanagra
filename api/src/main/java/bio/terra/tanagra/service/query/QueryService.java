package bio.terra.tanagra.service.query;

import bio.terra.tanagra.service.search.Query;
import bio.terra.tanagra.service.search.SearchContext;
import bio.terra.tanagra.service.search.Selection;
import bio.terra.tanagra.service.search.SqlVisitor;
import bio.terra.tanagra.service.underlay.Underlay;
import bio.terra.tanagra.service.underlay.UnderlayService;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** A service for executing queries. */
@Service
public class QueryService {

  private final UnderlayService underlayService;

  @Autowired
  public QueryService(UnderlayService underlayService) {
    this.underlayService = underlayService;
  }

  /** Generate an SQL query to select the primary ids for the entity of the entity filter. */
  public String generatePrimaryKeySql(EntityFilter entityFilter) {
    Optional<Underlay> underlay =
        underlayService.getUnderlay(entityFilter.primaryEntity().entity().underlay());
    Preconditions.checkArgument(
        underlay.isPresent(),
        "Unable to find underlay '%s'",
        entityFilter.primaryEntity().entity().underlay());

    Query query =
        Query.builder()
            .selections(
                ImmutableList.of(
                    Selection.PrimaryKey.create(entityFilter.primaryEntity(), Optional.empty())))
            .primaryEntity(entityFilter.primaryEntity())
            .filter(entityFilter.filter())
            .build();
    return new SqlVisitor(SearchContext.builder().underlay(underlay.get()).build())
        .createSql(query);
  }
}
