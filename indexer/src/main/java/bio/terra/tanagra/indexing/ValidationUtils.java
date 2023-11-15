package bio.terra.tanagra.indexing;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ValidationUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationUtils.class);

  private ValidationUtils() {}

  public static void validateRelationships(Underlay underlay) {
    // Check that there is only one unique definition of a relationship between 2 entities.
    // We could lift this restriction in the future, but it would require accompanying changes to
    // the:
    //   - API e.g. to specify the entity group, not just the related entity when defining a
    // relationship filter
    //   - Indexing e.g. to generate tables with unique names. Currently, we use the entity names.
    Map<String, List<String>> errorsForRelationship = new HashMap<>();
    Map<Set<Entity>, Relationship> relationshipMap = new HashMap<>();
    for (EntityGroup entityGroup : underlay.getEntityGroups()) {
      for (Relationship relationship : entityGroup.getRelationships()) {
        Set<Entity> relatedEntities = Set.of(relationship.getEntityA(), relationship.getEntityB());

        if (relationshipMap.containsKey(relatedEntities)) {
          String relatedEntitiesStr =
              relatedEntities.stream()
                  .map(Entity::getName)
                  .sorted()
                  .collect(Collectors.joining(","));
          if (relationshipMap.get(relatedEntities).equals(relationship)) {
            LOGGER.info(
                "Found another equivalent definition of the relationship between entities: {}",
                relatedEntitiesStr);
          } else {
            List<String> errorMsgs;
            if (errorsForRelationship.containsKey(relatedEntitiesStr)) {
              errorMsgs = errorsForRelationship.get(relatedEntitiesStr);
            } else {
              errorMsgs = new ArrayList<>();
              errorsForRelationship.put(relatedEntitiesStr, errorMsgs);
            }
            errorMsgs.add(entityGroup.getName());
            LOGGER.info(
                "Found a different definition of the relationship between entities: {} in entity groups: {}",
                relatedEntitiesStr,
                errorMsgs.stream().collect(Collectors.joining(",")));
          }
        } else {
          relationshipMap.put(relatedEntities, relationship);
        }
      }
    }

    // Output any error messages.
    if (errorsForRelationship.isEmpty()) {
      LOGGER.info("Validation of entity relationships succeeded");
    } else {
      errorsForRelationship.keySet().stream()
          .sorted()
          .forEach(
              relatedEntitiesStr -> {
                LOGGER.warn(
                    "Found >1 definition of the relationshp between entities: {} in entity groups: {}",
                    relatedEntitiesStr,
                    errorsForRelationship.get(relatedEntitiesStr).stream()
                        .collect(Collectors.joining(",")));
              });
      throw new InvalidConfigException("Validation of entity relationships had errors");
    }
  }
}
