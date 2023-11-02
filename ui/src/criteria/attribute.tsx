import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, generateId, registerCriteriaPlugin } from "cohort";
import { HintDataSelect } from "components/hintDataSelect";
import Loading from "components/loading";
import { DataRange, RangeSlider } from "components/rangeSlider";
import { ROLLUP_COUNT_ATTRIBUTE } from "data/configuration";
import { FilterType } from "data/filter";
import { IntegerHint, Source } from "data/source";
import { useSource } from "data/sourceContext";
import { DataEntry, DataValue } from "data/types";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import React, { useCallback, useMemo } from "react";
import useSWRImmutable from "swr/immutable";
import { CriteriaConfig } from "underlaysSlice";

type Selection = {
  value: DataValue;
  name: string;
};

interface Config extends CriteriaConfig {
  attribute: string;
  multiRange?: boolean;
  unit?: string;
}

interface Data {
  // Selected is valid for enum attributes.
  selected: Selection[];

  // dataRanges is valid for integer attributes.
  dataRanges: DataRange[];
}

@registerCriteriaPlugin(
  "attribute",
  (source: Source, c: CriteriaConfig, dataEntry?: DataEntry) => {
    return {
      selected: dataEntry
        ? [{ value: dataEntry.key, name: dataEntry.name }]
        : [],
      dataRanges: [],
    };
  },
  search
)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;
  private config: Config;

  constructor(
    public id: string,
    config: CriteriaConfig,
    data: unknown,
    private entity?: string
  ) {
    this.config = config as Config;
    this.data = data as Data;
  }

  renderInline(groupId: string) {
    return (
      <AttributeInline
        groupId={groupId}
        criteriaId={this.id}
        data={this.data}
        config={this.config}
        entity={this.entity}
      />
    );
  }

  displayDetails() {
    if (this.data.selected.length > 0) {
      return this.data.selected.length === 1
        ? {
            title: this.data.selected[0].name,
          }
        : {
            title: `(${this.data.selected.length} selected)`,
            additionalText:
              this.data.selected.length > 1
                ? this.data.selected.map((s) => s.name)
                : undefined,
          };
    }

    if (this.data.dataRanges.length > 0) {
      const additionalText = [
        this.data.dataRanges.map((r) => `${r.min} - ${r.max}`).join(", "),
      ];
      return this.data.dataRanges.length === 1
        ? {
            title: additionalText[0],
          }
        : {
            title: `(${this.data.dataRanges.length} ranges)`,
            additionalText,
          };
    }

    return {
      title: "(any)",
    };
  }

  generateFilter() {
    return {
      type: FilterType.Attribute,
      attribute: this.config.attribute,
      values: this.data.selected?.map(({ value }) => value),
      ranges: this.data.dataRanges,
    };
  }

  filterOccurrenceIds() {
    return [""];
  }
}

type SliderProps = {
  minBound: number;
  maxBound: number;
  range: DataRange;
  data: Data;
  groupId?: string;
  criteriaId?: string;
  index: number;
  multiRange?: boolean;
  unit?: string;
};

function AttributeSlider(props: SliderProps) {
  const updateCriteria = useUpdateCriteria(props.groupId, props.criteriaId);
  const { data } = props;

  const onUpdate = (
    range: DataRange,
    index: number,
    min: number,
    max: number
  ) => {
    updateCriteria(
      produce(data, (oldData) => {
        if (oldData.dataRanges.length === 0) {
          oldData.dataRanges.push(range);
        }

        oldData.dataRanges[index].min = min;
        oldData.dataRanges[index].max = max;
      })
    );
  };

  const onDelete = (range: DataRange, index: number) => {
    updateCriteria(
      produce(data, (oldData) => {
        oldData.dataRanges.splice(index, 1);
      })
    );
  };

  return (
    <RangeSlider
      minBound={props.minBound}
      maxBound={props.maxBound}
      range={props.range}
      index={props.index}
      multiRange={props.multiRange}
      unit={props.unit}
      onUpdate={onUpdate}
      onDelete={onDelete}
    />
  );
}

