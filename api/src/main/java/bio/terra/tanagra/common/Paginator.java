package bio.terra.tanagra.common;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.proto.utils.pagination.PageSizeOffset;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Base64;
import java.util.List;
import javax.annotation.Nullable;

/** Implements simple pagination for sorted lists in memory. */
public final class Paginator<T> {
  private Paginator() {}

  /**
   * Returns a {@link Page} for the results.
   *
   * @param allResults the sorted list of all results being paginated.
   * @param pageSize the maximumn number of results to have per page
   * @param pageToken the token of the page to retrieve. If null or empty, the first page is
   *     retrieved.
   */
  public static <T> Page<T> getPage(List<T> allResults, int pageSize, @Nullable String pageToken) {
    Preconditions.checkArgument(pageSize > 0, "pageSize must be greater than 0 to paginate.");
    if (pageToken == null || pageToken.isEmpty()) {
      return getPageInternal(
          allResults, PageSizeOffset.newBuilder().setOffest(0).setPageSize(pageSize).build());
    }
    PageSizeOffset pageSizeOffset = decode(pageToken);
    if (pageSizeOffset.getPageSize() != pageSize) {
      throw new BadRequestException(
          String.format(
              "Token page size '%d' does not match input page size '%d'.",
              pageSizeOffset.getPageSize(), pageSize));
    }
    return getPageInternal(allResults, decode(pageToken));
  }

  public static <T> Page<T> getPageInternal(List<T> allResults, PageSizeOffset pageSizeOffset) {
    Preconditions.checkArgument(
        pageSizeOffset.getOffest() < allResults.size() || allResults.isEmpty(),
        "page offset out of bounds.");
    int offset = pageSizeOffset.getOffest();
    int pageLimit = pageSizeOffset.getOffest() + pageSizeOffset.getPageSize();
    if (pageLimit >= allResults.size()) {
      List<T> results = allResults.subList(offset, allResults.size());
      return Page.createLastPage(results);
    }
    PageSizeOffset nextPageSizeOffset = pageSizeOffset.toBuilder().setOffest(pageLimit).build();
    List<T> results = allResults.subList(offset, pageLimit);
    return Page.create(results, encode(nextPageSizeOffset));
  }

  private static String encode(PageSizeOffset pageSizeOffset) {
    return Base64.getEncoder().encodeToString(pageSizeOffset.toByteArray());
  }

  private static PageSizeOffset decode(String pageToken) {
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

    public static <T> Page<T> createLastPage(List<T> results) {
      return create(results, "");
    }
  }
}
