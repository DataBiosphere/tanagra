package bio.terra.tanagra.underlay.serialization;

/**
 * Service configuration.
 *
 * <p>Define a version of this file for each place you will deploy the service. If you share the
 * same index dataset across multiple service deployments, you need a separate configuration for
 * each.
 */
public class SZService {
  /**
   * Name of the underlay to make available in the service deployment.
   *
   * <p>If a single deployment serves multiple underlays, you need a separate configuration for
   * each.
   *
   * <p>Name is specified in the underlay file, and also matches the name of the config/underlay
   * sub-directory in the underlay sub-project resources (e.g. cmssynpuf).
   */
  public String underlay;

  /** Pointers to the source and index BigQuery datasets. */
  public SZBigQuery bigQuery;
}
