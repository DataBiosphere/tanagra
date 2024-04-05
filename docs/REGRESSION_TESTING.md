# Regression Testing

## Motivation
Tanagra has a regression testing framework to detect unexpected changes in query results against different underlays.
This is different from the query engine tests we have that validate generated SQL strings. Those tests are very brittle, 
by design, so that we don't make unintentional changes to the generated SQL when e.g. adding support for a new type of 
query. The regression test framework only checks that the query results are the same, regardless of what generated SQL
was used to return them.

This framework is necessary because Tanagra has data dependent code paths. e.g.
  - The same generated SQL can return one row per `person` for one underlay, but multiple rows per `person` for a 
    different underlay, because the cardinality of the relationship between two entities is different.
  - The same generated SQL can return a valid numeric range for underlay, but only a `null` value for a different
    underlay.

Just validating that the generated SQL is as expected doesn't cover these variations in the underlying data.
Instead, we want to actually run the queries against a production or production-like dataset(s) and validate the results.

Most of these data dependent code paths can be broken by changes to any one of the following:
- Query engine code (e.g. expected generated SQL change not behaving as intended across all source datasets).
- Underlay config files (e.g. to the mapping to the source dataset).
- Criteria plugin code (e.g. filter builder returning unexpected filter for certain selection data).
- Underlying dataset (e.g. dataset refresh).

Note that some of these breakages fall under the purview of Tanagra's core codebase (e.g. query engine code), while
others are often managed elsewhere (e.g. dataset refresh).

## Framework overview
For a given underlay/dataset, we generally don't expect query results to change over time. For our purposes, checking
the query counts (i.e. total number of rows returned) is sufficient, and we don't compare the actual rows returned 
(e.g. check that all `person_id`s match). The idea of this framework is that we can preserve a particular set of 
cohort and data feature set definitions, compute the total number of rows they would export per output table, and save 
those definitions and counts to a regression test file. Then we can take those same definitions, recompute the export
row counts with the most recent code, and expect them to match the original.

## Create a new test
Create tests via the UI [screenshots].
Save tests to this repo is easiest, but not required.

## Run tests locally
`./gradlew service:regressionTests`
Explain available parameters and how service configs must match or include the tested underlays.

## Build a test suite
Recommend one test per cohort criteria + demographics data feature set, and one test per data feature set criteria + simple cohort.
Also include any tests against data that's notable (e.g. only your underlay includes a particular SQL schema or JOIN pattern, an output table is particularly large or contains many null values).

## Setup automatic test runs
Ideally, we could run these tests against an artificial/test dataset that is similar in complexity to our production
datasets. Unfortunately, the time investment required to build such a dataset is prohibitive, for now anyway. So we 
need to run these tests against production data. This limits where we can run these tests; Notably, we can't gate PRs 
in this DataBiosphere repo on these tests passing because our GitHub action service account doesn't have access to all
the production datasets. So, we need to be able to run some of these regression tests in this repo, and others in
"downstream" repos dedicated to each deployment and that have access to the production data.

We have a [sample GitHub action](../.github/workflowsForDownstreamRepo/regression-test-downstream-repo.yaml) for 
running these tests in a "downstream" repo. To get this working in your own repository:
- Specify the default underlays for which tests are run. This is set in 2 places:
  - The `default` value of the `workflow_dispatch.inputs.underlays` for on-demand runs.
  - The `env.DEFAULT_UNDERLAYS` variable for on-PR (or other triggered) runs.
- Specify the service config files that tests should use. This is set by the `env.TANAGRA_UNDERLAY_FILES` variable.
