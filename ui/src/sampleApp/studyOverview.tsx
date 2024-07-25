import DeleteIcon from "@mui/icons-material/Delete";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import IconButton from "@mui/material/IconButton";
import Empty from "components/empty";
import Loading from "components/loading";
import { useSimpleDialog } from "components/simpleDialog";
import { TreeGrid, TreeGridData, TreeGridId } from "components/treegrid";
import { useStudySource } from "data/studySourceContext";
import { DataKey } from "data/types";
import { useStudyId, useUnderlay } from "hooks";
import emptyImage from "images/empty.svg";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import React, { useCallback, useMemo } from "react";
import {
  absoluteCohortURL,
  absoluteExportURL,
  absoluteFeatureSetURL,
  useBaseParams,
} from "router";
import { Header } from "sampleApp/header";
import useSWR from "swr";
import { useNavigate } from "util/searchState";

enum ArtifactType {
  Cohort = "Cohort",
  FeatureSet = "Feature set",
}

type Artifact = {
  type: ArtifactType;
  name: string;
  id: string;
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
  {
    key: "controls",
    width: 80,
    title: "",
  },
];

export function StudyOverview() {
  const studySource = useStudySource();
  const studyId = useStudyId();

  const listArtifacts = useCallback(async () => {
    return await Promise.all([
      studySource.listCohortMetadata(studyId).then((res) =>
        res
          .filter((c) => c.underlayName === underlay.name)
          .map((c) => ({
            type: ArtifactType.Cohort,
            name: c.name,
            id: c.id,
          }))
      ),
      studySource.listFeatureSetMetadata(studyId).then((res) =>
        res
          .filter((fs) => fs.underlayName === underlay.name)
          .map((fs) => ({
            type: ArtifactType.FeatureSet,
            name: fs.name,
            id: fs.id,
          }))
      ),
    ]).then((res) => res.flat());
  }, [studySource, studyId]);

  const artifactsState = useSWR(
    { type: "studyOverview", studyId },
    listArtifacts
  );

  const deleteArtifact = useCallback(
    async (artifact: Artifact) => {
      artifactsState.mutate(async () => {
        switch (artifact.type) {
          case ArtifactType.Cohort:
            await studySource.deleteCohort(studyId, artifact.id);
            break;
          case ArtifactType.FeatureSet:
            await studySource.deleteFeatureSet(studyId, artifact.id);
            break;
          default:
            throw new Error(
              `Unknown artifact type: ${artifact.type}, id: ${artifact.id}`
            );
        }

        return await listArtifacts();
      });
    },
    [studySource, artifactsState.data]
  );

  const [confirmDialog, showConfirmDialog] = useSimpleDialog();

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
          type: <Chip label={String(artifact.type)} size="small" />,
          name: artifact.name,
          controls: (
            <GridLayout colAlign="center">
              <IconButton
                onClick={() =>
                  showConfirmDialog({
                    title: `Delete ${artifact.name}?`,
                    text: `Are you sure you want to delete "${artifact.name}"? This action is permanent.`,
                    buttons: ["Cancel", "Delete"],
                    onButton: (button) => {
                      if (button === 1) {
                        deleteArtifact(artifact);
                      }
                    },
                  })
                }
              >
                <DeleteIcon />
              </IconButton>
            </GridLayout>
          ),
        },
      };
      data[key] = item;
    });

    return data;
  }, [studySource, artifactsState.data]);

  const navigate = useNavigate();

  const underlay = useUnderlay();
  const params = useBaseParams();

  const newCohort = async () => {
    const cohort = await studySource.createCohort(underlay.name, studyId);
    navigate(absoluteCohortURL(params, cohort.id).substring(1));
  };

  const newFeatureSet = async () => {
    const featureSet = await studySource.createFeatureSet(
      underlay.name,
      studyId
    );
    navigate(absoluteFeatureSetURL(params, featureSet.id).substring(1));
  };

  const onClick = (artifact: Artifact) => {
    switch (artifact.type) {
      case ArtifactType.Cohort:
        navigate(absoluteCohortURL(params, artifact.id).substring(1));
        break;
      case ArtifactType.FeatureSet:
        navigate(absoluteFeatureSetURL(params, artifact.id).substring(1));
        break;
    }
  };

  return (
    <GridBox
      sx={{ backgroundColor: (theme) => theme.palette.background.paper }}
    >
      <GridLayout rows>
        <Header />
        <GridBox sx={{ px: 4, py: 2 }}>
          <GridLayout cols spacing={1}>
            <Button variant="contained" onClick={newCohort}>
              New cohort
            </Button>
            <Button variant="contained" onClick={newFeatureSet}>
              New feature set
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
              image={emptyImage}
              title="Create cohorts and data features using the buttons above"
            />
          )}
        </Loading>
      </GridLayout>
      {confirmDialog}
    </GridBox>
  );
}
