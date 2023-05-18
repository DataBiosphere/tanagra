import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useCohortReviewContext } from "cohortReview/cohortReviewContext";
import {
  CohortReviewPlugin,
  registerCohortReviewPlugin,
} from "cohortReview/pluginRegistry";
import { HintDataSelect, Selection } from "components/hintDataSelect";
import { Search } from "components/search";
import { SortDirection, SortOrder } from "data/configuration";
import { useSource } from "data/source";
import { DataValue } from "data/types";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import React, { PropsWithChildren, useMemo } from "react";
import Highlighter from "react-highlight-words";
import useSWRImmutable from "swr/immutable";
import { CohortReviewPageConfig } from "underlaysSlice";

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

const fillerText = [
  "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec ex leo, placerat sit amet purus sit amet, malesuada sagittis magna. Donec pretium justo non justo fringilla, non efficitur magna consequat. Nunc efficitur velit at neque interdum rutrum. Morbi viverra at erat a mollis. Nulla et erat finibus neque posuere volutpat id et enim. Ut pretium eget ante id suscipit. Fusce gravida eleifend orci, sed convallis turpis lobortis sed. Morbi in ex consequat tortor porta egestas. Proin maximus augue id nisi iaculis faucibus. Morbi quis arcu id felis consequat pretium in in velit. Sed euismod augue interdum, volutpat ante eget, pellentesque lacus. Donec elementum vestibulum pulvinar. Morbi at porta turpis. Donec eget malesuada massa, vitae ultrices neque. Donec pharetra, ante nec efficitur semper, turpis turpis dignissim tortor, vel hendrerit ante erat ut diam. Aliquam quis augue id felis sollicitudin vestibulum.",
  "Vestibulum cursus velit leo, at congue odio aliquam a. Aenean leo dui, finibus ac mollis sit amet, pretium vitae ex. Aliquam scelerisque rhoncus justo, sed fringilla justo pellentesque at. Cras sed erat congue, molestie tellus in, dictum diam. Integer pellentesque a mauris euismod ullamcorper. Fusce non tempus quam, nec convallis risus. Sed est orci, tincidunt at dolor eget, luctus rutrum nisl.",
  "Etiam semper magna enim, nec cursus nisl imperdiet in. Pellentesque dapibus arcu nisl, hendrerit condimentum justo mollis vitae. Nunc feugiat tempus erat, nec convallis erat efficitur non. Aliquam ornare eros in nisi malesuada, non hendrerit odio facilisis. Donec bibendum enim eu turpis scelerisque efficitur. Phasellus id feugiat orci. Nunc scelerisque ligula vitae neque porta, porttitor pretium ex dictum. Proin accumsan, orci ullamcorper vulputate sagittis, nibh lectus fringilla dui, eu placerat metus eros in ante. Quisque non sem vitae tortor feugiat sagittis. Vestibulum feugiat lacus arcu. Aliquam erat volutpat. Sed sit amet mauris vitae quam pharetra euismod ut eu ligula. Mauris nisl elit, gravida vel libero at, efficitur tristique orci. Sed nec sapien eget libero porta auctor id ac enim. Etiam molestie libero non molestie gravida. Vestibulum sit amet lorem vitae nunc maximus ultrices eget eget quam.",
  "Phasellus ac porttitor nibh. Proin est eros, faucibus in arcu gravida, tempus ultrices purus. Proin et magna accumsan, pharetra lacus in, porta dui. Integer iaculis lorem vel erat imperdiet, vel fermentum felis tincidunt. Integer vestibulum dictum turpis, nec aliquet nisl fringilla non. Curabitur quam quam, posuere ac sollicitudin sed, rhoncus ut nisl. Fusce a luctus odio. Nunc non lacus sit amet leo tempus finibus. Donec sed tincidunt arcu, dignissim pellentesque dui. Aliquam quam sapien, sollicitudin vel sem at, feugiat euismod lacus. Integer laoreet velit non lorem pretium condimentum. Mauris pulvinar et sapien sit amet sollicitudin. Fusce at tincidunt turpis, vitae tempus nisi. Etiam non malesuada leo, nec vestibulum ipsum. Mauris nec risus a sapien consequat fringilla.",
  "Aliquam ligula felis, porttitor id pulvinar non, condimentum et justo. Sed efficitur mauris metus, id dictum urna sagittis nec. Aliquam nec placerat eros, quis suscipit mauris. Phasellus eget nisl in arcu posuere hendrerit non vel enim. Pellentesque blandit nec dui sed pellentesque. Proin fermentum, elit placerat tempus viverra, eros risus consequat ante, in placerat augue nunc ut elit. Integer eget malesuada erat, at facilisis tellus. Mauris eleifend ante id massa placerat auctor. Donec et felis ornare sem lacinia viverra vitae a tellus. Phasellus eu porttitor elit. Donec eget tristique dolor. Ut cursus velit a nibh faucibus, quis feugiat erat imperdiet. Duis nec facilisis orci. Etiam porttitor magna a mauris accumsan, et iaculis mi accumsan. Curabitur mollis placerat augue, vitae egestas elit sodales sed.",
];

