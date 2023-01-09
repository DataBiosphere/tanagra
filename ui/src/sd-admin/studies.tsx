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
import { Link as RouterLink, useParams } from "react-router-dom";
import { useAdminSource } from "sd-admin/source";
import { StudyV2 } from "tanagra-api";

interface StudyResource {
  resourceId: string;
  resourceType: string;
  displayName: string;
  studyId: string;
  studyName: string;
  lastModified: string;
  underlayName: string;
  createdBy: string;
}

export function Studies() {
  const source = useAdminSource();
  const { underlayName } = useParams<{ underlayName: string }>();
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
    return [...studyCohorts, ...cohortReviews.flat(1)].map(
      ({ id, displayName, lastModified, createdBy }, index) =>
        ({
          resourceId: id,
          resourceType: index < studyCohorts.length ? "Cohort" : "Review",
          displayName,
          lastModified: lastModified.toLocaleDateString(),
          underlayName,
          createdBy,
          studyId: study.id,
          studyName: study.displayName,
        } as StudyResource)
    );
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
              to={study.id}
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
                  <TableCell>Dataset</TableCell>
                  <TableCell>Created By</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {studyResources.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6}>No resources found</TableCell>
                  </TableRow>
                )}
                {studyResources.map((resource, index) => (
                  <TableRow key={index}>
                    <TableCell>
                      <Chip label={resource.resourceType} color="primary" />
                    </TableCell>
                    <TableCell>{resource.displayName}</TableCell>
                    <TableCell>
                      <Link
                        variant="body2"
                        color="primary"
                        underline="hover"
                        component={RouterLink}
                        to={resource.studyId}
                      >
                        {resource.studyName}
                      </Link>
                    </TableCell>
                    <TableCell>{resource.lastModified}</TableCell>
                    <TableCell>{resource.underlayName}</TableCell>
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
