package bio.terra.tanagra.indexing.jobexecutor;

import bio.terra.tanagra.indexing.job.IndexingJob;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Container for a set of jobs and the sequence they must be run in. The stages must be run
 * serially. The jobs within each stage can be run in parallel.
 */
public class SequencedJobSet {
  private List<List<IndexingJob>> stages;
  private final String description;

  public SequencedJobSet(String description) {
    this.stages = new ArrayList<>();
    this.description = description;
  }

  public void startNewStage() {
    stages.add(new ArrayList<>());
  }

  public void addJob(IndexingJob job) {
    stages.get(stages.size() - 1).add(job);
  }

  public Iterator<List<IndexingJob>> iterator() {
    return stages.iterator();
  }

  public SequencedJobSet filterJobs(List<String> jobClassNames) {
    List<List<IndexingJob>> filteredStages = new ArrayList<>();
    for (List<IndexingJob> stage : stages) {
      List<IndexingJob> filteredStage =
          stage.stream()
              .filter(indexingJob -> jobClassNames.contains(indexingJob.getClass().getName()))
              .collect(Collectors.toList());
      if (!filteredStage.isEmpty()) {
        filteredStages.add(filteredStage);
      }
    }
    this.stages = filteredStages;
    return this;
  }

  public int getNumStages() {
    return stages.size();
  }

  public long getNumJobs() {
    return stages.stream().map(List::size).count();
  }

  public String getDescription() {
    return description;
  }
}
