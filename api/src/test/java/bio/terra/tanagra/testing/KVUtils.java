package bio.terra.tanagra.testing;

import com.google.common.collect.Multimap;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.beam.sdk.values.KV;

public final class KVUtils {
  private KVUtils() {}

  /** Convert a multimap to an equivalent list of KVs. */
  public static <A, B> List<KV<A, B>> convertToKvs(Multimap<A, B> multimap) {
    return multimap.entries().stream()
        .map(entry -> KV.of(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }
}
