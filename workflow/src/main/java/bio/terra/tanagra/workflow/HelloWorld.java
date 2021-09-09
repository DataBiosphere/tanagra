package bio.terra.tanagra.workflow;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;

/** Dummy Beam workflow. */
public final class HelloWorld {
  private HelloWorld() {}

  public static void main(String[] args) {
    PipelineOptions options = PipelineOptionsFactory.create();
    Pipeline p = Pipeline.create(options);

    p.apply(Create.of("Hello", "World")).apply(TextIO.write().to("hello"));

    p.run().waitUntilFinish();
  }
}
