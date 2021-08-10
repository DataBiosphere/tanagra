package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** A service to make {@link Underlay}s available. */
@Service
public class UnderlayService {
  private final ImmutableMap<String, Underlay> underlays;

  @Autowired
  public UnderlayService(UnderlayConfiguration underlayConfiguration) {
    List<bio.terra.tanagra.proto.underlay.Underlay> underlayProtos;
    try {
      underlayProtos = underlayConfiguration.getUnderlays();
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read underlays.", e);
    }
    this.underlays =
        Maps.uniqueIndex(
            underlayProtos.stream().map(UnderlayConversion::convert).iterator(), Underlay::name);
  }

  /** Returns the underlay with the given name, if there is one. */
  public Optional<Underlay> getUnderlay(String name) {
    return Optional.ofNullable(underlays.get(name));
  }
}
