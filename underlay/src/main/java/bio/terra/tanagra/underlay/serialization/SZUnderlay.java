package bio.terra.tanagra.underlay.serialization;

import java.util.Map;
import java.util.Set;

/**
 * Underlay configuration.
 *
 * <p>Define a version of this file for each dataset. If you index and/or serve a dataset in
 * multiple places or deployments, you only need one version of this file.
 */
public class SZUnderlay {
  /**
   * Name of the underlay.
   *
   * <p>This is the unique identifier for the underlay. If you serve multiple underlays in a single
   * service deployment, the underlay names cannot overlap.
   *
   * <p>Name may not include spaces or special characters, only letters and numbers.
   */
  public String name;

  /**
   * Name of the primary entity.
   *
   * <p>A cohort contains instances of the primary entity (e.g. persons).
   */
  public String primaryEntity;

  /**
   * List of paths of all the entities.
   *
   * <p>An entity is any object that the UI might show a list of (e.g. list of persons, conditions,
   * condition occurrences). The list must include the primary entity.
   *
   * <p>Path consists of two parts: [Data-Mapping Group]/[Entity Name] (e.g. omop/condition).
   *
   * <p>[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory
   * in the underlay sub-project resources (e.g. omop).
   *
   * <p>[Entity Name] is specified in the entity file, and also matches the name of the
   * sub-directory of the config/datamapping/[Data-Mapping Group]/entity sub-directory in the
   * underlay sub-project resources (e.g. condition).
   *
   * <p>Using the path here instead of just the entity name allows us to share entity definitions
   * across underlays. For example, the <code>omop</code> data-mapping group contains template
   * entity definitions for standing up a new underlay.
   */
  public Set<String> entities;

  /**
   * List of paths of <code>group-items</code> type entity groups.
   *
   * <p>A <code>group-items</code> type entity group defines a relationship between two entities.
   *
   * <p>Path consists of two parts: [Data-Mapping Group]/[Entity Group Name] (e.g.
   * omop/brandIngredient).
   *
   * <p>[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory
   * in the underlay sub-project resources (e.g. omop).
   *
   * <p>[Entity Group Name] is specified in the entity group file, and also matches the name of the
   * sub-directory of the config/datamapping/[Data-Mapping Group]/entitygroup sub-directory in the
   * underlay sub-project resources (e.g. brandIngredient).
   *
   * <p>Using the path here instead of just the entity group name allows us to share entity group
   * definitions across underlays. For example, the <code>omop</code> data-mapping group contains
   * template entity group definitions for standing up a new underlay.
   */
  public Set<String> groupItemsEntityGroups;

  /**
   * List of paths of <code>criteria-occurrence</code> type entity groups.
   *
   * <p>A <code>criteria-occurrence</code> type entity group defines a relationship between three
   * entities.
   *
   * <p>Path consists of two parts: [Data-Mapping Group]/[Entity Group Name] (e.g.
   * omop/conditionPerson).
   *
   * <p>[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory
   * in the underlay sub-project resources (e.g. omop).
   *
   * <p>[Entity Group Name] is specified in the entity group file, and also matches the name of the
   * sub-directory of the config/datamapping/[Data-Mapping Group]/entitygroup sub-directory in the
   * underlay sub-project resources (e.g. conditionPerson).
   *
   * <p>Using the path here instead of just the entity group name allows us to share entity group
   * definitions across underlays. For example, the <code>omop</code> data-mapping group contains
   * template entity group definitions for standing up a new underlay.
   */
  public Set<String> criteriaOccurrenceEntityGroups;

  /** Metadata for the underlay. */
  public Metadata metadata;

  /**
   * Name of the UI config file.
   *
   * <p>File must be in the same directory as the underlay file. Name includes file extension (e.g.
   * ui.json).
   */
  // TODO: Merge UI config into backend config.
  public String uiConfigFile;

  /**
   * Metadata for the underlay.
   *
   * <p>Information in this object is not used in the operation of the indexer or service, it is for
   * display purposes only.
   */
  public static class Metadata {
    /**
     * Display name for the underlay.
     *
     * <p>Unlike the underlay {@link bio.terra.tanagra.underlay.serialization.SZUnderlay#name}, it
     * may include spaces and special characters.
     */
    public String displayName;

    /** <strong>(optional)</strong> Description of the underlay. */
    public String description;

    /**
     * <strong>(optional)</strong> Key-value map of underlay properties.
     *
     * <p>Keys may not include spaces or special characters, only letters and numbers.
     */
    // TODO: Pass these to the access control and export implementation classes.
    public Map<String, String> properties;
  }
}
