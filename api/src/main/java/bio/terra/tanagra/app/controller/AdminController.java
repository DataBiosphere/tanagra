package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.plugin.PluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class AdminController {
  private final PluginService pluginService;

  @Autowired
  public AdminController(PluginService pluginService) {
    this.pluginService = pluginService;
  }
}
