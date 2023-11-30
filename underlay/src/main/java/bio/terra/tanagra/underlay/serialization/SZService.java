package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;

@AnnotatedClass(
    name = "SZService",
    markdown =
        "Service configuration.\n\n"
            + "Define a version of this file for each place you will deploy the service. If you share the "
            + "same index dataset across multiple service deployments, you need a separate configuration for each.")
public class SZService {
  @AnnotatedField(
      name = "SZService.underlay",
      markdown =
          "Name of the underlay to make available in the service deployment.\n\n"
              + "If a single deployment serves multiple underlays, you need a separate configuration for each. "
              + "Name is specified in the underlay file, and also matches the name of the config/underlay "
              + "sub-directory in the underlay sub-project resources.",
      exampleValue = "cmssynpuf")
  public String underlay;

  @AnnotatedField(
      name = "SZService.bigQuery",
      markdown = "Pointers to the source and index BigQuery datasets.")
  public SZBigQuery bigQuery;
}
