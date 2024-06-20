package bio.terra.tanagra.api.query;

import bio.terra.tanagra.utils.JacksonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Nullable;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PageMarker {
  private static final Logger LOGGER = LoggerFactory.getLogger(PageMarker.class);
  private String pageToken;
  private Integer offset;
  private Instant instant;

  public PageMarker() {
    // Default constructor for Jackson deserialization.
  }

  private PageMarker(String pageToken, Integer offset, Instant instant) {
    this.pageToken = pageToken;
    this.offset = offset;
    this.instant = instant;
  }

  public static PageMarker forToken(String pageToken) {
    return new PageMarker(pageToken, null, null);
  }

  public static PageMarker forOffset(Integer offset) {
    return new PageMarker(null, offset, null);
  }

  public String getPageToken() {
    return pageToken;
  }

  public Integer getOffset() {
    return offset;
  }

  public Instant getInstant() {
    return instant;
  }

  public PageMarker instant(Instant instant) {
    this.instant = instant;
    return this;
  }

  public String serialize() {
    try {
      return JacksonMapper.serializeJavaObject(this);
    } catch (JsonProcessingException jpEx) {
      LOGGER.error("Error serializing page marker", jpEx);
      return null;
    }
  }

  public static PageMarker deserialize(@Nullable String jsonStr) {
    if (jsonStr == null) {
      return null;
    }
    try {
      return JacksonMapper.deserializeJavaObject(jsonStr, PageMarker.class);
    } catch (JsonProcessingException jpEx) {
      LOGGER.error("Error deserializing page marker", jpEx);
      return null;
    }
  }
}
