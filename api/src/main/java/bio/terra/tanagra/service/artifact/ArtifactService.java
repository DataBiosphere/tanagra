package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.plugin.accesscontrol.Workspace;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ArtifactService {
  public Map<String, Workspace> searchWorkspaces(int pageSize, int page) {
    return searchWorkspaces("", pageSize, page);
  }

  public Map<String, Workspace> searchWorkspaces(String searchTerm, int pageSize, int page) {
    Workspace workspace = new Workspace("aaa");
    workspace.setName("aaa");

    HashMap<String, Workspace> workspaces = new HashMap<>();
    workspaces.put(workspace.getIdentifier(), workspace);

    return workspaces;
  }
}
