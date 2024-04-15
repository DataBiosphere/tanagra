import Button from "@mui/material/Button";
import Link from "@mui/material/Link";
import ActionBar from "actionBar";
import { generateId, sectionName } from "cohort";
import { insertCohortCriteria, useCohortContext } from "cohortContext";
import Empty from "components/empty";
import Loading from "components/loading";
import {
  TreeGrid,
  TreeGridId,
  useArrayAsTreeGridData,
} from "components/treegrid";
import { FeatureSet } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useCohortGroupSectionAndGroup, useStudyId, useUnderlay } from "hooks";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useCallback } from "react";
import { absoluteFeatureSetURL, cohortURL, useBaseParams } from "router";
import useSWR from "swr";
import { useNavigate } from "util/searchState";

export function AddFeatureSet() {
  const studyId = useStudyId();
  const underlaySource = useUnderlaySource();
  const studySource = useStudySource();
  const underlay = useUnderlay();
  const context = useCohortContext();
  const { cohort, section, sectionIndex } = useCohortGroupSectionAndGroup();

  const navigate = useNavigate();
  const params = useBaseParams();

  const featureSetsState = useSWR(
    {
      type: "featureSets",
      studyId,
      underlayName: underlay.name,
    },
    async () =>
      await studySource
        .listFeatureSets(studyId, underlaySource)
        .then((res) => res.filter((fs) => fs.underlayName === underlay.name))
  );

  const data = useArrayAsTreeGridData(featureSetsState.data ?? [], "id");

  const columns = [
    { key: "name", width: "100%", title: "Name" },
    { key: "lastModified", width: 200, title: "Last modified" },
    { key: "button", width: 80 },
  ];

  const newFeatureSet = async () => {
    const featureSet = await studySource.createFeatureSet(
      underlay.name,
      studyId
    );
    navigate(absoluteFeatureSetURL(params, featureSet.id));
  };

  const onInsertFeatureSet = useCallback(
    (featureSet: FeatureSet) => {
      const filteredCriteria = featureSet.criteria.filter(
        (c) => c.config.isEnabledForCohorts
      );
      const group = insertCohortCriteria(
        context,
        section.id,
        produce(filteredCriteria, (fc) =>
          fc.forEach((c) => (c.id = generateId()))
        )
      );
      navigate("../../../" + cohortURL(cohort.id, section.id, group.id));
    },
    [context, cohort.id, section.id, navigate]
  );

  const title = `Adding criteria from a feature set to ${sectionName(
    section,
    sectionIndex
  )}`;

  return (
    <GridLayout rows>
      <ActionBar title={title} />
      <GridBox
        sx={{
          backgroundColor: (theme) => theme.palette.background.paper,
        }}
      >
        <Loading status={featureSetsState}>
          {featureSetsState.data?.length ? (
            <TreeGrid
              data={data}
              columns={columns}
              rowCustomization={(id: TreeGridId) => {
                if (!featureSetsState.data) {
                  return undefined;
                }

                const featureSet = data[id].data as FeatureSet;
                if (!featureSet) {
                  return undefined;
                }

                return [
                  {
                    column: columns.length - 1,
                    content: (
                      <GridLayout cols fillCol={0}>
                        <Button
                          variant="outlined"
                          onClick={() => onInsertFeatureSet(featureSet)}
                        >
                          Add
                        </Button>
                      </GridLayout>
                    ),
                  },
                ];
              }}
            />
          ) : (
            <Empty
              maxWidth="80%"
              title="No feature sets have been created"
              subtitle={
                <Link
                  variant="link"
                  underline="hover"
                  onClick={() => newFeatureSet()}
                  sx={{ cursor: "pointer" }}
                >
                  Create a feature set
                </Link>
              }
            />
          )}
        </Loading>
      </GridBox>
    </GridLayout>
  );
}
