import KeyboardArrowLeftIcon from "@mui/icons-material/KeyboardArrowLeft";
import KeyboardArrowRightIcon from "@mui/icons-material/KeyboardArrowRight";
import MenuIcon from "@mui/icons-material/Menu";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { CohortReviewContext } from "cohortReview/cohortReviewContext";
import { useParticipantsListDialog } from "cohortReview/participantsList";
import { getCohortReviewPlugin } from "cohortReview/pluginRegistry";
import {
  SearchState,
  useReviewAnnotations,
  useReviewInstances,
  useReviewParams,
  useReviewSearchState,
} from "cohortReview/reviewHooks";
import Loading from "components/loading";
import { Tabs } from "components/tabs";
import { Annotation, AnnotationType, ReviewInstance } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { DataEntry, DataValue, stringifyDataValue } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useUnderlay } from "hooks";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useMemo, useState } from "react";
import { absoluteCohortReviewListURL, useBaseParams } from "router";
import useSWR from "swr";
import useSWRImmutable from "swr/immutable";

export function CohortReview() {
  const studySource = useStudySource();
  const underlaySource = useUnderlaySource();
  const underlay = useUnderlay();
  const baseParams = useBaseParams();
  const params = useReviewParams();

  const primaryKey = underlaySource.primaryEntity().idAttribute;
  const uiConfig = underlay.uiConfiguration.cohortReviewConfig;
  const participantIdAttribute = uiConfig.participantIdAttribute ?? primaryKey;

  const pagePlugins = useMemo(
    () => uiConfig.pages.map((p) => getCohortReviewPlugin(p)),
    [uiConfig.pages]
  );

  const reviewState = useSWR(
    {
      type: "review",
      studyId: params.studyId,
      reviewId: params.reviewId,
      cohortId: params.cohort.id,
    },
    async (key) => {
      return await studySource.getCohortReview(
        key.studyId,
        underlaySource,
        key.cohortId,
        key.reviewId
      );
    }
  );

  const instancesState = useReviewInstances();

  const mutateInstance = (
    updateRemote: () => void,
    updateLocal: (instance: ReviewInstance) => void
  ) => {
    if (!instancesState.data) {
      return;
    }

    const data = produce(instancesState.data, (data) => {
      const instance = data?.[instanceIndex];
      if (instance) {
        updateLocal(data?.[instanceIndex]);
      }
    });
    instancesState.mutate(
      async () => {
        await updateRemote();
        return await studySource.listReviewInstances(
          params.studyId,
          underlaySource,
          params.cohort.id,
          params.reviewId,
          params.primaryAttributes
        );
      },
      {
        optimisticData: data,
        populateCache: true,
        revalidate: false,
      }
    );
  };

  const annotationsState = useReviewAnnotations();
  const [searchState, updateSearchState] = useReviewSearchState();

  const instanceIndex = searchState.instanceIndex ?? 0;
  const instance = instancesState.data?.[instanceIndex];
  const count = instancesState.data?.length ?? 1;

  const pageId = searchState.pageId ?? pagePlugins[0].id;

  const changePage = (newValue: string) => {
    updateSearchState((state) => {
      state.pageId = newValue;
    });
  };

  const instanceDataState = useSWRImmutable(
    {
      type: "reviewInstanceData",
      studyId: params.studyId,
      cohortId: params.cohort.id,
      reviewId: params.reviewId,
      instanceIndex,
    },
    async () => {
      if (!instance?.data) {
        throw new Error("Instances not loaded yet.");
      }

      const entityIds: string[] = [];
      pagePlugins.forEach((p) => entityIds.push(...p.entities));

      const res = await Promise.all(
        entityIds.map((id) =>
          underlaySource.listDataForPrimaryEntity(
            underlaySource.listAttributes(id),
            id,
            instance?.data?.[primaryKey]
          )
        )
      );

      const rows: { [x: string]: DataEntry[] } = {};
      res.forEach(
        (r, i) =>
          (rows[entityIds[i]] = r.data.map((o) => ({
            ...o,
            timestamp: o["start_date"] as Date,
          })))
      );
      return {
        rows,
      };
    }
  );

  const [participantsListDialog, showParticipantsListDialog] =
    useParticipantsListDialog({ count });

  return (
    <GridLayout rows>
      <ActionBar
        title={
          reviewState?.data?.displayName
            ? `Review "${reviewState?.data?.displayName}"`
            : ""
        }
        backAction={absoluteCohortReviewListURL(
          baseParams,
          params.cohort.id,
          params.reviewId
        )}
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
                <Typography variant="body1em">Participant</Typography>
                <Typography variant="body1em">
                  {instance?.data?.[participantIdAttribute] ??
                    instance?.data?.[primaryKey]}
                </Typography>
                <Typography variant="body1">
                  {instanceIndex + 1}/{count}
                </Typography>
                <GridLayout cols>
                  <IconButton
                    disabled={instanceIndex === 0}
                    onClick={() =>
                      updateSearchState((state: SearchState) => {
                        state.instanceIndex = instanceIndex - 1;
                      })
                    }
                  >
                    <KeyboardArrowLeftIcon />
                  </IconButton>
                  <IconButton disabled={count === 0}>
                    <MenuIcon onClick={() => showParticipantsListDialog()} />
                  </IconButton>
                  <IconButton
                    disabled={instanceIndex === count - 1}
                    onClick={() =>
                      updateSearchState((state: SearchState) => {
                        state.instanceIndex = instanceIndex + 1;
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
                    <Typography variant="body2em" sx={{ fontWeight: 700 }}>
                      {attribute.title}:&nbsp;
                    </Typography>
                    <Typography variant="body2">
                      {stringifyDataValue(instance?.data?.[attribute.key])}
                    </Typography>
                  </GridLayout>
                ))}
              </GridLayout>
              <GridBox sx={{ m: 2 }} />
              <GridLayout rows={3} spacing={2}>
                <Typography variant="body1em">Annotations</Typography>
                <Loading status={annotationsState}>
                  <GridLayout rows spacing={2}>
                    {annotationsState.data?.map(
                      (a) =>
                        !!instance && (
                          <AnnotationComponent
                            studyId={params.studyId}
                            cohortId={params.cohort.id}
                            reviewId={params.reviewId}
                            annotation={a}
                            instance={instance}
                            mutateInstance={mutateInstance}
                            key={`${a.id}-${instance?.data?.key}`}
                          />
                        )
                    )}
                  </GridLayout>
                </Loading>
              </GridLayout>
            </GridLayout>
          </GridBox>
          <GridBox
            sx={{
              backgroundColor: (theme) => theme.palette.background.paper,
              borderLeftStyle: "solid",
              borderColor: (theme) => theme.palette.divider,
              borderWidth: "1px",
            }}
          >
            <Loading status={instanceDataState}>
              <CohortReviewContext.Provider
                value={{
                  rows: instanceDataState?.data?.rows ?? {},
                  searchState: <T extends object>(plugin: string) =>
                    (searchState?.plugins?.[plugin] ?? {}) as T,
                  updateSearchState: <T extends object>(
                    plugin: string,
                    fn: (value: T) => void
                  ) =>
                    updateSearchState((state) => {
                      state.plugins = state.plugins ?? {};
                      state.plugins[plugin] = produce(
                        state.plugins[plugin] ?? {},
                        fn
                      );
                    }),
                }}
              >
                <Tabs
                  configs={pagePlugins}
                  currentTab={pageId}
                  setCurrentTab={changePage}
                />
              </CohortReviewContext.Provider>
            </Loading>
          </GridBox>
        </GridLayout>
        {participantsListDialog}
      </Loading>
    </GridLayout>
  );
}

