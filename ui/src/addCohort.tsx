import Button from "@mui/material/Button";
import Link from "@mui/material/Link";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { generateId, getCriteriaTitle } from "cohort";
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

type CohortData = Cohort & {
  criteriaTitles: string[];
};

function filterCriteria(cohort: Cohort) {
  return cohort.groupSections
    .filter((gs) => !gs.filter.excluded)
    .map((gs) => gs.groups)
    .flat()
    .map((g) => g.criteria[0])
    .filter((c) => c.config.isEnabledForDataFeatureSets);
}

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
      (
        await studySource
          .listCohorts(studyId, underlaySource)
          .then((res) => res.filter((fs) => fs.underlayName === underlay.name))
      ).map((cohort) => ({
        criteriaTitles: filterCriteria(cohort).map((c) => getCriteriaTitle(c)),
        ...cohort,
      }))
  );

  const data = useArrayAsTreeGridData(cohortsState.data ?? [], "id");

  const columns = [
    { key: "name", width: "100%", title: "Name" },
    { key: "lastModified", width: 200, title: "Last modified" },
    { key: "criteria", width: 100, title: "Criteria" },
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
      const filteredCriteria = filterCriteria(cohort);
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

                const cohortData = data[id].data as CohortData;
                if (!cohortData) {
                  return undefined;
                }

                return [
                  {
                    column: columns.length - 2,
                    content: cohortData.criteriaTitles.length ? (
                      <Tooltip
                        title={
                          <span style={{ whiteSpace: "pre-line" }}>
                            {cohortData.criteriaTitles.join("\n")}
                          </span>
                        }
                      >
                        <Typography
                          variant="body2"
                          sx={{ textDecoration: "underline" }}
                        >
                          {cohortData.criteriaTitles.length}
                        </Typography>
                      </Tooltip>
                    ) : (
                      <Typography variant="body2">0</Typography>
                    ),
                  },
                  {
                    column: columns.length - 1,
                    content: (
                      <Button
                        variant="outlined"
                        disabled={!cohortData.criteriaTitles.length}
                        onClick={() => onInsertCohort(cohortData)}
                      >
                        Add
                      </Button>
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
