package bio.terra.tanagra.common;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.proto.utils.pagination.PageSizeOffset;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Base64;
import java.util.List;
import javax.annotation.Nullable;

/** Implements simple pagination for sorted lists in memory. */
public final class Paginator<T> {
  /** The maximum number of results to include per page. */
  private final int pageSize;
  /**
   * Hash of non pageSize/pageToken parameters in the request to ensure that the query is consistent
   * with the page token.
   */
  private final String parameterHash;

  public Paginator(int pageSize, String parameterHash) {
    Preconditions.checkArgument(pageSize > 0, "pageSize must be greater than 0 to paginate.");
    this.pageSize = pageSize;
    this.parameterHash = parameterHash;
  }

  /**
   * Returns a {@link Page} for the results.
   *
   * @param allResults the sorted list of all results being paginated.
   * @param pageToken the token of the page to retrieve. If null or empty, the first page is
   *     retrieved.
   * @throws BadRequestException if the parsed pageToken has a page size or parameters hash
   *     different than what's expected.
   */
  public <T> Page<T> getPage(List<T> allResults, @Nullable String pageToken) {
    if (pageToken == null || pageToken.isEmpty()) {
      return getPageInternal(
          allResults,
          PageSizeOffset.newBuilder()
              .setOffset(0)
              .setPageSize(pageSize)
              .setParameterHash(parameterHash)
              .build());
    }
    PageSizeOffset pageSizeOffset = decode(pageToken);
    validateToken(pageSizeOffset);
    return getPageInternal(allResults, decode(pageToken));
  }

  private void validateToken(PageSizeOffset pageSizeOffset) {
    if (pageSizeOffset.getPageSize() != pageSize) {
      throw new BadRequestException(
          String.format(
              "Toke page size '%d' does not match input page size '%d'. Only use page tokens "
                  + "with the query parameters used to create the page token.",
              pageSizeOffset.getPageSize(), pageSize));
    }
    if (!pageSizeOffset.getParameterHash().equals(parameterHash)) {
      throw new BadRequestException(
          "Page token hash query parameters. Only use page tokens with the query parameters used "
              + "to create the page token.");
    }
  }

  public static <T> Page<T> getPageInternal(List<T> allResults, PageSizeOffset pageSizeOffset) {
    Preconditions.checkArgument(
        pageSizeOffset.getOffset() < allResults.size() || pageSizeOffset.getOffset() == 0,
        "page offset out of bounds.");
    int offset = pageSizeOffset.getOffset();
    int pageLimit = pageSizeOffset.getOffset() + pageSizeOffset.getPageSize();
    if (pageLimit >= allResults.size()) {
      List<T> results = allResults.subList(offset, allResults.size());
      // This page includes the last of the results. Use an empty string for the next page token.
      return Page.create(results, "");
    }
    PageSizeOffset nextPageSizeOffset = pageSizeOffset.toBuilder().setOffset(pageLimit).build();
    List<T> results = allResults.subList(offset, pageLimit);
    return Page.create(results, encode(nextPageSizeOffset));
  }

  @VisibleForTesting
  static String encode(PageSizeOffset pageSizeOffset) {
    return Base64.getEncoder().encodeToString(pageSizeOffset.toByteArray());
  }

  @VisibleForTesting
  static PageSizeOffset decode(String pageToken) {
    try {
      return PageSizeOffset.parseFrom(Base64.getDecoder().decode(pageToken));
    } catch (InvalidProtocolBufferException e) {
      throw new BadRequestException(String.format("Unable to decode pageToken '%s'", pageToken), e);
    }
  }

  /** A page of results and the token for the next page of results. */
  @AutoValue
  public abstract static class Page<T> {
    /** The results of this page. */
    public abstract ImmutableList<T> results();

    /** Token to retrieve the next page of results. If empty, there are no more results. */
    public abstract String nextPageToken();

    public static <T> Page<T> create(List<T> results, String nextPageToken) {
      return new AutoValue_Paginator_Page<>(ImmutableList.copyOf(results), nextPageToken);
    }
  }
}
