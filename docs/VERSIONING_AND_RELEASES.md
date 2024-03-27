# Versioning and Releases

## Bump the codebase version
Tanagra uses semantic versioning `major.minor.patch`.
Each PR that's merged to the `main` branch automatically bumps the `patch` number.

Run the [`Bump, Tag, Publish` GitHub action](https://github.com/DataBiosphere/tanagra/actions/workflows/bump-tag-publish.yaml)
manually to bump the `major` or `minor` version.

Every version is assumed to be working. Bugs will slip through of course, but we will never intentionally leave the `main`
branch in a broken state. Use a feature branch for long-running work. You can open PRs against a feature branch to break
up a large change. Only merge to `main` once the branch is tested and working.


## Create a new release
Each PR that's merged to the `main` branch creates a new release and publishes the service client library to Artifactory.
The PR description and link are included in the release description.
If you manually create a release by running the [`Bump, Tag, Publish` GitHub action](https://github.com/DataBiosphere/tanagra/actions/workflows/bump-tag-publish.yaml), 
make sure to also edit the created release description, since it won't be populated automatically.

Each release includes a copy of the OpenAPI specification for the service. This is useful for generating clients for
different languages. We also publish a copy of the Java client library to Artifactory for each release.

A list of all releases is [available here](https://github.com/DataBiosphere/tanagra/releases).


## Manage releases for a deployment
Tanagra supports multiple deployments, all with different release cadences.
Each deployment needs to manage which version of the main Tanagra codebase it's on.

One way to do this is to have a separate, downstream GitHub repository for each deployment that:
  - Maintains the version that the deployment is pegged to. 
  - Points back to this repository with a [git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules) reference.
  - Pulls the pegged version of the main Tanagra codebase and runs any deployment or dataset-specific validation.
  - Builds and deploys the code.

(This is basically the approach that the VUMC and Verily deployments currently use.)

When you want to update the code version for a deployment, there are 2 options.

#### Pull a tag for a specific version.
This is the easiest option. It will pull in all changes to this codebase through the new release.
- Bump the version that the deployment is pegged to e.g. from `0.0.400` -> `0.0.435`.
- Run any deployment or dataset-specific validation before deploying the newer release.

#### Pull a specific change(s)
This is more involved, with the tradeoff of potentially greater stability of your deployment.
It will only pull in the specific changes you choose since the pegged version.
- Create a branch from the version that your deployment is currently pegged to e.g. from `0.0.400`.
- Find the specific change(s) that you want and [cherry-pick](https://git-scm.com/docs/git-cherry-pick) them into your branch.
- From the [releases page](https://github.com/DataBiosphere/tanagra/releases), manually create a new release from your branch.
- Run any deployment or dataset-specific validation before deploying the custom release.


## One version across the project
The Tanagra project has three main pieces: indexer, service, UI.
**We generally expect the version across all three to match.**
This means that the service & UI versions match and the dataset was indexed using an indexer with the same version.
Provided the versions are close enough, things often work even if they don't match across all three pieces.
However, this is not something we build for or test currently, so no guarantees.

Longer-term, we'd like to support some backwards compatibility for the indexer, at least.
Of the three main pieces, indexing is currently the one where we most frequently want to try a mismatched version,
because it can take several hours to run and so isn't conducive to turning around a quick fix to the service or UI.

You can use this [tool to diff two release tags](./DIFF_RELEASES.md) to get a crude guess of whether you need
to re-run indexing in order to bump a version. This is a very simple script that just checks for any code changes in the 
indexer code path, so it returns many false positives.
The tool also returns a list of merged PRs between the releases, so you can also contact the authors of all those
PRs to get a more informed answer about whether re-indexing is needed.
