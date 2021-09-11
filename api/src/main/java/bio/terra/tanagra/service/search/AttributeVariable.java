package bio.terra.tanagra.service.search;

import bio.terra.tanagra.model.Attribute;
import com.google.auto.value.AutoValue;

/**
 * An {@link Attribute} for a bound {@link Variable}. The entity bound by the variable should match
 * the attribute's entity.
 *
 * <p>e.g. "x.attribute" in "SELECT x.attribute FROM entity as x".
 */
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
