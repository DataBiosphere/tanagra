package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

// DO NOT SUBMIT comment me
@AutoValue
public abstract class AttributeVariable {
  public abstract Attribute attribute();

  /** The variable binding the entity of the attribute. */
  public abstract Variable variable();

  /** The {@link EntityVariable} that this attribute is a part of. */
  public EntityVariable entityVariable() {
    return EntityVariable.create(attribute().entity(), variable());
  }

  public static AttributeVariable create(Attribute attribute, Variable variable) {
    return new AutoValue_AttributeVariable(attribute, variable);
  }
}
