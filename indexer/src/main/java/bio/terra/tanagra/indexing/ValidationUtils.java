package bio.terra.tanagra.indexing;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ValidationUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationUtils.class);

  private ValidationUtils() {}

  //  public static void validateRelationships(Underlay underlay) {
  //    // Check that there is only one unique definition of a relationship between 2 entities.
  //    // We could lift this restriction in the future, but it would require accompanying changes
  // to
  //    // the:
  //    //   - API e.g. to specify the entity group, not just the related entity when defining a
  //    // relationship filter
  //    //   - Indexing e.g. to generate tables with unique names. Currently, we use the entity
  // names.
  //    Map<String, List<String>> errorsForRelationship = new HashMap<>();
  //    Map<Set<Entity>, Relationship> relationshipMap = new HashMap<>();
  //    for (EntityGroup entityGroup : underlay.getEntityGroups()) {
  //      for (Relationship relationship : entityGroup.getRelationships()) {
  //        Set<Entity> relatedEntities = Set.of(relationship.getEntityA(),
  // relationship.getEntityB());
  //
  //        if (relationshipMap.containsKey(relatedEntities)) {
  //          String relatedEntitiesStr =
  //              relatedEntities.stream()
  //                  .map(Entity::getName)
  //                  .sorted()
  //                  .collect(Collectors.joining(","));
  //          if (relationshipMap.get(relatedEntities).isEquivalentTo(relationship)) {
  //            LOGGER.info(
  //                "Found another equivalent definition of the relationship between entities: {}",
  //                relatedEntitiesStr);
  //          } else {
  //            List<String> errorMsgs;
  //            if (errorsForRelationship.containsKey(relatedEntitiesStr)) {
  //              errorMsgs = errorsForRelationship.get(relatedEntitiesStr);
  //            } else {
  //              errorMsgs = new ArrayList<>();
  //              errorsForRelationship.put(relatedEntitiesStr, errorMsgs);
  //            }
  //            errorMsgs.add(entityGroup.getName());
  //            LOGGER.info(
  //                "Found a different definition of the relationship between entities: {} in entity
  // groups: {}",
  //                relatedEntitiesStr,
  //                errorMsgs.stream().collect(Collectors.joining(",")));
  //          }
  //        } else {
  //          relationshipMap.put(relatedEntities, relationship);
  //        }
  //      }
  //    }
  //
  //    // Output any error messages.
  //    if (errorsForRelationship.isEmpty()) {
  //      LOGGER.info("Validation of entity relationships succeeded");
  //    } else {
  //      errorsForRelationship.keySet().stream()
  //          .sorted()
  //          .forEach(
  //              relatedEntitiesStr -> {
  //                LOGGER.warn(
  //                    "Found >1 definition of the relationshp between entities: {} in entity
  // groups: {}",
  //                    relatedEntitiesStr,
  //                    errorsForRelationship.get(relatedEntitiesStr).stream()
  //                        .collect(Collectors.joining(",")));
  //              });
  //      throw new InvalidConfigException("Validation of entity relationships had errors");
  //    }
  //  }
  //
  //  public static void validateAttributes(Underlay underlay) {
  //    // Check that the attribute data types are all defined and match the expected.
  //    Map<String, List<String>> errorsForEntity = new HashMap<>();
  //    underlay.getEntities().stream()
  //        .sorted(Comparator.comparing(Entity::getName))
  //        .forEach(
  //            entity -> {
  //              List<String> errors = new ArrayList<>();
  //              entity.getAttributes().stream()
  //                  .sorted(Comparator.comparing(Attribute::getName))
  //                  .forEach(
  //                      attribute -> {
  //                        LOGGER.info(
  //                            "Validating data type for entity {}, attribute {}",
  //                            entity.getName(),
  //                            attribute.getName());
  //                        Literal.DataType computedDataType =
  //                            attribute
  //                                .getSourceValueField()
  //                                .getTablePointer()
  //                                .getDataPointer()
  //                                .lookupDatatype(attribute.getSourceValueField());
  //                        if (attribute.getDataType() == null
  //                            || !attribute.getDataType().equals(computedDataType)) {
  //                          String msg =
  //                              "attribute: "
  //                                  + attribute.getName()
  //                                  + ", expected data type: "
  //                                  + computedDataType
  //                                  + ", actual data type: "
  //                                  + attribute.getDataType();
  //                          errors.add(msg);
  //                          LOGGER.info("entity: {}, {}", entity.getName(), msg);
  //                        }
  //                      });
  //              if (!errors.isEmpty()) {
  //                errorsForEntity.put(entity.getName(), errors);
  //              }
  //            });
  //
  //    // Output any error messages.
  //    if (errorsForEntity.isEmpty()) {
  //      LOGGER.info("Validation of attribute data types succeeded");
  //    } else {
  //      errorsForEntity.keySet().stream()
  //          .sorted()
  //          .forEach(
  //              entityName -> {
  //                LOGGER.warn("Validation of attribute data types for entity {} failed",
  // entityName);
  //                errorsForEntity.get(entityName).stream().forEach(msg -> LOGGER.warn(msg));
  //              });
  //      throw new InvalidConfigException("Validation attribute data types had errors");
  //    }
  //  }
}
