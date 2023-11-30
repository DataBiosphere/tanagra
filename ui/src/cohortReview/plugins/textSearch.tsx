import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useCohortReviewContext } from "cohortReview/cohortReviewContext";
import {
  CohortReviewPlugin,
  registerCohortReviewPlugin,
} from "cohortReview/pluginRegistry";
import Checkbox from "components/checkbox";
import { HintDataSelect, Selection } from "components/hintDataSelect";
import { Search } from "components/search";
import { SortDirection, SortOrder } from "data/configuration";
import { DataValue } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { findAll } from "highlight-words-core";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import React, { useMemo } from "react";
import useSWRImmutable from "swr/immutable";
import { CohortReviewPageConfig } from "underlaysSlice";
import { safeRegExp } from "util/safeRegExp";

type Config = {
  occurrence: string;
  title: string;
  text: string;
  subtitles?: string[];
  categoryAttribute?: string;
  sortOrder?: SortOrder;
};

@registerCohortReviewPlugin("textSearch")
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CohortReviewPlugin {
  public occurrences: string[];
  private config: Config;

  constructor(
    public id: string,
    public title: string,
    config: CohortReviewPageConfig
  ) {
    this.config = config.plugin as Config;
    this.occurrences = [this.config.occurrence];
  }

  render() {
    return <TextSearch id={this.id} config={this.config} />;
  }
}

type SearchState = {
  query: string;
  categories?: Selection[];
  context?: boolean;
};

function TextSearch({ id, config }: { id: string; config: Config }) {
  const underlaySource = useUnderlaySource();

  const context = useCohortReviewContext();
  if (!context) {
    return null;
  }

  // TODO(tjennison): Remove random text once notes actually have content.
  const filledOccurrences = useMemo(
    () =>
      context.occurrences[config.occurrence].map((o) => ({
        ...o,
        [config.text]: formatValue(o[config.text]),
      })),
    [context.occurrences]
  );

  const searchState = context.searchState<SearchState>(id);
  const query = searchState?.query ?? "";
  const [regExp] = safeRegExp(query);

  const occurrences = useMemo(
    () =>
      filledOccurrences
        .filter((o) => regExp.test(o[config.text] as string))
        .filter((o) => {
          const ca = config.categoryAttribute;
          if (!ca || !searchState?.categories?.length) {
            return true;
          }
          return searchState.categories.find((s) => s.name === o[ca]);
        })
        .sort((left, right) => {
          const a = config.sortOrder?.attribute;
          if (!a) {
            return 0;
          }
          const l = left[a];
          const r = right[a];

          let ret = 0;
          if (l && r) {
            if (l < r) {
              ret = -1;
            } else if (r < l) {
              ret = 1;
            }
          } else if (!l) {
            ret = -1;
          } else if (!r) {
            ret = 1;
          }
          return config.sortOrder?.direction != SortDirection.Desc ? ret : -ret;
        }),
    [filledOccurrences, searchState]
  );

  const hintDataState = useSWRImmutable(
    {
      type: "hintData",
      occurrence: config.occurrence,
      attribute: config.categoryAttribute,
    },
    async () => {
      return {
        hintData: config.categoryAttribute
          ? await underlaySource.getHintData(
              config.occurrence,
              config?.categoryAttribute
            )
          : undefined,
      };
    }
  );

  const onSearch = (query: string) =>
    context.updateSearchState(id, (state: SearchState) => {
      state.query = query;
    });

  const onSelect = (sel: Selection[]) =>
    context.updateSearchState(id, (state: SearchState) => {
      state.categories = sel;
    });

  const onChangeContext = () =>
    context.updateSearchState(id, (state: SearchState) => {
      state.context = !state.context;
    });

  return (
    <GridLayout rows>
      <GridBox
        sx={{
          p: 2,
          backgroundColor: (theme) => theme.palette.background.paper,
          borderBottomStyle: "solid",
          borderColor: (theme) => theme.palette.divider,
          borderWidth: "1px",
        }}
      >
        <GridLayout rows spacing={1} height="auto">
          <GridLayout cols="1fr 1fr" spacing={2} rowAlign="middle">
            <Search
              initialValue={searchState?.query}
              delayMs={0}
              onSearch={onSearch}
            />
            {config.categoryAttribute ? (
              <HintDataSelect
                hintData={hintDataState.data?.hintData}
                maxChips={2}
                selected={searchState?.categories ?? []}
                onSelect={onSelect}
              />
            ) : null}
          </GridLayout>
          <GridLayout cols rowAlign="middle">
            <Checkbox
              size="small"
              fontSize="small"
              checked={!!searchState?.context}
              onChange={onChangeContext}
            />
            <Typography variant="body2">Show keywords in context</Typography>
          </GridLayout>
        </GridLayout>
      </GridBox>
      <GridBox
        sx={{
          backgroundColor: (theme) => theme.palette.background.default,
          p: 1,
          overflowY: "auto",
        }}
      >
        <Stack spacing={1} alignItems="stretch">
          {occurrences.map((o) => (
            <Paper key={o.key} sx={{ overflow: "hidden" }}>
              <Stack
                alignItems="flex-start"
                sx={{
                  p: 1,
                  backgroundColor: (theme) => theme.palette.divider,
                }}
              >
                <Typography variant="h4">
                  {formatValue(o[config.title])}
                </Typography>
                {config.subtitles?.map((s) => (
                  <Typography key={s} variant="body1">
                    {formatValue(o[s])}
                  </Typography>
                ))}
              </Stack>
              <Box sx={{ p: 1 }}>
                <TextBlock
                  query={query}
                  text={String(o[config.text])}
                  context={searchState?.context}
                />
              </Box>
            </Paper>
          ))}
        </Stack>
      </GridBox>
    </GridLayout>
  );
}

function formatValue(value: DataValue) {
  return value instanceof Date ? value.toDateString() : String(value);
}

type TextBlockProps = {
  text: string;
  query: string;
  context?: boolean;
};

const CONTEXT_LENGTH = 40;

function TextBlock(props: TextBlockProps) {
  const chunks = useMemo(() => {
    return findAll({
      searchWords: [props.query],
      textToHighlight: props.text,
    });
  }, [props.text, props.query]);

  return (
    <>
      {chunks.map(({ start, end, highlight }, i) => {
        const trim = !!props.context && !highlight && chunks.length > 1;

        let text = "";
        if (trim && i === 0) {
          text = "…" + props.text.substring(end - CONTEXT_LENGTH, end);
        } else if (trim && i === chunks.length - 1) {
          text = props.text.substr(start, CONTEXT_LENGTH) + "…";
        } else if (trim && end - start > CONTEXT_LENGTH * 2 + 1) {
          text =
            props.text.substr(start, CONTEXT_LENGTH) +
            "…\n…" +
            props.text.substr(end - CONTEXT_LENGTH, CONTEXT_LENGTH);
        } else {
          text = props.text.substring(start, end);
        }

        return (
          <Typography
            key={i}
            component="span"
            sx={{
              backgroundColor: (theme) =>
                highlight ? theme.highlightColor : undefined,
              whiteSpace: "pre-wrap",
            }}
          >
            {text}
          </Typography>
        );
      })}
    </>
  );
}
