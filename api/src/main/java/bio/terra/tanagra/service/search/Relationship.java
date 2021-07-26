package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Relationship {
  public abstract String name();

  public abstract Attribute role1();

  public abstract Attribute role2();
}
