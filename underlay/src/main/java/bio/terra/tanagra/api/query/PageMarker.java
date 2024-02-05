package bio.terra.tanagra.api.query;

import bio.terra.tanagra.utils.JacksonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PageMarker {
  private static final Logger LOGGER = LoggerFactory.getLogger(PageMarker.class);
  private String pageToken;
  private Integer offset;

  public PageMarker() {
    // Default constructor for Jackson deserialization.
  }

  private PageMarker(String pageToken, Integer offset) {
    this.pageToken = pageToken;
    this.offset = offset;
  }

  public static PageMarker forToken(String pageToken) {
    return new PageMarker(pageToken, null);
  }

  public static PageMarker forOffset(Integer offset) {
    return new PageMarker(null, offset);
  }

  public String getPageToken() {
    return pageToken;
  }

  public Integer getOffset() {
    return offset;
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
