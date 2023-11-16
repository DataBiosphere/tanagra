package bio.terra.tanagra.underlay.serialization;

import java.util.Map;
import java.util.Set;

/**
 * <p>Underlay configuration.</p>
 * <p>Define a version of this file for each dataset.
 * If you index and/or serve a dataset in multiple places or deployments, you only need one version of this file.</p>
 */
public class SZUnderlay {
  /**
   * <p>Name of the underlay.</p>
   * <p>This is the unique identifier for the underlay.
   * If you serve multiple underlays in a single service deployment, the underlay names cannot overlap.</p>
   * <p>Name may not include spaces or special characters, only letters and numbers.</p>
   */
  public String name;

  /**
   * <p>Name of the primary entity.</p>
   * <p>A cohort contains instances of the primary entity (e.g. persons).</p>
   */
  public String primaryEntity;

  /**
   * <p>List of paths of all the entities.</p>
   * <p>An entity is any object that the UI might show a list of (e.g. list of persons, conditions, condition occurrences). The list must include the primary entity.</p>
   * <p>Path consists of two parts: [Data-Mapping Group]/[Entity Name] (e.g. omop/condition).</p>
   * <p>[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory in the underlay sub-project resources (e.g. omop).</p>
   * <p>[Entity Name] is specified in the entity file, and also matches the name of the sub-directory of the config/datamapping/[Data-Mapping Group]/entity sub-directory in the underlay sub-project resources (e.g. condition).</p>
   * <p>Using the path here instead of just the entity name allows us to share entity definitions across underlays.
   * For example, the <code>omop</code> data-mapping group contains template entity definitions for standing up a new underlay.</p>
   */
  public Set<String> entities;

  /**
   * <p>List of paths of <code>group-items</code> type entity groups.</p>
   * <p>A <code>group-items</code> type entity group defines a relationship between two entities.</p>
   * <p>Path consists of two parts: [Data-Mapping Group]/[Entity Group Name] (e.g. omop/brandIngredient).</p>
   * <p>[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory in the underlay sub-project resources (e.g. omop).</p>
   * <p>[Entity Group Name] is specified in the entity group file, and also matches the name of the sub-directory of the config/datamapping/[Data-Mapping Group]/entitygroup sub-directory in the underlay sub-project resources (e.g. brandIngredient).</p>
   * <p>Using the path here instead of just the entity group name allows us to share entity group definitions across underlays.
   * For example, the <code>omop</code> data-mapping group contains template entity group definitions for standing up a new underlay.</p>
   */
  public Set<String> groupItemsEntityGroups;

  /**
   * <p>List of paths of <code>criteria-occurrence</code> type entity groups.</p>
   * <p>A <code>criteria-occurrence</code> type entity group defines a relationship between three entities.</p>
   * <p>Path consists of two parts: [Data-Mapping Group]/[Entity Group Name] (e.g. omop/conditionPerson).</p>
   * <p>[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory in the underlay sub-project resources (e.g. omop).</p>
   * <p>[Entity Group Name] is specified in the entity group file, and also matches the name of the sub-directory of the config/datamapping/[Data-Mapping Group]/entitygroup sub-directory in the underlay sub-project resources (e.g. conditionPerson).</p>
   * <p>Using the path here instead of just the entity group name allows us to share entity group definitions across underlays.
   * For example, the <code>omop</code> data-mapping group contains template entity group definitions for standing up a new underlay.</p>
   */
  public Set<String> criteriaOccurrenceEntityGroups;

  /**
   * <p>Metadata for the underlay.</p>
   */
  public Metadata metadata;

  /**
   * <p>Name of the UI config file.</p>
   * <p>File must be in the same directory as the underlay file. Name includes file extension (e.g. ui.json).</p>
   */
  // TODO: Merge UI config into backend config.
  public String uiConfigFile;

  /**
   * <p>Metadata for the underlay.</p>
   * <p>Information in this object is not used in the operation of the indexer or service, it is for display purposes only.</p>
   */
  public static class Metadata {
    /**
     * <p>Display name for the underlay.</p>
     * <p>Unlike the underlay {@link bio.terra.tanagra.underlay.serialization.SZUnderlay#name}, it may include spaces and special characters.</p>
     */
    public String displayName;

    /**
     * <p><strong>(optional)</strong> Description of the underlay.</p>
     */
    public String description;

    /**
     * <p><strong>(optional)</strong> Key-value map of underlay properties.</p>
     * <p>Keys may not include spaces or special characters, only letters and numbers.</p>
     */
    // TODO: Pass these to the access control and export implementation classes.
    public Map<String, String> properties;
  }
}
