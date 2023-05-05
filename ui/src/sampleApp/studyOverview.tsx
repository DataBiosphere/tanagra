import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import { getCriteriaTitle } from "cohort";
import Empty from "components/empty";
import Loading from "components/loading";
import { TreeGrid, TreeGridData, TreeGridId } from "components/treegrid";
import { useSource } from "data/sourceContext";
import { DataKey } from "data/types";
import { useStudyId, useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import React, { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import {
  absoluteCohortURL,
  absoluteConceptSetURL,
  absoluteExportURL,
  absoluteNewConceptSetURL,
  useBaseParams,
} from "router";
import { Header } from "sampleApp/header";
import useSWR from "swr";

enum ArtifactType {
  Cohort = "Cohort",
  ConceptSet = "Data feature",
}

type Artifact = {
  type: ArtifactType;
  name: string;
  id: string;
  cohortGroupSectionId: string;
};

const columns = [
  {
    key: "type",
    width: 120,
    title: "",
  },
  {
    key: "name",
    width: "100%",
    title: "Name",
  },
];

export function StudyOverview() {
  const source = useSource();
  const studyId = useStudyId();

  const artifactsState = useSWR(
    { type: "studyOverview", studyId },
    async () => {
      return await Promise.all([
        source.listCohorts(studyId).then((res) =>
          res
            .filter((c) => c.underlayName === underlay.name)
            .map((c) => ({
              type: ArtifactType.Cohort,
              name: c.name,
              id: c.id,
              cohortGroupSectionId: c.groupSections[0].id,
            }))
        ),
        source
          .listConceptSets(studyId)

          .then((res) =>
            res
              .filter((cs) => cs.underlayName === underlay.name)
              .map((cs) => ({
                type: ArtifactType.ConceptSet,
                name: getCriteriaTitle(cs.criteria),
                id: cs.id,
                cohortGroupSectionId: "",
              }))
          ),
      ]).then((res) => [...res[0], ...res[1]]);
    }
  );

  const data = useMemo(() => {
    const children: DataKey[] = [];
    const data: TreeGridData = {
      root: { data: {}, children },
    };

    artifactsState.data?.forEach((artifact) => {
      const key = `${artifact.type}~${artifact.id}`;
      children.push(key);

      const item = {
        data: {
          type: (
            <Stack direction="row" justifyContent="center">
              <Chip label={String(artifact.type)} size="small" />
            </Stack>
          ),
          name: artifact.name,
        },
      };
      data[key] = item;
    });

    return data;
  }, [source, artifactsState.data]);

  const navigate = useNavigate();

  const underlay = useUnderlay();
  const params = useBaseParams();

  const newCohort = async () => {
    const cohort = await source.createCohort(
      underlay.name,
      studyId,
      `Untitled cohort ${new Date().toLocaleString()}`
    );
    navigate(
      absoluteCohortURL(
        params,
        cohort.id,
        cohort.groupSections[0].id
      ).substring(1)
    );
  };

  const onClick = (artifact: Artifact) => {
    switch (artifact.type) {
      case ArtifactType.Cohort:
        navigate(
          absoluteCohortURL(
            params,
            artifact.id,
            artifact.cohortGroupSectionId
          ).substring(1)
        );
        break;
      case ArtifactType.ConceptSet:
        navigate(absoluteConceptSetURL(params, artifact.id).substring(1));
        break;
    }
  };

  return (
    <GridBox
      sx={{ backgroundColor: (theme) => theme.palette.background.paper }}
    >
      <GridLayout rows>
        <Header />
        <GridBox sx={{ p: 1 }}>
          <GridLayout cols={4} spacing={1}>
            <Button variant="contained" onClick={newCohort}>
              New cohort
            </Button>
            <Button
              variant="contained"
              onClick={() =>
                navigate(absoluteNewConceptSetURL(params).substring(1))
              }
            >
              New data feature
            </Button>
            <Button
              variant="contained"
              onClick={() => navigate(absoluteExportURL(params).substring(1))}
            >
              Export
            </Button>
          </GridLayout>
        </GridBox>
        <Loading status={artifactsState}>
          {!!data?.root?.children?.length ? (
            <TreeGrid
              columns={columns}
              data={data}
              rowCustomization={(id: TreeGridId) => {
                const parts = String(id).split("~");
                if (parts.length != 2) {
                  return undefined;
                }

                const artifact = artifactsState.data?.find(
                  (a) => a.type === parts[0] && a.id === parts[1]
                );
                if (!artifact) {
                  return undefined;
                }

                return [
                  {
                    column: 1,
                    onClick: () => onClick(artifact),
                  },
                ];
              }}
            />
          ) : (
            <Empty
              minHeight="300px"
              image="/empty.svg"
              title="Create cohorts and data features using the buttons above"
            />
          )}
        </Loading>
      </GridLayout>
    </GridBox>
  );
}
