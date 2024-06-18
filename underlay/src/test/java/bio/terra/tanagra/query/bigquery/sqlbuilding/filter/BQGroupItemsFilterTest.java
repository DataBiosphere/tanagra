package bio.terra.tanagra.query.bigquery.sqlbuilding.filter;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.bigquery.BQQueryRunner;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQGroupItemsFilterTest extends BQRunnerTest {
  @Test
  void itemInGroupFilterIntermediateTable() throws IOException {
    GroupItems groupItems = (GroupItems) underlay.getEntityGroup("brandIngredient");

    // No sub-filter.
    ItemInGroupFilter itemInGroupFilter =
        new ItemInGroupFilter(underlay, groupItems, null, null, null, null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getItemsEntity(),
            groupItems.getItemsEntity().getAttribute("name"),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                itemInGroupFilter,
                null,
                null));

    BQTable groupEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getGroupEntity().getName())
            .getTablePointer();
    BQTable itemsEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getItemsEntity().getName())
            .getTablePointer();
    BQTable idPairsTable =
        underlay
            .getIndexSchema()
            .getRelationshipIdPairs(
                groupItems.getName(),
                groupItems.getGroupEntity().getName(),
                groupItems.getItemsEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "itemInGroupNoSubFilterIntTable",
        listQueryResult.getSql(),
        groupEntityTable,
        itemsEntityTable,
        idPairsTable);

    // With sub-filter.
    EntityFilter groupSubFilter =
        new AttributeFilter(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(19_042_336L));
    itemInGroupFilter =
        new ItemInGroupFilter(underlay, groupItems, groupSubFilter, null, null, null);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                itemInGroupFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "itemInGroupWithSubFilterIntTable",
        listQueryResult.getSql(),
        groupEntityTable,
        itemsEntityTable,
        idPairsTable);

    // With group by, no attribute, no sub-filter.
    itemInGroupFilter =
        new ItemInGroupFilter(underlay, groupItems, null, null, BinaryOperator.GREATER_THAN, 2);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                itemInGroupFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "itemInGroupWithGroupByNoSubFilterIntTable",
        listQueryResult.getSql(),
        groupEntityTable,
        itemsEntityTable,
        idPairsTable);

    // With group by attribute, with sub-filter.
    groupSubFilter =
        new AttributeFilter(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getAttribute("standard_concept"),
            BinaryOperator.EQUALS,
            Literal.forString("S"));
    itemInGroupFilter =
        new ItemInGroupFilter(
            underlay,
            groupItems,
            groupSubFilter,
            List.of(groupItems.getItemsEntity().getAttribute("vocabulary")),
            BinaryOperator.GREATER_THAN,
            2);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                itemInGroupFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "itemInGroupWithGroupByAttributeWithSubFilterIntTable",
        listQueryResult.getSql(),
        groupEntityTable,
        itemsEntityTable,
        idPairsTable);
  }

  @Test
  void groupHasItemsFilterIntermediateTable() throws IOException {
    // Intermediate table.
    GroupItems groupItems = (GroupItems) underlay.getEntityGroup("brandIngredient");
    GroupHasItemsFilter groupHasItemsFilter =
        new GroupHasItemsFilter(underlay, groupItems, null, null, null, null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getAttribute("name"),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getGroupEntity(),
                List.of(simpleAttribute),
                groupHasItemsFilter,
                null,
                null));

    BQTable groupEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getGroupEntity().getName())
            .getTablePointer();
    BQTable itemsEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getItemsEntity().getName())
            .getTablePointer();
    BQTable idPairsTable =
        underlay
            .getIndexSchema()
            .getRelationshipIdPairs(
                groupItems.getName(),
                groupItems.getGroupEntity().getName(),
                groupItems.getItemsEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "groupHasItemsIntTable",
        listQueryResult.getSql(),
        groupEntityTable,
        itemsEntityTable,
        idPairsTable);

    // Foreign key on items entity table.
    // The cmssynpuf underlay does not have an example of this type of relationship
    // (i.e. foreign key relationship on the filter entity).
    // This is typical for things like vitals (e.g. list of pulse readings for a person,
    // query is to get all persons with at least one pulse reading). So for this part
    // of the test, we use the SD underlay. But since we the GHA does not have
    // credentials to query against an SD dataset, here we just check the generated SQL.
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("sd20230331_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    Underlay underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
    BQQueryRunner bqQueryRunner =
        new BQQueryRunner(szService.bigQuery.queryProjectId, szService.bigQuery.dataLocation);

    groupItems = (GroupItems) underlay.getEntityGroup("pulsePerson");
    groupHasItemsFilter = new GroupHasItemsFilter(underlay, groupItems, null, null, null, null);
    simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getIdAttribute(),
            false);
    SqlQueryRequest sqlQueryRequest =
        bqQueryRunner.buildListQuerySqlAgainstIndexData(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getGroupEntity(),
                List.of(simpleAttribute),
                groupHasItemsFilter,
                null,
                null));

    groupEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getGroupEntity().getName())
            .getTablePointer();
    itemsEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getItemsEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "groupHasItemsFKItemsTable", sqlQueryRequest.getSql(), groupEntityTable, itemsEntityTable);
  }
}
