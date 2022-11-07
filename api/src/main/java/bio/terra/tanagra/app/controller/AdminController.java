package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.AdminApi;
import bio.terra.tanagra.generated.model.ApiListWorkspacesResponse;
import bio.terra.tanagra.generated.model.ApiWorkspace;
import bio.terra.tanagra.plugin.accesscontrol.Workspace;
import bio.terra.tanagra.service.admin.WorkspaceService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class AdminController implements AdminApi {
  private final WorkspaceService workspaceService;

  @Autowired
  public AdminController(WorkspaceService workspaceService) {
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<ApiListWorkspacesResponse> listWorkspaces(Integer pageSize, Integer page) {
    return searchWorkspaces("", pageSize, page);
  }

  @Override
  public ResponseEntity<ApiListWorkspacesResponse> searchWorkspaces(
      String searchTerm, Integer pageSize, Integer page) {
    Map<String, Workspace> workspaces = workspaceService.search(searchTerm, pageSize, page);
    List<ApiWorkspace> apiWorkspaces = workspaceService.toApiList(workspaces);

    return ResponseEntity.ok(new ApiListWorkspacesResponse().entities(apiWorkspaces));
  }
}
