package bio.terra.tanagra.service.admin;

import bio.terra.tanagra.generated.model.ApiWorkspace;
import bio.terra.tanagra.plugin.PluginService;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.Workspace;
import bio.terra.tanagra.plugin.identity.IIdentityPlugin;
import bio.terra.tanagra.service.artifact.ArtifactService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceService {
  private static final int DEFAULT_PAGE_SIZE = 25;

  private final ArtifactService artifactService;
  private final IAccessControlPlugin accessControlPlugin;
  private final IIdentityPlugin identityPlugin;

  @Autowired
  public WorkspaceService(ArtifactService artifactService, PluginService pluginService) {
    this.artifactService = artifactService;
    this.accessControlPlugin = pluginService.getPlugin(IAccessControlPlugin.class);
    this.identityPlugin = pluginService.getPlugin(IIdentityPlugin.class);
  }

  public Map<String, Workspace> search(String searchTerm, Integer pageSize, Integer page) {
    int brandNewPageSizeToMakePmdHappy = pageSize == 0 ? DEFAULT_PAGE_SIZE : pageSize;

    Map<String, Workspace> workspaces =
        artifactService.searchWorkspaces(searchTerm, brandNewPageSizeToMakePmdHappy, page);
    accessControlPlugin.hydrate(workspaces);

    workspaces.values().forEach(workspace -> identityPlugin.hydrate(workspace.getMembers()));

    return workspaces;
  }

  public List<ApiWorkspace> toApiList(List<Workspace> workspaces) {
    return workspaces.stream().map(Workspace::toApiObject).collect(Collectors.toList());
  }

  public List<ApiWorkspace> toApiList(Map<String, Workspace> workspaces) {
    return workspaces.values().stream().map(Workspace::toApiObject).collect(Collectors.toList());
  }
}