type SearchData = {
  query: string;
  categories?: Selection[];
};

function TextSearch({ id, config }: { id: string; config: Config }) {
  const source = useSource();

  const context = useCohortReviewContext();
  if (!context) {
    return null;
  }

  // TODO(tjennison): Remove random text once notes actually have content.
  const filledOccurrences = useMemo(
    () =>
      context.occurrences[config.occurrence].map((o) => ({
        ...o,
        [config.text]: formatValue(
          o[config.text] ??
            fillerText[Math.floor(Math.random() * fillerText.length)]
        ),
      })),
    [context.occurrences]
  );

  const searchData = context.searchData<SearchData>(id);
  const regExp = new RegExp(searchData?.query ?? "", "i");

  const occurrences = useMemo(
    () =>
      filledOccurrences
        .filter((o) => regExp.test(o[config.text] as string))
        .filter((o) => {
          const ca = config.categoryAttribute;
          if (!ca || !searchData?.categories?.length) {
            return true;
          }
          return searchData.categories.find((s) => s.name === o[ca]);
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
    [filledOccurrences, searchData]
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
          ? await source.getHintData(
              config.occurrence,
              config?.categoryAttribute
            )
          : undefined,
      };
    }
  );

  const onSearch = (query: string) =>
    context.updateSearchData(id, (data: SearchData) => {
      data.query = query;
    });

  const onSelect = (sel: Selection[]) =>
    context.updateSearchData(id, (data: SearchData) => {
      data.categories = sel;
    });

  return (
    <GridLayout rows>
      <GridBox
        sx={{
          backgroundColor: (theme) => theme.palette.background.paper,
          borderBottomStyle: "solid",
          borderColor: (theme) => theme.palette.divider,
          borderWidth: "1px",
        }}
      >
        <GridLayout cols="1fr 1fr" rowAlign="middle">
          <Search
            initialValue={searchData?.query}
            delayMs={0}
            onSearch={onSearch}
          />
          {config.categoryAttribute ? (
            <HintDataSelect
              hintData={hintDataState.data?.hintData}
              maxChips={2}
              selected={searchData?.categories ?? []}
              onSelect={onSelect}
            />
          ) : null}
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
                <Highlighter
                  searchWords={[regExp]}
                  textToHighlight={String(o[config.text])}
                  unhighlightTag={Unhighlighted}
                  highlightTag={Highlighted}
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

function Unhighlighted(props: PropsWithChildren<object>) {
  return <Typography component="span">{props.children}</Typography>;
}

function Highlighted(props: PropsWithChildren<{ highlightIndex: number }>) {
  return (
    <Typography
      component="span"
      sx={{ backgroundColor: (theme) => theme.highlightColor }}
    >
      {props.children}
    </Typography>
  );
}
