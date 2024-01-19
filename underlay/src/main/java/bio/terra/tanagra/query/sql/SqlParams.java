package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.SystemException;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

  private boolean isTest() {
    return System.getProperty("IS_TEST") != null;
  }
}
