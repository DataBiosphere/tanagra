import Backdrop from "@mui/material/Backdrop";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { useEffect, useState } from "react";
import { Link as RouterLink } from "react-router-dom";
import { useAdminSource } from "sd-admin/source";
import { CohortV2, ReviewV2, StudyV2 } from "tanagra-api";

const mapCohortToResource = (
  {
    id,
    underlayName,
    displayName,
    lastModified,
    criteriaGroups,
    createdBy,
  }: CohortV2,
  study: StudyV2
) =>
  ({
    resourceId: id,
    resourceType: "Cohort",
    displayName,
    lastModified: lastModified.toLocaleDateString(),
    underlayName,
    createdBy,
    studyId: study.id,
    studyName: study.displayName,
    path: `/${underlayName}/cohorts/${id}/${criteriaGroups[0]?.id ?? "first"}`,
  } as StudyResource);

const mapReviewToResource = (
  { id, displayName, lastModified, createdBy }: ReviewV2,
  study: StudyV2,
  cohort: CohortV2
) =>
  ({
    resourceId: id,
    resourceType: "Review",
    displayName,
    lastModified: lastModified.toLocaleDateString(),
    underlayName: cohort.underlayName,
    createdBy,
    studyId: study.id,
    studyName: study.displayName,
    path: `/${cohort.underlayName}/review/${cohort.id}/${id}`,
  } as StudyResource);

interface StudyResource {
  resourceId: string;
  resourceType: string;
  displayName: string;
  studyId: string;
  studyName: string;
  lastModified: string;
  underlayName: string;
  createdBy: string;
  path: string;
}

export function Studies() {
  const source = useAdminSource();
  const [loadingStudies, setLoadingStudies] = useState<boolean>(true);
  const [studies, setStudies] = useState<StudyV2[]>([]);
  const [studyResources, setStudyResources] = useState<StudyResource[]>([]);

  useEffect(() => {
    getStudies();
  }, []);

  useEffect(() => {
    if (studies.length > 0) {
      getStudyResources();
    }
  }, [studies]);

  const getStudies = async () => {
    const studies = await source.getStudiesList();
    setStudies(studies);
    setLoadingStudies(false);
  };

  const getCohortsAndReviewsAsResources = async (study: StudyV2) => {
    const studyCohorts = await source.getCohortsForStudy(study.id);
    const cohortReviews = await Promise.all(
      studyCohorts.map(({ id }) => source.getReviewsForStudy(study.id, id))
    );
    const cohortResources = studyCohorts.map((cohort) =>
      mapCohortToResource(cohort, study)
    );
    const reviewResources = cohortReviews.reduce(
      (resourceList, reviewList, index) => [
        ...resourceList,
        ...reviewList.map((review) =>
          mapReviewToResource(review, study, studyCohorts[index])
        ),
      ],
      [] as StudyResource[]
    );
    return [...cohortResources, ...reviewResources];
  };

  const getConceptSetsAsResources = async (study: StudyV2) => {
    const studyConceptSets = await source.getConceptSetsForStudy(study.id);
    return studyConceptSets.map(
      ({ id, displayName, lastModified, underlayName, createdBy }) =>
        ({
          resourceId: id,
          resourceType: "Concept Set",
          displayName,
          lastModified: lastModified.toLocaleDateString(),
          underlayName,
          createdBy,
          studyId: study.id,
          studyName: study.displayName,
          path: `/${underlayName}/conceptSets/edit/${id}`,
        } as StudyResource)
    );
  };

  const getStudyResources = async () => {
    const promises = studies.reduce(
      (promiseList, study) => [
        ...promiseList,
        getCohortsAndReviewsAsResources(study),
        getConceptSetsAsResources(study),
      ],
      [] as Promise<StudyResource[]>[]
    );
    const resourceResponses = await Promise.all(promises);
    const resources = resourceResponses.reduce(
      (resourceList, response) => [...resourceList, ...response],
      [] as StudyResource[]
    );
    setStudyResources(resources);
  };

  return (
    <>
      <ActionBar title="Studies" />
      <div style={{ padding: "1rem" }}>
        <Stack direction="row" sx={{ position: "relative" }}>
          <Backdrop
            invisible
            open={loadingStudies}
            sx={{ position: "absolute" }}
          >
            <CircularProgress />
          </Backdrop>
          {!loadingStudies && !studies.length && (
            <Typography variant="h4">No studies found</Typography>
          )}
          {studies.map((study, index) => (
            <Card
              key={index}
              sx={{
                border: "1px solid rgb(183, 183, 183)",
                borderRadius: "0.25rem",
                boxShadow: "rgb(183 183 183) 0px 0.125rem 0.125rem 0px",
                height: "225px",
                width: "300px",
                margin: "0 1rem 1rem 0",
                textDecoration: "none",
              }}
              component={RouterLink}
              to={""}
            >
              <CardContent sx={{ height: "100%" }}>
                <Typography variant="h3">{study.displayName}</Typography>
                <Typography variant="body1">{study.description}</Typography>
              </CardContent>
            </Card>
          ))}
        </Stack>
        {studies.length > 0 && (
          <>
            <h2>Resources</h2>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Item Type</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Study Name</TableCell>
                  <TableCell>Last Modified Date</TableCell>
                  <TableCell>Created By</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {studyResources.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5}>No resources found</TableCell>
                  </TableRow>
                )}
                {studyResources.map((resource, index) => (
                  <TableRow key={index}>
                    <TableCell>
                      <Chip label={resource.resourceType} color="primary" />
                    </TableCell>
                    <TableCell>
                      <Link
                        variant="body2"
                        color="primary"
                        underline="hover"
                        component={RouterLink}
                        to={resource.path}
                      >
                        {resource.displayName}
                      </Link>
                    </TableCell>
                    <TableCell>{resource.studyName}</TableCell>
                    <TableCell>{resource.lastModified}</TableCell>
                    <TableCell>{resource.createdBy}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </>
        )}
      </div>
    </>
  );
}
