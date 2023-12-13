package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.Literal;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SqlParams {
  private final Map<String, Literal> params = new HashMap<>();

  public String addParam(String paramBaseName, Literal paramValue) {
    String paramName;
    if (!params.containsKey(paramBaseName)) {
      // If there is no existing parameter with the same name, then just use the name as is.
      paramName = paramBaseName;
    } else {
      // Otherwise, generate a new name based on the requested name.
      paramName = generateNewParamName(paramBaseName);
    }
    params.put(paramName, paramValue);
    return paramName;
  }

  private String generateNewParamName(String paramBaseName) {
    Random randomNumberGenerator = new Random();
    final int maxTries = 20;
    for (int i = 0; i < maxTries; i++) {
      String paramName = paramBaseName + (isTest() ? i : randomNumberGenerator.nextInt(100));
      if (!params.containsKey(paramName)) {
        return paramName;
      }
    }
    throw new SystemException(
        "Failed to generate a unique parameter name in " + maxTries + "tries: " + paramBaseName);
  }

  public ImmutableMap<String, Literal> getParams() {
    return ImmutableMap.copyOf(params);
  }

  private boolean isTest() {
    return System.getProperty("IS_TEST") != null;
  }
}
