package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.api.shared.Literal;
import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SqlParams {
  private final Map<String, Literal> params = new HashMap<>();

  public String addParam(String paramBaseName, Literal paramValue) {
    String paramName = paramBaseName + params.size();
    params.put(paramName, paramValue);
    return paramName;
  }

  public ImmutableMap<String, Literal> getParams() {
    return ImmutableMap.copyOf(params);
  }

  public Literal getParamValue(String paramName) {
    return params.get(paramName);
  }

  public List<String> getParamNamesLongestFirst() {
    return params.keySet().stream()
        .sorted(Comparator.comparing(String::length).reversed())
        .collect(Collectors.toList());
  }
}
