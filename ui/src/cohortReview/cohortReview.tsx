import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import KeyboardArrowLeftIcon from "@mui/icons-material/KeyboardArrowLeft";
import KeyboardArrowRightIcon from "@mui/icons-material/KeyboardArrowRight";
import LockIcon from "@mui/icons-material/Lock";
import MenuIcon from "@mui/icons-material/Menu";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { useCohortContext } from "cohortContext";
import Loading from "components/loading";
import {
  Annotation,
  AnnotationType,
  ReviewInstance,
  useSource,
} from "data/source";
import { useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { ReactNode, useMemo } from "react";
import { useParams } from "react-router-dom";
import { absoluteCohortReviewListURL, useBaseParams } from "router";
import useSWR from "swr";
import { useSearchData } from "util/searchData";
import { useNewAnnotationDialog } from "./newAnnotationDialog";
import {CohortReviewAttribute} from "../underlaysSlice";

type SearchData = {
  index?: number;
  editingAnnotations?: boolean;
};

export function CohortReview() {
  const source = useSource();
  const underlay = useUnderlay();
  const params = useBaseParams();

  const { reviewId } = useParams<{ reviewId: string }>();
  if (!reviewId) {
    throw new Error("Review ID is null.");
  }

  const cohort = useCohortContext().state?.present;
  if (!cohort) {
    throw new Error("Cohort context state is null.");
  }

  const uiConfig = {attributes: [] as CohortReviewAttribute[], primaryKey: ''};

  const primaryAttributes = useMemo(
    () => [uiConfig.primaryKey, ...uiConfig.attributes.map((a) => a.key)],
    [uiConfig]
  );

  const reviewState = useSWR(
    { type: "review", studyId: params.studyId, reviewId, cohortId: cohort.id },
    async (key) => {
      return await source.getCohortReview(
        key.studyId,
        key.cohortId,
        key.reviewId
      );
    }
  );

  const instancesState = useSWR(
    {
      type: "reviewInstances",
      studyId: params.studyId,
      reviewId,
      cohortId: cohort.id,
    },
    async (key) => {
      return await source.listReviewInstances(
        key.studyId,
        key.cohortId,
        key.reviewId,
        primaryAttributes
      );
    }
  );

  const annotationsState = useSWR(
    {
      type: "annotation",
      studyId: params.studyId,
      cohortId: cohort.id,
    },
    async (key) => {
      return await source.listAnnotations(key.studyId, key.cohortId);
    }
  );

  const [newAnnotationDialog, showNewAnnotationDialog] = useNewAnnotationDialog(
    {
      onCreate: async (
        displayName: string,
        annotationType: AnnotationType,
        enumVals?: string[]
      ) => {
        await source.createAnnotation(
          params.studyId,
          cohort.id,
          displayName,
          annotationType,
          enumVals
        );
        annotationsState.mutate();
      },
    }
  );

  const [searchData, updateSearchData] = useSearchData<SearchData>();

  const index = searchData.index ?? 0;
  const instance = instancesState.data?.[index];
  const count = instancesState.data?.length ?? 1;

  return (
    <GridBox>
      <ActionBar
        title={
          reviewState?.data?.displayName
            ? `Review "${reviewState?.data?.displayName}"`
            : ""
        }
        backURL={absoluteCohortReviewListURL(params, cohort.id, reviewId)}
      />
      <Loading status={instancesState}>
        <GridLayout cols="240px auto">
          <GridBox
            sx={{
              p: 1,
              backgroundColor: (theme) => theme.palette.background.paper,
            }}
          >
            <GridLayout rows>
              <GridLayout rows colAlign="center">
                <Typography variant="h3">Participant</Typography>
                <Typography variant="h3">
                  {instance?.data?.[uiConfig.primaryKey]}
                </Typography>
                <Typography variant="h4">
                  {index + 1}/{count}
                </Typography>
                <GridLayout cols>
                  <IconButton
                    disabled={index === 0}
                    onClick={() =>
                      updateSearchData((data: SearchData) => {
                        data.index = index - 1;
                      })
                    }
                  >
                    <KeyboardArrowLeftIcon />
                  </IconButton>
                  <IconButton disabled>
                    <MenuIcon />
                  </IconButton>
                  <IconButton
                    disabled={index === count - 1}
                    onClick={() =>
                      updateSearchData((data: SearchData) => {
                        data.index = index + 1;
                      })
                    }
                  >
                    <KeyboardArrowRightIcon />
                  </IconButton>
                </GridLayout>
              </GridLayout>
              <GridBox sx={{ m: 2 }} />
              <GridLayout rows>
                {uiConfig.attributes.map((attribute) => (
                  <GridLayout cols rowAlign="baseline" key={attribute.key}>
                    <Typography variant="body1" sx={{ fontWeight: 700 }}>
                      {attribute.title}:&nbsp;
                    </Typography>
                    <Typography variant="body2">
                      {instance?.data?.[attribute.key]}
                    </Typography>
                  </GridLayout>
                ))}
              </GridLayout>
              <GridBox sx={{ m: 2 }} />
              <GridLayout rows={3}>
                <GridLayout cols rowAlign="middle">
                  <Typography variant="h3">Annotations</Typography>
                  <IconButton
                    onClick={() =>
                      updateSearchData((data) => {
                        data.editingAnnotations = !data?.editingAnnotations;
                      })
                    }
                  >
                    {searchData?.editingAnnotations ? (
                      <LockIcon />
                    ) : (
                      <EditIcon />
                    )}
                  </IconButton>
                  {searchData?.editingAnnotations ? (
                    <IconButton onClick={() => showNewAnnotationDialog()}>
                      <AddIcon />
                    </IconButton>
                  ) : null}
                </GridLayout>
                <Loading status={annotationsState}>
                  <GridLayout rows>
                    {annotationsState.data?.map((a) => (
                      <AnnotationComponent
                        studyId={params.studyId}
                        cohortId={cohort.id}
                        reviewId={reviewId}
                        annotation={a}
                        mutateAnnotation={() => annotationsState.mutate()}
                        instance={instance}
                        key={a.id}
                      />
                    ))}
                  </GridLayout>
                </Loading>
              </GridLayout>
            </GridLayout>
          </GridBox>
          <GridBox
            sx={{
              p: 1,
              backgroundColor: (theme) => theme.palette.background.paper,
              borderLeftStyle: "solid",
              borderColor: (theme) => theme.palette.divider,
              borderWidth: "1px",
            }}
          >
            TODO
          </GridBox>
        </GridLayout>
        {newAnnotationDialog}
      </Loading>
    </GridBox>
  );
}

function AnnotationComponent(props: {
  studyId: string;
  cohortId: string;
  reviewId: string;

  annotation: Annotation;
  instance?: ReviewInstance;

  mutateAnnotation: () => void;
}) {
  const [searchData, updateSearchData] = useSearchData<SearchData>();

  const source = useSource();

  const values = props.instance?.annotations?.[props.annotation.id];
  const currentValue = values?.find((v) => v.current)?.value;
  const latestValue = values?.[values?.length]?.value;

  let comp: ReactNode | undefined;
  switch (props.annotation.annotationType) {
    case AnnotationType.String:
      if (props.annotation.enumVals?.length) {
        comp = (
          <FormControl fullWidth>
            <InputLabel id={`label-${props.annotation.id}`}>
              {props.annotation.displayName}
            </InputLabel>
            <Select
              labelId={`label-${props.annotation.id}`}
              defaultValue={latestValue ?? ""}
              value={currentValue ?? ""}
              label={props.annotation.displayName}
            >
              <MenuItem value="" key="">
                &nbsp;
              </MenuItem>
              {props.annotation.enumVals.map((v) => (
                <MenuItem value={v} key={v}>
                  {v}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        );
      } else {
        comp = (
          <TextField
            variant="outlined"
            fullWidth
            label={props.annotation.displayName}
            value={currentValue}
            defaultValue={latestValue}
          />
        );
      }
      break;

    default:
      throw new Error(
        `Unhandled annotation type ${props.annotation.annotationType}.`
      );
  }

  return (
    <GridLayout cols fillCol={0} rowAlign="middle">
      {comp}
      {searchData?.editingAnnotations ? (
        <IconButton
          onClick={async () => {
            await source.deleteAnnotation(
              props.studyId,
              props.cohortId,
              props.annotation.id
            );
            await props.mutateAnnotation();
            updateSearchData((data) => {
              data.editingAnnotations = false;
            });
          }}
        >
          <DeleteIcon />
        </IconButton>
      ) : null}
    </GridLayout>
  );
}
