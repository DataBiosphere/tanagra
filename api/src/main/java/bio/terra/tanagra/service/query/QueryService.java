package bio.terra.tanagra.service.query;

import bio.terra.tanagra.service.databaseaccess.QueryExecutor;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.Expression.AttributeExpression;
import bio.terra.tanagra.service.search.Query;
import bio.terra.tanagra.service.search.SearchContext;
import bio.terra.tanagra.service.search.SearchEngine;
import bio.terra.tanagra.service.search.Selection;
import bio.terra.tanagra.service.search.SqlVisitor;
import bio.terra.tanagra.service.search.utils.RandomNumberGenerator;
import bio.terra.tanagra.service.underlay.Underlay;
import bio.terra.tanagra.service.underlay.UnderlayService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A service for executing queries.
 *
 * <p>Tanagra logical query types, like {@link EntityFilter}, are used here to create {@link Query}s
 * and execute them.
 */
@Service
public class QueryService {
  private final UnderlayService underlayService;
  private final QueryExecutor.Factory queryExecutorFactory;
  private final RandomNumberGenerator randomNumberGenerator;

  @Autowired
  public QueryService(
      UnderlayService underlayService,
      QueryExecutor.Factory queryExecutorFactory,
      RandomNumberGenerator randomNumberGenerator) {
    this.underlayService = underlayService;
    this.queryExecutorFactory = queryExecutorFactory;
    this.randomNumberGenerator = randomNumberGenerator;
  }

  /** Generate an SQL query to select the primary ids for the entity of the entity filter. */
  public String generatePrimaryKeySql(EntityFilter entityFilter) {
    Underlay underlay = getUnderlay(entityFilter.primaryEntity().entity().underlay());

    Query query =
        Query.builder()
            .selections(
                ImmutableList.of(
                    Selection.PrimaryKey.builder()
                        .entityVariable(entityFilter.primaryEntity())
                        .name("primary_key")
                        .build()))
            .primaryEntity(entityFilter.primaryEntity())
            .filter(entityFilter.filter())
            .build();
    return new SqlVisitor(
            SearchContext.builder()
                .underlay(underlay)
                .randomNumberGenerator(randomNumberGenerator)
                .build())
        .createSql(query);
  }

  /** Generate an SQL query for the entity dataset. */
  public String generateSql(EntityDataset entityDataset) {
    Underlay underlay = getUnderlay(entityDataset.primaryEntity().entity().underlay());
    Query query = createQuery(entityDataset);
    return new SqlVisitor(
            SearchContext.builder()
                .underlay(underlay)
                .randomNumberGenerator(randomNumberGenerator)
                .build())
        .createSql(query);
  }

  public QueryResult retrieveResults(EntityDataset entityDataset) {
    Underlay underlay = getUnderlay(entityDataset.primaryEntity().entity().underlay());
    Query query = createQuery(entityDataset);
    return new SearchEngine(queryExecutorFactory)
        .execute(
            query,
            SearchContext.builder()
                .underlay(underlay)
                .randomNumberGenerator(randomNumberGenerator)
                .build());
  }

  @VisibleForTesting
  Query createQuery(EntityDataset entityDataset) {
    ImmutableList<Selection> selections =
        entityDataset.selectedAttributes().stream()
            .map(
                attribute ->
                    Selection.SelectExpression.builder()
                        .expression(
                            AttributeExpression.create(
                                AttributeVariable.create(
                                    attribute, entityDataset.primaryEntity().variable())))
                        .name(attribute.name())
                        .build())
            .collect(ImmutableList.toImmutableList());

    Query.Builder queryBuilder =
        Query.builder()
            .selections(selections)
            .primaryEntity(entityDataset.primaryEntity())
            .filter(entityDataset.filter());

    if (entityDataset.orderByAttribute() != null) {
      Selection orderBy =
          Selection.SelectExpression.builder()
              .expression(
                  AttributeExpression.create(
                      AttributeVariable.create(
                          entityDataset.orderByAttribute(),
                          entityDataset.primaryEntity().variable())))
              // set the attribute alias to empty string, because we can't use the AS keyword in an
              // ORDER BY clause
              .name("")
              .build();
      queryBuilder.orderBy(orderBy).orderByDirection(entityDataset.orderByDirection());
    }

    if (entityDataset.pageSize() != null) {
      queryBuilder.pageSize(entityDataset.pageSize());
    }

    return queryBuilder.build();
  }

  private Underlay getUnderlay(String underlayName) {
    Optional<Underlay> underlay = underlayService.getUnderlay(underlayName);
    Preconditions.checkArgument(underlay.isPresent(), "Unable to find underlay '%s'", underlayName);
    return underlay.get();
  }
}
