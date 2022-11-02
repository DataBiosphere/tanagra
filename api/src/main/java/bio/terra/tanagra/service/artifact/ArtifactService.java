package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.plugin.accesscontrol.Workspace;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ArtifactService {
  public List<Workspace> searchWorkspaces(int pageSize, int page) {
    return searchWorkspaces("", pageSize, page);
  }

  public List<Workspace> searchWorkspaces(String searchTerm, int pageSize, int page) {
    Workspace workspace = new Workspace("aaa");
    workspace.setName("aaa");

    List<Workspace> workspaces = new ArrayList<>();
    workspaces.add(workspace);

    return workspaces;
  }
}
