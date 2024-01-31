package bio.terra.tanagra.indexing.job.dataflow.beam;

import static bio.terra.tanagra.underlay.entitymodel.DuplicateHandling.CHOOSE_ONE;
import static bio.terra.tanagra.underlay.entitymodel.DuplicateHandling.REMOVE_ALL;

import bio.terra.tanagra.underlay.entitymodel.DuplicateHandling;
import java.util.Iterator;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

public final class DuplicateHandlingUtils {
  private DuplicateHandlingUtils() {}

  public static <K, R> PCollection<R> run(
      DuplicateHandling mode, PCollection<KV<K, R>> allRowsKeyedById) {
    PCollection<KV<K, R>> allRowsFiltered;
    if (CHOOSE_ONE.equals(mode) || REMOVE_ALL.equals(mode)) {
      // Group by key.
      PCollection<KV<K, Iterable<R>>> allRowsGroupedById =
          allRowsKeyedById.apply(GroupByKey.create());
      allRowsFiltered =
          allRowsGroupedById.apply(
              ParDo.of(
                  new DoFn<KV<K, Iterable<R>>, KV<K, R>>() {
                    @ProcessElement
                    public void processElement(ProcessContext context) {
                      KV<K, Iterable<R>> element = context.element();
                      K id = element.getKey();
                      Iterator<R> rowsWithIdItr = element.getValue().iterator();

                      R firstRow = rowsWithIdItr.next();
                      if (CHOOSE_ONE.equals(mode)
                          || (REMOVE_ALL.equals(mode) && !rowsWithIdItr.hasNext())) {
                        context.output(KV.of(id, firstRow));
                      }
                    }
                  }));
    } else {
      allRowsFiltered = allRowsKeyedById;
    }
    return allRowsFiltered.apply(Values.create());
  }
}