function AnnotationComponent(props: {
  studyId: string;
  cohortId: string;
  reviewId: string;

  annotation: Annotation;
  instance: ReviewInstance;

  mutateInstance: (
    updateRemote: () => void,
    updateLocal: (instance: ReviewInstance) => void
  ) => void;
}) {
  const studySource = useStudySource();

  // TODO(tjennison): Expand handling of older and newer revisions and improve
  // their UI once the API is updated.
  const values = props.instance?.annotations?.[props.annotation.id];
  const currentValue = values?.find((v) => v.current)?.value;
  const latestValue = values?.[values?.length]?.value;

  const [text, setText] = useState<string>(
    !!currentValue ? String(currentValue) : ""
  );

  const updateValue = (value: DataValue) => {
    if (!value) {
      if (!currentValue) {
        return;
      }

      props.mutateInstance(
        async () =>
          await studySource.deleteAnnotationValue(
            props.studyId,
            props.cohortId,
            props.reviewId,
            props.annotation.id,
            props.instance.data.key
          ),
        (instance: ReviewInstance) => {
          instance.annotations[props.annotation.id] = instance.annotations[
            props.annotation.id
          ]?.filter((v) => !v.current);
        }
      );
    } else {
      props.mutateInstance(
        async () =>
          await studySource.createUpdateAnnotationValue(
            props.studyId,
            props.cohortId,
            props.reviewId,
            props.annotation.id,
            props.instance.data.key,
            value
          ),
        (instance: ReviewInstance) =>
          createUpdateCurrentValue(instance, props.annotation.id, value)
      );
    }
  };

  switch (props.annotation.annotationType) {
    case AnnotationType.String:
      if (props.annotation.enumVals?.length) {
        const currentIndex =
          (props.annotation.enumVals?.indexOf(String(currentValue)) ?? -1) + 1;
        const latestIndex =
          (props.annotation.enumVals?.indexOf(String(latestValue)) ?? -1) + 1;

        const onSelect = (event: SelectChangeEvent<string>) => {
          const {
            target: { value: sel },
          } = event;
          updateValue(
            !!sel ? props.annotation.enumVals?.[Number(sel) - 1] ?? null : null
          );
        };

        return (
          <FormControl fullWidth>
            <InputLabel id={`label-${props.annotation.id}`}>
              {props.annotation.displayName}
            </InputLabel>
            <Select
              labelId={`label-${props.annotation.id}`}
              defaultValue={String(latestIndex)}
              value={String(currentIndex)}
              label={props.annotation.displayName}
              onChange={onSelect}
            >
              <MenuItem value={0} key="">
                &nbsp;
              </MenuItem>
              {props.annotation.enumVals.map((v, i) => (
                <MenuItem value={i + 1} key={v}>
                  {v}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        );
      } else {
        return (
          <TextField
            variant="outlined"
            fullWidth
            multiline
            minRows={1}
            maxRows={4}
            label={props.annotation.displayName}
            value={text}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
              setText(event.target.value)
            }
            onBlur={() => updateValue(text)}
          />
        );
      }

    default:
      throw new Error(
        `Unhandled annotation type ${props.annotation.annotationType}.`
      );
  }
}

function createUpdateCurrentValue(
  instance: ReviewInstance,
  annotationId: string,
  value: DataValue
) {
  const annotations = instance.annotations;
  const cur = annotations[annotationId]?.find((v) => v.current);
  if (cur) {
    cur.value = value;
  } else {
    annotations[annotationId] = [
      ...(annotations[annotationId] ?? []),
      {
        current: true,
        instanceId: instance.data.key,
        value: value,
      },
    ];
  }
}
