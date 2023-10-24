package bio.terra.tanagra.api2.query.hint;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class HintQueryResult {
  private final String sql;
  private final ImmutableList<HintInstance> hintInstances;

  public HintQueryResult(String sql, List<HintInstance> hintInstances) {
    this.sql = sql;
    this.hintInstances = ImmutableList.copyOf(hintInstances);
  }

  public String getSql() {
    return sql;
  }

  public ImmutableList<HintInstance> getHintInstances() {
    return hintInstances;
  }
}
