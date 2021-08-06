package bio.terra.tanagra.service.query;

import bio.terra.tanagra.service.search.Query;
import bio.terra.tanagra.service.search.SearchContext;
import bio.terra.tanagra.service.search.Selection;
import bio.terra.tanagra.service.search.SqlVisitor;
import bio.terra.tanagra.service.underlay.Underlay;
import bio.terra.tanagra.service.underlay.UnderlayService;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** A service for executing queries. */
@Component
public class QueryService {

  private final UnderlayService underlayService;

  @Autowired
  public QueryService(UnderlayService underlayService) {
    this.underlayService = underlayService;
  }

  /** Generate an SQL query to select the primary ids for the entity of the entity filter. */
  public String generatePrimaryKeySql(EntityFilter entityFilter) {
    Underlay underlay =
        underlayService
            .getUnderlay(entityFilter.primaryEntity().entity().underlay())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        String.format(
                            "Unable to find underlay '%s'",
                            entityFilter.primaryEntity().entity().underlay())));

    Query query =
        Query.create(
            ImmutableList.of(
                Selection.PrimaryKey.create(entityFilter.primaryEntity(), Optional.empty())),
            entityFilter.primaryEntity(),
            Optional.of(entityFilter.filter()));
    return new SqlVisitor(SearchContext.builder().underlay(underlay).build()).createSql(query);
  }
}
