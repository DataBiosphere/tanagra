package bio.terra.tanagra.plugin.included;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlledEntity;
import bio.terra.tanagra.plugin.identity.User;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class SimpleAccessControlPlugin implements IAccessControlPlugin {
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public void init(PluginConfig config, DataSource dataSource) {
    this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  @Override
  public boolean checkAccess(User user, IAccessControlledEntity entity) {
    SqlParameterSource params = getIdentifierParams(user, entity);

    String results =
        jdbcTemplate.queryForObject(
            "SELECT 1 FROM entity_acl WHERE user_id = :user_id AND access_control_type = :access_control_type AND entity_id = :entity_id",
            params,
            String.class);

    return results != null;
  }

  @Override
  public boolean grantAccess(User user, IAccessControlledEntity entity) {
    SqlParameterSource params = getIdentifierParams(user, entity);

    int status =
        jdbcTemplate.update(
            "INSERT (user_id, access_control_type, entity_id) VALUES (:user_id, :access_control_type, :entity_id) INTO entity_acl ON CONFLICT DO NOTHING",
            params);

    return status != 0;
  }

  @Override
  public boolean revokeAccess(User user, IAccessControlledEntity entity) {
    SqlParameterSource params = getIdentifierParams(user, entity);

    int status =
        jdbcTemplate.update(
            "DELETE FROM entity_acl WHERE user_id = :user_id AND access_control_type = :access_control_type AND entity_id = :entity_id",
            params);

    return status == 1;
  }

  @Override
  public void hydrate(Map<String, ? extends IAccessControlledEntity> entities) {
    SqlParameterSource params =
        new MapSqlParameterSource().addValue("entities", entities.keySet().toArray());

    List<Map<String, Object>> acl =
        jdbcTemplate.queryForList(
            "SELECT entity_id, user_id FROM entity_acl WHERE entity_id IN (:entities)", params);

    acl.forEach(
        row ->
            entities
                .get((String) row.get("entity_id"))
                .addMember(new User((String) row.get("user_id"))));
  }

  private SqlParameterSource getIdentifierParams(User user, IAccessControlledEntity entity) {
    return new MapSqlParameterSource()
        .addValue("user_id", user.getIdentifier())
        .addValue("access_control_type", entity.getAccessControlType())
        .addValue("entity_id", entity.getIdentifier());
  }
}
