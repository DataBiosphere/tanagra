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
    public boolean checkAccess(User user, IControlledAccessAsset asset) {
        SqlParameterSource params = new MapSqlParameterSource()
            .addValue("user_id", user.getIdentifier())
            .addValue("asset_type", asset.getAccessControlType())
            .addValue("asset_id", asset.getIdentifier());

        String results = jdbcTemplate.queryForObject("SELECT 1 FROM tanagra_acl WHERE user_id = :user_id AND asset_type = :asset_type AND asset_id = :asset_id", params, String.class);

        return (results != null);
    }
}
