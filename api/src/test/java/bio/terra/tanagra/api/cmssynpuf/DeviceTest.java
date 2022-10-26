package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.BaseQueryTest;
import bio.terra.tanagra.api.EntityQueryRequest;
import bio.terra.tanagra.api.entityfilter.TextFilter;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class DeviceTest extends BaseQueryTest {
  @Test
  void noFilter() throws IOException {
    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/device-noFilter.sql");
  }

  @Test
  void textFilter() throws IOException {
    // filter for "device" entity instances that match the search term "hearing aid"
    // i.e. devices that have a name or synonym that includes "hearing aid"
    TextFilter textFilter =
        new TextFilter.Builder()
            .textSearch(getEntity().getTextSearch())
            .functionTemplate(FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH)
            .text("hearing aid")
            .build();

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(textFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/device-textFilter.sql");
  }

  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }

  @Override
  protected String getEntityName() {
    return "device";
  }
}
