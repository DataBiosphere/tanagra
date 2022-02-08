package bio.terra.tanagra.workflow.utils;

import com.google.common.collect.Multimap;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.beam.sdk.values.KV;

public class KVUtils {
  private KVUtils() {}

  /** Convert a multimap to an equivalent list of KVs. */
  public static <T1, T2> List<KV<T1, T2>> convertToKvs(Multimap<T1, T2> multimap) {
    return multimap.entries().stream()
        .map(entry -> KV.of(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }
}
