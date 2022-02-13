package bio.terra.tanagra.service.search;

import bio.terra.tanagra.service.search.utils.RandomNumberGenerator;
import bio.terra.tanagra.service.underlay.Underlay;
import com.google.auto.value.AutoValue;

/** Value class to hold all the context that's needed to evaluate a search. */
@AutoValue
public abstract class SearchContext {
  public abstract Underlay underlay();

  public abstract RandomNumberGenerator randomNumberGenerator();

  public static Builder builder() {
    return new AutoValue_SearchContext.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder underlay(Underlay underlay);

    public abstract Builder randomNumberGenerator(RandomNumberGenerator randomNumberGenerator);

    public abstract SearchContext build();
  }
}
