import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import KeyboardArrowLeftIcon from "@mui/icons-material/KeyboardArrowLeft";
import KeyboardArrowRightIcon from "@mui/icons-material/KeyboardArrowRight";
import LockIcon from "@mui/icons-material/Lock";
import MenuIcon from "@mui/icons-material/Menu";
import TabContext from "@mui/lab/TabContext";
import TabList from "@mui/lab/TabList";
import TabPanel from "@mui/lab/TabPanel";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Tab from "@mui/material/Tab";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { CohortReviewContext } from "cohortReview/cohortReviewContext";
import { useNewAnnotationDialog } from "cohortReview/newAnnotationDialog";
import { useParticipantsListDialog } from "cohortReview/participantsList";
import { getCohortReviewPlugin } from "cohortReview/pluginRegistry";
import {
  SearchData,
  useReviewAnnotations,
  useReviewInstances,
  useReviewParams,
  useReviewSearchData,
} from "cohortReview/reviewHooks";
import Loading from "components/loading";
import { FilterType } from "data/filter";
import {
  Annotation,
  AnnotationType,
  ReviewInstance,
  useSource,
} from "data/source";
import { DataEntry, DataValue } from "data/types";
import { useUnderlay } from "hooks";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { ReactNode, useMemo, useState } from "react";
import { absoluteCohortReviewListURL, useBaseParams } from "router";
import useSWR from "swr";
import useSWRImmutable from "swr/immutable";

export function CohortReview() {
  const source = useSource();
  const underlay = useUnderlay();
  const baseParams = useBaseParams();
  const params = useReviewParams();

  const primaryKey = underlay.uiConfiguration.dataConfig.primaryEntity.key;
  const uiConfig = underlay.uiConfiguration.cohortReviewConfig;

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
      return await source.getCohortReview(
        key.studyId,
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
        return await source.listReviewInstances(
          params.studyId,
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

  const [newAnnotationDialog, showNewAnnotationDialog] = useNewAnnotationDialog(
    {
      onCreate: async (
        displayName: string,
        annotationType: AnnotationType,
        enumVals?: string[]
      ) => {
        await source.createAnnotation(
          params.studyId,
          params.cohort.id,
          displayName,
          annotationType,
          enumVals
        );
        annotationsState.mutate();
      },
    }
  );

  const [searchData, updateSearchData] = useReviewSearchData();

  const instanceIndex = searchData.instanceIndex ?? 0;
  const instance = instancesState.data?.[instanceIndex];
  const count = instancesState.data?.length ?? 1;

  const pageId = searchData.pageId ?? pagePlugins[0].id;

  const changePage = (event: React.SyntheticEvent, newValue: string) => {
    updateSearchData((data) => {
      data.pageId = newValue;
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

      const occurrenceIds: string[] = [];
      pagePlugins.forEach((p) => occurrenceIds.push(...p.occurrences));

      const res = await Promise.all(
        occurrenceIds.map((id) => {
          return source.listData(
            source.listAttributes(id),
            id,
            {
              type: FilterType.Attribute,
              occurrenceId: "",
              attribute: "id",
              values: [instance?.data?.[primaryKey]],
            },
            null,
            10000
          );
        })
      );

      const occurrences: { [x: string]: DataEntry[] } = {};
      res.forEach(
        (r, i) =>
          (occurrences[occurrenceIds[i]] = r.data.map((o) => ({
            ...o,
            timestamp: o["start_date"] as Date,
          })))
      );
      return {
        occurrences,
      };
    }
  );

  const [participantsListDialog, showParticipantsListDialog] =
    useParticipantsListDialog({ count });

  return (
    <GridBox>
      <ActionBar
        title={
          reviewState?.data?.displayName
            ? `Review "${reviewState?.data?.displayName}"`
            : ""
        }
        backURL={absoluteCohortReviewListURL(
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
                  {instance?.data?.[primaryKey]}
                </Typography>
                <Typography variant="body1">
                  {instanceIndex + 1}/{count}
                </Typography>
                <GridLayout cols>
                  <IconButton
                    disabled={instanceIndex === 0}
                    onClick={() =>
                      updateSearchData((data: SearchData) => {
                        data.instanceIndex = instanceIndex - 1;
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
                      updateSearchData((data: SearchData) => {
                        data.instanceIndex = instanceIndex + 1;
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
                      {instance?.data?.[attribute.key]}
                    </Typography>
                  </GridLayout>
                ))}
              </GridLayout>
              <GridBox sx={{ m: 2 }} />
              <GridLayout rows={3}>
                <GridLayout cols rowAlign="middle">
                  <Typography variant="body1em">Annotations</Typography>
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
                    {annotationsState.data?.map(
                      (a) =>
                        !!instance && (
                          <AnnotationComponent
                            studyId={params.studyId}
                            cohortId={params.cohort.id}
                            reviewId={params.reviewId}
                            annotation={a}
                            mutateAnnotation={() => annotationsState.mutate()}
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
                  occurrences: instanceDataState?.data?.occurrences ?? {},
                  searchData: <T extends object>(plugin: string) =>
                    searchData?.plugins?.[plugin] as T,
                  updateSearchData: <T extends object>(
                    plugin: string,
                    fn: (value: T) => void
                  ) =>
                    updateSearchData((data) => {
                      data.plugins = data.plugins ?? {};
                      data.plugins[plugin] = produce(
                        data.plugins[plugin] ?? {},
                        fn
                      );
                    }),
                }}
              >
                <TabContext value={pageId}>
                  <GridLayout rows>
                    <TabList onChange={changePage}>
                      {pagePlugins.map((p) => (
                        <Tab key={p.id} label={p.title} value={p.id} />
                      ))}
                    </TabList>
                    <GridBox
                      sx={{
                        borderTopStyle: "solid",
                        borderColor: (theme) => theme.palette.divider,
                        borderWidth: "1px",
                      }}
                    >
                      {pagePlugins.map((p) => (
                        <TabPanel
                          key={p.id}
                          value={p.id}
                          sx={{ p: 0, height: "100%" }}
                        >
                          {p.render()}
                        </TabPanel>
                      ))}
                    </GridBox>
                  </GridLayout>
                </TabContext>
              </CohortReviewContext.Provider>
            </Loading>
          </GridBox>
        </GridLayout>
        {newAnnotationDialog}
        {participantsListDialog}
      </Loading>
    </GridBox>
  );
}

function AnnotationComponent(props: {
  studyId: string;
  cohortId: string;
  reviewId: string;

  annotation: Annotation;
  instance: ReviewInstance;

  mutateAnnotation: () => void;
  mutateInstance: (
    updateRemote: () => void,
    updateLocal: (instance: ReviewInstance) => void
  ) => void;
}) {
  const [searchData, updateSearchData] = useReviewSearchData();

  const source = useSource();

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
          await source.deleteAnnotationValue(
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
          await source.createUpdateAnnotationValue(
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

  let comp: ReactNode | undefined;
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

        comp = (
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
        comp = (
          <TextField
            variant="outlined"
            fullWidth
            label={props.annotation.displayName}
            value={text}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
              setText(event.target.value)
            }
            onBlur={() => updateValue(text)}
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
