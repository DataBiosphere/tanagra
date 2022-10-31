package bio.terra.tanagra.service.admin;

import bio.terra.tanagra.generated.model.ApiWorkspace;
import bio.terra.tanagra.plugin.PluginService;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.Workspace;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceService {
  private final PluginService pluginService;

  @Autowired
  public WorkspaceService(PluginService pluginService) {
    this.pluginService = pluginService;
  }

  public List<Workspace> search() {
    pluginService.getPlugin(IAccessControlPlugin.class);

    Workspace workspace = new Workspace("aaa");
    workspace.setName("aaa");

    List<Workspace> workspaces = new ArrayList<>();
    workspaces.add(workspace);

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
