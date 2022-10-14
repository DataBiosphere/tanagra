package bio.terra.tanagra.plugin.accessControl;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.plugin.accessControl.example.User;
import bio.terra.tanagra.service.jdbc.DataSourceFactory;
import bio.terra.tanagra.service.jdbc.DataSourceId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class DefaultAccessControlPlugin implements IAccessControlPlugin {
    private PluginConfig config;
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Override
    public void init(PluginConfig config) {
        this.config = config;

        DataSourceId dataSourceId = DataSourceId.create(this.config.getValue("datasource-id"));
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSourceFactory.getDataSource(dataSourceId));
    }

    @Override
    public boolean checkAccess(User user, IControlledAccessArtifact artifact) {
        SqlParameterSource params = new MapSqlParameterSource()
            .addValue("user_id", user.getIdentifier())
            .addValue("artifact_type", artifact.getAccessControlType())
            .addValue("artifact_id", artifact.getIdentifier());

        String results = jdbcTemplate.queryForObject("SELECT 1 FROM artifact_acl WHERE user_id = :user_id AND artifact_type = :artifact_type AND artifact_id = :artifact_id", params, String.class);

        return (results != null);
    }

    @Override
    public boolean grantAccess(User user, IControlledAccessArtifact artifact) {
        SqlParameterSource params = new MapSqlParameterSource()
            .addValue("user_id", user.getIdentifier())
            .addValue("artifact_type", artifact.getAccessControlType())
            .addValue("artifact_id", artifact.getIdentifier());

        int status = jdbcTemplate.update("INSERT (user_id, artifact_type, artifact_id) VALUES (:user_id, :artifact_type, :artifact_id) INTO artifact_acl ON CONFLICT DO NOTHING", params);

        return (status == 1);
    }

    @Override
    public boolean revokeAccess(User user, IControlledAccessArtifact artifact) {
        SqlParameterSource params = new MapSqlParameterSource()
            .addValue("user_id", user.getIdentifier())
            .addValue("artifact_type", artifact.getAccessControlType())
            .addValue("artifact_id", artifact.getIdentifier());

        int status = jdbcTemplate.update("DELETE FROM artifact_acl WHERE user_id = :user_id AND artifact_type = :artifact_type AND artifact_id = :artifact_id", params);

        return (status == 1);
    }
}
