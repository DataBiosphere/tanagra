package bio.terra.tanagra.api.query;

import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.DisplayHint;
import java.util.Collections;
import java.util.Map;

public class EntityHintResult {
  private final String sql;
  private final Map<Attribute, DisplayHint> hintMap;

  public EntityHintResult(String sql, Map<Attribute, DisplayHint> hintMap) {
    this.sql = sql;
    this.hintMap = hintMap;
  }

  public String getSql() {
    return sql;
  }

  public Map<Attribute, DisplayHint> getHintMap() {
    return Collections.unmodifiableMap(hintMap);
  }

  public DisplayHint getHint(Attribute attribute) {
    return hintMap.get(attribute);
  }

  public boolean hasHint(Attribute attribute) {
    return hintMap.containsKey(attribute);
  }
}
