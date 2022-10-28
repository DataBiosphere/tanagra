package bio.terra.tanagra.plugin.included;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.IArtifact;
import bio.terra.tanagra.plugin.identity.User;
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
  public boolean checkAccess(User user, IArtifact artifact) {
    SqlParameterSource params = getIdentifierParams(user, artifact);

    String results =
        jdbcTemplate.queryForObject(
            "SELECT 1 FROM artifact_acl WHERE user_id = :user_id AND artifact_type = :artifact_type AND artifact_id = :artifact_id",
            params,
            String.class);

    return results != null;
  }

  @Override
  public boolean grantAccess(User user, IArtifact artifact) {
    SqlParameterSource params = getIdentifierParams(user, artifact);

    int status =
        jdbcTemplate.update(
            "INSERT (user_id, artifact_type, artifact_id) VALUES (:user_id, :artifact_type, :artifact_id) INTO artifact_acl ON CONFLICT DO NOTHING",
            params);

    return status != 0;
  }

  @Override
  public boolean revokeAccess(User user, IArtifact artifact) {
    SqlParameterSource params = getIdentifierParams(user, artifact);

    int status =
        jdbcTemplate.update(
            "DELETE FROM artifact_acl WHERE user_id = :user_id AND artifact_type = :artifact_type AND artifact_id = :artifact_id",
            params);

    return status == 1;
  }

  private SqlParameterSource getIdentifierParams(User user, IArtifact artifact) {
    return new MapSqlParameterSource()
        .addValue("user_id", user.getIdentifier())
        .addValue("artifact_type", artifact.getArtifactType())
        .addValue("artifact_id", artifact.getIdentifier());
  }
}
