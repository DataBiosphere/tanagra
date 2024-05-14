package bio.terra.tanagra.service.accesscontrol;

/** Names of actions users can take on Tanagra resources. */
public enum Action {
  READ,
  CREATE,
  UPDATE,
  DELETE,
  CREATE_COHORT,
  CREATE_CONCEPT_SET,
  CREATE_REVIEW,
  CREATE_ANNOTATION_KEY
}
