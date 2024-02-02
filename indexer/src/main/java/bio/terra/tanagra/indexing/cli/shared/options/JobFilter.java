package bio.terra.tanagra.indexing.cli.shared.options;

import bio.terra.tanagra.cli.exception.UserActionableException;
import bio.terra.tanagra.indexing.job.IndexingJob;
import java.util.List;
import java.util.stream.Collectors;
import picocli.CommandLine;

/**
 * Command helper class that defines a filter on the jobs that get run.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class JobFilter {
  @CommandLine.Option(
      names = "--job-filter",
      description =
          "Only run jobs with these class names. Specify the class names relative to the IndexingJob class (e.g. bigquery.ValidateDataTypes, not bio.terra.tanagra.indexing.job.bigquery.ValidateDataTypes). Useful for debugging a particular indexing job.",
      split = ",")
  private List<String> classNames;

  public boolean isDefined() {
    return classNames != null && !classNames.isEmpty();
  }

  public List<String> getClassNamesWithPackage() {
    final String packageName = IndexingJob.class.getPackageName();
    return classNames.stream()
        .map(className -> packageName + '.' + className)
        .collect(Collectors.toList());
  }

  public void validate() {
    if (!isDefined()) {
      return;
    }
    for (String className : getClassNamesWithPackage()) {
      try {
        Class<?> clazz = Class.forName(className);
        if (!IndexingJob.class.isAssignableFrom(clazz)) {
          throw new UserActionableException(
              "Class name does not reference an indexing job: " + className);
        }
      } catch (ClassNotFoundException cnfEx) {
        throw new UserActionableException("Invalid class name: " + className, cnfEx);
      }
    }
  }
}
