package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

// DO NOT SUBMIT comment are we using this?
@AutoValue
public abstract class Variable {
  public abstract String name();

  public static Variable create(String name) {
    return new AutoValue_Variable(name);
  }
}
