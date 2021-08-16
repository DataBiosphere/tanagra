package bio.terra.tanagra.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.common.Paginator.Page;
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
    Page<Integer> page0 = Paginator.getPage(allResults, 3, null);
    Page<Integer> page1 = Paginator.getPage(allResults, 3, page0.nextPageToken());
    Page<Integer> page2 = Paginator.getPage(allResults, 3, page1.nextPageToken());
    assertEquals(ImmutableList.of(0, 1, 2), page0.results());
    assertEquals(ImmutableList.of(3, 4, 5), page1.results());
    assertEquals(ImmutableList.of(6, 7), page2.results());
    assertEquals("", page2.nextPageToken());
  }

  @Test
  void getPageSizeMultipleOfResults() {
    List<Integer> allResults = IntStream.range(0, 4).boxed().collect(Collectors.toList());
    Page<Integer> page0 = Paginator.getPage(allResults, 2, null);
    Page<Integer> page1 = Paginator.getPage(allResults, 2, page0.nextPageToken());
    assertEquals(ImmutableList.of(0, 1), page0.results());
    assertEquals(ImmutableList.of(2, 3), page1.results());
    assertEquals("", page1.nextPageToken());
  }

  @Test
  void getPageSizeGreaterThanResults() {
    List<Integer> allResults = ImmutableList.of(0, 1, 2);
    assertEquals(Page.create(allResults, ""), Paginator.getPage(allResults, 3, null));
    assertEquals(Page.create(allResults, ""), Paginator.getPage(allResults, 4, null));
  }

  @Test
  void getPageEmptyResults() {
    assertEquals(
        Page.create(ImmutableList.of(), ""), Paginator.getPage(ImmutableList.of(), 2, null));
  }

  @Test
  void getPageBadTokenThrows() {
    assertThrows(
        BadRequestException.class,
        () -> Paginator.getPage(ImmutableList.of(1, 2), 3, "bogusToken"));
  }

  @Test
  void getPageTokenSizeMismatchPageSizeThrows() {
    List<Integer> allResults = IntStream.rangeClosed(0, 8).boxed().collect(Collectors.toList());
    Page<Integer> page0 = Paginator.getPage(allResults, 3, null);
    // Change the pageSize from page0.
    assertThrows(
        BadRequestException.class, () -> Paginator.getPage(allResults, 5, page0.nextPageToken()));
  }
}
