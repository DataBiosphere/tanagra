package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

/** Value class to hold all the context that's needed to evaluate a search. */
@AutoValue
public abstract class SearchContext {
  public abstract UnderlaySqlResolver underlaySqlResolver();

  public static Builder builder() {
    return new AutoValue_SearchContext.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder underlaySqlResolver(UnderlaySqlResolver underlaySqlResolver);

    public abstract SearchContext build();
  }
}
