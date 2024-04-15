import Button from "@mui/material/Button";
import Link from "@mui/material/Link";
import ActionBar from "actionBar";
import { generateId } from "cohort";
import Empty from "components/empty";
import Loading from "components/loading";
import {
  TreeGrid,
  TreeGridId,
  useArrayAsTreeGridData,
} from "components/treegrid";
import { Cohort } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import {
  insertFeatureSetCriteria,
  useFeatureSetContext,
} from "featureSet/featureSetContext";
import { useFeatureSet, useStudyId, useUnderlay } from "hooks";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useCallback } from "react";
import { absoluteFeatureSetURL, featureSetURL, useBaseParams } from "router";
import useSWR from "swr";
import { useNavigate } from "util/searchState";

export function AddCohort() {
  const studyId = useStudyId();
  const underlaySource = useUnderlaySource();
  const studySource = useStudySource();
  const underlay = useUnderlay();
  const context = useFeatureSetContext();
  const featureSet = useFeatureSet();

  const navigate = useNavigate();
  const params = useBaseParams();

  const cohortsState = useSWR(
    {
      type: "cohorts",
      studyId,
      underlayName: underlay.name,
    },
    async () =>
      await studySource
        .listCohorts(studyId, underlaySource)
        .then((res) => res.filter((fs) => fs.underlayName === underlay.name))
  );

  const data = useArrayAsTreeGridData(cohortsState.data ?? [], "id");

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

  const onInsertCohort = useCallback(
    (cohort: Cohort) => {
      const filteredCriteria = cohort.groupSections
        .filter((gs) => !gs.filter.excluded)
        .map((gs) => gs.groups)
        .flat()
        .map((g) => g.criteria[0])
        .filter((c) => c.config.isEnabledForDataFeatureSets);
      insertFeatureSetCriteria(
        context,
        produce(filteredCriteria, (fc) =>
          fc.forEach((c) => (c.id = generateId()))
        )
      );
      navigate("../../../" + featureSetURL(featureSet.id));
    },
    [context, featureSet.id, navigate]
  );

  return (
    <GridLayout rows>
      <ActionBar title={"Adding criteria from a cohort"} />
      <GridBox
        sx={{
          backgroundColor: (theme) => theme.palette.background.paper,
        }}
      >
        <Loading status={cohortsState}>
          {cohortsState.data?.length ? (
            <TreeGrid
              data={data}
              columns={columns}
              rowCustomization={(id: TreeGridId) => {
                if (!cohortsState.data) {
                  return undefined;
                }

                const cohort = data[id].data as Cohort;
                if (!cohort) {
                  return undefined;
                }

                return [
                  {
                    column: columns.length - 1,
                    content: (
                      <GridLayout cols fillCol={0}>
                        <Button
                          variant="outlined"
                          onClick={() => onInsertCohort(cohort)}
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
              title="No cohorts have been created"
              subtitle={
                <Link
                  variant="link"
                  underline="hover"
                  onClick={() => newFeatureSet()}
                  sx={{ cursor: "pointer" }}
                >
                  Create a cohort
                </Link>
              }
            />
          )}
        </Loading>
      </GridBox>
    </GridLayout>
  );
}
