package bio.terra.tanagra.service.admin;

import bio.terra.tanagra.generated.model.ApiWorkspace;
import bio.terra.tanagra.plugin.PluginService;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.Workspace;
import bio.terra.tanagra.service.artifact.ArtifactService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceService {
  private static final int DEFAULT_PAGE_SIZE = 25;

  private final ArtifactService artifactService;
  private final IAccessControlPlugin accessControlPlugin;

  @Autowired
  public WorkspaceService(ArtifactService artifactService, PluginService pluginService) {
    this.artifactService = artifactService;
    this.accessControlPlugin = pluginService.getPlugin(IAccessControlPlugin.class);
  }

  public List<Workspace> search(String searchTerm, Integer pageSize, Integer page) {
    int brandNewPageSizeToMakePmdHappy = pageSize == 0 ? DEFAULT_PAGE_SIZE : pageSize;

    List<Workspace> workspaces =
        artifactService.searchWorkspaces(searchTerm, brandNewPageSizeToMakePmdHappy, page);
    accessControlPlugin.hydrate(workspaces);

    return workspaces;
  }

  public ApiWorkspace toApiObject(Workspace workspace) {
    ApiWorkspace apiWorkspace = new ApiWorkspace();
    apiWorkspace.setIdentifier(workspace.getIdentifier());
    apiWorkspace.setName(workspace.getName());

    return apiWorkspace;
  }

  public List<ApiWorkspace> toApiList(List<Workspace> workspaces) {
    return workspaces.stream().map(this::toApiObject).collect(Collectors.toList());
  }
}
