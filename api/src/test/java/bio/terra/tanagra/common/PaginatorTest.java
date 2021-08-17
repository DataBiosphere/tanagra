package bio.terra.tanagra.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.common.Paginator.Page;
import bio.terra.tanagra.proto.utils.pagination.PageSizeOffset;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class PaginatorTest {

  @Test
  void getPageSizeLessThanResults() {
    List<Integer> allResults = IntStream.range(0, 8).boxed().collect(Collectors.toList());
    Paginator<Integer> paginator = new Paginator<>(3, "foo");
    Page<Integer> page0 = paginator.getPage(allResults, null);
    Page<Integer> page1 = paginator.getPage(allResults, page0.nextPageToken());
    Page<Integer> page2 = paginator.getPage(allResults, page1.nextPageToken());
    assertEquals(ImmutableList.of(0, 1, 2), page0.results());
    assertEquals(ImmutableList.of(3, 4, 5), page1.results());
    assertEquals(ImmutableList.of(6, 7), page2.results());
    assertEquals("", page2.nextPageToken());
  }

  @Test
  void getPageSizeMultipleOfResults() {
    List<Integer> allResults = IntStream.range(0, 4).boxed().collect(Collectors.toList());
    Paginator<Integer> paginator = new Paginator<>(2, "foo");
    Page<Integer> page0 = paginator.getPage(allResults, null);
    Page<Integer> page1 = paginator.getPage(allResults, page0.nextPageToken());
    assertEquals(ImmutableList.of(0, 1), page0.results());
    assertEquals(ImmutableList.of(2, 3), page1.results());
    assertEquals("", page1.nextPageToken());
  }

  @Test
  void getPageSizeGreaterThanResults() {
    List<Integer> allResults = ImmutableList.of(0, 1, 2);
    assertEquals(Page.create(allResults, ""), new Paginator<>(3, "foo").getPage(allResults, null));
    assertEquals(Page.create(allResults, ""), new Paginator<>(4, "foo").getPage(allResults, null));
  }

  @Test
  void getPageEmptyResults() {
    assertEquals(
        Page.create(ImmutableList.of(), ""),
        new Paginator<>(2, "foo").getPage(ImmutableList.of(), null));
  }

  @Test
  void getPageBadTokenThrows() {
    assertThrows(
        BadRequestException.class,
        () -> new Paginator<>(3, "foo").getPage(ImmutableList.of(1, 2), "bogusToken"));
  }

  @Test
  void getPageTokenMismatchThrows() {
    List<Integer> allResults = IntStream.rangeClosed(0, 8).boxed().collect(Collectors.toList());
    Paginator<Integer> paginator = new Paginator<>(3, "foo");
    Page<Integer> page0 = paginator.getPage(allResults, null);

    PageSizeOffset decoded = Paginator.decode(page0.nextPageToken());
    // Change the pageSize from page0.
    assertThrows(
        BadRequestException.class,
        () ->
            paginator.getPage(
                allResults, Paginator.encode(decoded.toBuilder().setPageSize(5).build())));
    // Change the parameter hash from page0.
    assertThrows(
        BadRequestException.class,
        () ->
            paginator.getPage(
                allResults, Paginator.encode(decoded.toBuilder().setParameterHash("bar").build())));
  }
}
