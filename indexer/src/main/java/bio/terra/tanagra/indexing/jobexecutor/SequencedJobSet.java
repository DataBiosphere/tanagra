package bio.terra.tanagra.indexing.jobexecutor;

import bio.terra.tanagra.indexing.job.IndexingJob;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Container for a set of jobs and the sequence they must be run in. The stages must be run
 * serially. The jobs within each stage can be run in parallel.
 */
public class SequencedJobSet {
  private final List<List<IndexingJob>> stages;
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
