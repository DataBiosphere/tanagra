package bio.terra.tanagra.underlay.serialization;

/**
 * <p>Service configuration.</p>
 * <p>Define a version of this file for each place you will deploy the service.
 * If you share the same index dataset across multiple service deployments, you need a separate configuration for each.</p>
 */
public class SZService {
  /**
   * <p>Name of the underlay to make available in the service deployment.</p>
   * <p>If a single deployment serves multiple underlays, you need a separate configuration for each.</p>
   * <p>Name is specified in the underlay file, and also matches the name of the config/underlay sub-directory in the underlay sub-project resources (e.g. cmssynpuf).</p>
   */
  public String underlay;

  /**
   * <p>Pointers to the source and index BigQuery datasets.</p>
   */
  public SZBigQuery bigQuery;
}
