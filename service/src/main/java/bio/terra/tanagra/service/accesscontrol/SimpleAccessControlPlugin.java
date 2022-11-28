package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.plugin.PluginConfig;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/** Simple access control plugin implementation that maintains basic state in a database */
public class SimpleAccessControlPlugin implements AccessControlPlugin {
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public void init(PluginConfig config, DataSource dataSource) {
    this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, ResourceId resourceId) {
    SqlParameterSource params = getIdentifierParams(userId, resourceType, resourceId);

    String results =
        jdbcTemplate.queryForObject(
            "SELECT 1 FROM entity_acl WHERE user_id = :user_id AND access_control_type = :access_control_type AND entity_id = :entity_id",
            params,
            String.class);

    return results != null;
  }

  @Override
  public ResourceIdCollection listResourceIds(ResourceType type, int offset, int limit) {
    return null;
  }

  private SqlParameterSource getIdentifierParams(
      UserId userId, ResourceType resourceType, ResourceId resourceId) {
    return new MapSqlParameterSource()
        .addValue("user_id", userId)
        .addValue("access_control_type", resourceType.toString())
        .addValue("entity_id", resourceId.getId());
  }
}
