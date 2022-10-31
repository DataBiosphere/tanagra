package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.AdminApi;
import bio.terra.tanagra.generated.model.ApiListWorkspacesResponse;
import bio.terra.tanagra.plugin.PluginService;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class AdminController implements AdminApi {
  private final PluginService pluginService;

  @Autowired
  public AdminController(PluginService pluginService) {
    this.pluginService = pluginService;
  }

  public void stubEndpoint() {
    this.pluginService.getPlugin(IAccessControlPlugin.class).grantAccess(null, null);
  }

  @Override
  public ResponseEntity<ApiListWorkspacesResponse> listWorkspaces(
      Integer pageSize, String pageToken) {
    return AdminApi.super.listWorkspaces(pageSize, pageToken);
  }
}