type AttributeInlineProps = {
  groupId: string;
  criteriaId: string;
  config: Config;
  data: Data;
  entity?: string;
};

function AttributeInline(props: AttributeInlineProps) {
  const source = useSource();
  const updateCriteria = useUpdateCriteria(props.groupId, props.criteriaId);

  const fetchHintData = useCallback(() => {
    return source.getHintData(props.entity ?? "", props.config.attribute);
  }, [props.config.attribute]);
  const hintDataState = useSWRImmutable(
    { component: "Attribute", attribute: props.config.attribute },
    fetchHintData
  );

  const handleAddRange = useCallback(
    (hint: IntegerHint) => {
      updateCriteria(
        produce(props.data, (data) => {
          data.dataRanges.push({
            id: generateId(),
            ...hint,
          });
        })
      );
    },
    [props.data]
  );

  const emptyRange = useMemo(
    () => ({
      id: generateId(),
      min: Number.MIN_SAFE_INTEGER,
      max: Number.MAX_SAFE_INTEGER,
    }),
    [props.criteriaId]
  );

  const listRanges = () => {
    if (!hintDataState.data?.integerHint) {
      return null;
    }

    if (!props.config.multiRange && props.data.dataRanges.length === 0) {
      return (
        <AttributeSlider
          key={emptyRange.id}
          index={0}
          minBound={hintDataState.data.integerHint.min}
          maxBound={hintDataState.data.integerHint.max}
          range={emptyRange}
          unit={props.config.unit}
          data={props.data}
          groupId={props.groupId}
          criteriaId={props.criteriaId}
        />
      );
    }

    return props.data.dataRanges.map(
      (range, index) =>
        hintDataState.data?.integerHint && (
          <AttributeSlider
            key={range.id}
            index={index}
            minBound={hintDataState.data.integerHint.min}
            maxBound={hintDataState.data.integerHint.max}
            range={range}
            unit={props.config.unit}
            data={props.data}
            groupId={props.groupId}
            criteriaId={props.criteriaId}
          />
        )
    );
  };

  const onSelect = (sel: Selection[]) => {
    updateCriteria(
      produce(props.data, (data) => {
        data.selected = sel;
      })
    );
  };

  return (
    <Loading status={hintDataState}>
      <Box>
        <Stack spacing={1}>{listRanges()}</Stack>
        {props.config.multiRange && (
          <Button
            variant="contained"
            size="large"
            sx={{ mt: 2 }}
            onClick={() =>
              hintDataState.data?.integerHint &&
              handleAddRange(hintDataState.data.integerHint)
            }
          >
            Add Range
          </Button>
        )}
      </Box>

      {!!hintDataState.data?.enumHintOptions && (
        <Box sx={{ maxWidth: 500 }}>
          <HintDataSelect
            hintData={hintDataState.data}
            selected={props.data.selected}
            onSelect={onSelect}
          />
        </Box>
      )}

      {!hintDataState.data && (
        <Typography>
          No information for attribute {props.config.attribute}.
        </Typography>
      )}
    </Loading>
  );
}

async function search(
  source: Source,
  c: CriteriaConfig,
  query: string
): Promise<DataEntry[]> {
  const config = c as Config;

  const hintData = await source.getHintData("", config.attribute);
  if (!hintData?.enumHintOptions) {
    return [];
  }

  const re = new RegExp(query, "i");
  const results: DataEntry[] = [];
  hintData.enumHintOptions.forEach((hint) => {
    const key = hint.value;
    if (
      (typeof key === "string" || typeof key === "number") &&
      hint.name.search(re) >= 0
    ) {
      results.push({
        key,
        name: hint.name,
        [ROLLUP_COUNT_ATTRIBUTE]: hint.count,
      });
    }
  });

  return results.sort(
    (a, b) =>
      (b[ROLLUP_COUNT_ATTRIBUTE] as number) -
      (a[ROLLUP_COUNT_ATTRIBUTE] as number)
  );
}
