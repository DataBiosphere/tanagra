package bio.terra.tanagra.indexing.job.bigquery.beam;

import bio.terra.tanagra.indexing.job.dataflow.beam.DuplicateHandlingUtils;
import bio.terra.tanagra.testing.KVUtils;
import bio.terra.tanagra.underlay.entitymodel.DuplicateHandling;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.List;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

public class DuplicateHandlingUtilsTest {
  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  @Test
  public void expectNoneNoDuplicates() {
    // EXPECT_NONE = no changes to input.
    Multimap<Long, String> input = MultimapBuilder.hashKeys().arrayListValues().build();
    input.putAll(10L, List.of("20"));
    input.putAll(11L, List.of("21"));
    input.putAll(12L, List.of("22"));
    PCollection<KV<Long, String>> inputPC = pipeline.apply(Create.of(KVUtils.convertToKvs(input)));
    PCollection<String> outputPC =
        DuplicateHandlingUtils.run(DuplicateHandling.EXPECT_NONE, inputPC);
    PAssert.that(outputPC).containsInAnyOrder(input.values());
    pipeline.run().waitUntilFinish();
  }

  @Test
  public void expectNoneWithDuplicates() {
    // EXPECT_NONE = no changes to input.
    Multimap<Long, String> input = MultimapBuilder.hashKeys().arrayListValues().build();
    input.putAll(10L, List.of("20", "24"));
    input.putAll(11L, List.of("21"));
    input.putAll(12L, List.of("22", "25"));
    PCollection<KV<Long, String>> inputPC = pipeline.apply(Create.of(KVUtils.convertToKvs(input)));
    PCollection<String> outputPC =
        DuplicateHandlingUtils.run(DuplicateHandling.EXPECT_NONE, inputPC);
    PAssert.that(outputPC).containsInAnyOrder(input.values());
    pipeline.run().waitUntilFinish();
  }

  @Test
  public void chooseOneNoDuplicates() {
    // CHOOSE_ONE = randomly choose one value and drop the others when there are duplicates.
    Multimap<Long, String> input = MultimapBuilder.hashKeys().arrayListValues().build();
    input.putAll(10L, List.of("20"));
    input.putAll(11L, List.of("21"));
    input.putAll(12L, List.of("22"));
    PCollection<KV<Long, String>> inputPC = pipeline.apply(Create.of(KVUtils.convertToKvs(input)));
    PCollection<String> outputPC =
        DuplicateHandlingUtils.run(DuplicateHandling.CHOOSE_ONE, inputPC);
    PAssert.that(outputPC).containsInAnyOrder(input.values());
    pipeline.run().waitUntilFinish();
  }

  @Test
  public void chooseOneWithDuplicates() {
    // CHOOSE_ONE = randomly choose one value and drop the others when there are duplicates.
    Multimap<Long, String> input = MultimapBuilder.hashKeys().arrayListValues().build();
    input.putAll(10L, List.of("20", "20"));
    input.putAll(11L, List.of("21"));
    input.putAll(12L, List.of("22", "22"));
    PCollection<KV<Long, String>> inputPC = pipeline.apply(Create.of(KVUtils.convertToKvs(input)));
    PCollection<String> outputPC =
        DuplicateHandlingUtils.run(DuplicateHandling.CHOOSE_ONE, inputPC);

    Multimap<Long, String> expectedOutput = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedOutput.putAll(10L, List.of("20"));
    expectedOutput.putAll(11L, List.of("21"));
    expectedOutput.putAll(12L, List.of("22"));
    PAssert.that(outputPC).containsInAnyOrder(expectedOutput.values());
    pipeline.run().waitUntilFinish();
  }

  @Test
  public void removeAllNoDuplicates() {
    // REMOVE_ALL = drop all rows that contain duplicates.
    Multimap<Long, String> input = MultimapBuilder.hashKeys().arrayListValues().build();
    input.putAll(10L, List.of("20"));
    input.putAll(11L, List.of("21"));
    input.putAll(12L, List.of("22"));
    PCollection<KV<Long, String>> inputPC = pipeline.apply(Create.of(KVUtils.convertToKvs(input)));
    PCollection<String> outputPC =
        DuplicateHandlingUtils.run(DuplicateHandling.REMOVE_ALL, inputPC);
    PAssert.that(outputPC).containsInAnyOrder(input.values());
    pipeline.run().waitUntilFinish();
  }

  @Test
  public void removeAllWithDuplicates() {
    // REMOVE_ALL = drop all rows that contain duplicates.
    Multimap<Long, String> input = MultimapBuilder.hashKeys().arrayListValues().build();
    input.putAll(10L, List.of("20", "45"));
    input.putAll(11L, List.of("48"));
    input.putAll(12L, List.of("22", "46"));
    PCollection<KV<Long, String>> inputPC = pipeline.apply(Create.of(KVUtils.convertToKvs(input)));
    PCollection<String> outputPC =
        DuplicateHandlingUtils.run(DuplicateHandling.REMOVE_ALL, inputPC);

    Multimap<Long, String> expectedOutput = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedOutput.putAll(11L, List.of("48"));
    PAssert.that(outputPC).containsInAnyOrder(expectedOutput.values());
    pipeline.run().waitUntilFinish();
  }
}
