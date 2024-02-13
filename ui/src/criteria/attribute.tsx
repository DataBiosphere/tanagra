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
import {
  dataValueFromProto,
  IntegerHint,
  protoFromDataValue,
  UnderlaySource,
} from "data/source";
import { DataEntry, DataValue } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import * as dataProto from "proto/criteriaselector/dataschema/attribute";
import React, { useCallback, useMemo } from "react";
import useSWRImmutable from "swr/immutable";
import { CriteriaConfig } from "underlaysSlice";
import { base64ToBytes, bytesToBase64 } from "util/base64";
import { safeRegExp } from "util/safeRegExp";

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
  (
    underlaySource: UnderlaySource,
    c: CriteriaConfig,
    dataEntry?: DataEntry
  ) => {
    return encodeData({
      selected: dataEntry
        ? [{ value: dataEntry.key, name: dataEntry.name as string }]
        : [],
      dataRanges: [],
    });
  },
  search
)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<string> {
  public data: string;
  private config: Config;

  constructor(
    public id: string,
    config: CriteriaConfig,
    data: string,
    private entity?: string
  ) {
    this.config = config as Config;
    try {
      this.data = encodeData(JSON.parse(data));
    } catch (e) {
      this.data = data;
    }
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
    const decodedData = decodeData(this.data);

    if (decodedData.selected.length > 0) {
      return decodedData.selected.length === 1
        ? {
            title: decodedData.selected[0].name,
          }
        : {
            title: `(${decodedData.selected.length} selected)`,
            additionalText:
              decodedData.selected.length > 1
                ? decodedData.selected.map((s) => s.name)
                : undefined,
          };
    }

    if (decodedData.dataRanges.length > 0) {
      const additionalText = [
        decodedData.dataRanges.map((r) => `${r.min} - ${r.max}`).join(", "),
      ];
      return decodedData.dataRanges.length === 1
        ? {
            title: additionalText[0],
          }
        : {
            title: `(${decodedData.dataRanges.length} ranges)`,
            additionalText,
          };
    }

    return {
      title: "(any)",
    };
  }

  generateFilter() {
    const decodedData = decodeData(this.data);

    return {
      type: FilterType.Attribute,
      attribute: this.config.attribute,
      values: decodedData.selected?.map(({ value }) => value),
      ranges: decodedData.dataRanges,
    };
  }

  filterEntityIds() {
    return [""];
  }
}

type SliderProps = {
  minBound: number;
  maxBound: number;
  range: DataRange;
  data: string;
  groupId?: string;
  criteriaId?: string;
  index: number;
  multiRange?: boolean;
  unit?: string;
};

function AttributeSlider(props: SliderProps) {
  const updateEncodedCriteria = useUpdateCriteria(
    props.groupId,
    props.criteriaId
  );
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);

  const onUpdate = (
    range: DataRange,
    index: number,
    min: number,
    max: number
  ) => {
    updateCriteria(
      produce(decodedData, (oldData) => {
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
      produce(decodedData, (oldData) => {
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
  data: string;
  entity?: string;
};

function AttributeInline(props: AttributeInlineProps) {
  const underlaySource = useUnderlaySource();
  const updateEncodedCriteria = useUpdateCriteria(
    props.groupId,
    props.criteriaId
  );
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);

  const entity = underlaySource.lookupEntity(props.entity ?? "");
  const attribute = entity.attributes.find(
    (a) => a.name === props.config.attribute
  );
  if (!attribute) {
    throw new Error(
      `Attribute ${props.config.attribute} not found in "${entity.name}`
    );
  }

  const fetchHintData = useCallback(() => {
    return underlaySource.getHintData(entity.name, props.config.attribute);
  }, [props.config.attribute]);
  const hintDataState = useSWRImmutable(
    { component: "Attribute", attribute: props.config.attribute },
    fetchHintData
  );

  const handleAddRange = useCallback(
    (hint: IntegerHint) => {
      updateCriteria(
        produce(decodedData, (data) => {
          data.dataRanges.push({
            id: generateId(),
            ...hint,
          });
        })
      );
    },
    [decodedData]
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

    if (!props.config.multiRange && decodedData.dataRanges.length === 0) {
      return (
        <AttributeSlider
          key={emptyRange.id}
          index={0}
          minBound={
            attribute.displayHintRangeMin ?? hintDataState.data.integerHint.min
          }
          maxBound={
            attribute.displayHintRangeMax ?? hintDataState.data.integerHint.max
          }
          range={emptyRange}
          unit={props.config.unit}
          data={props.data}
          groupId={props.groupId}
          criteriaId={props.criteriaId}
        />
      );
    }

    return decodedData.dataRanges.map(
      (range, index) =>
        hintDataState.data?.integerHint && (
          <AttributeSlider
            key={range.id}
            index={index}
            minBound={
              attribute.displayHintRangeMin ??
              hintDataState.data.integerHint.min
            }
            maxBound={
              attribute.displayHintRangeMax ??
              hintDataState.data.integerHint.max
            }
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
      produce(decodedData, (data) => {
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
            selected={decodedData.selected}
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
  underlaySource: UnderlaySource,
  c: CriteriaConfig,
  query: string
): Promise<DataEntry[]> {
  const config = c as Config;

  const hintData = await underlaySource.getHintData("", config.attribute);
  if (!hintData?.enumHintOptions) {
    return [];
  }

  const [re] = safeRegExp(query);
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

function decodeData(data: string): Data {
  const message = dataProto.Attribute.decode(base64ToBytes(data));
  return {
    selected:
      message.selected?.map((s) => ({
        value: dataValueFromProto(s.value),
        name: s.name,
      })) ?? [],
    dataRanges:
      message.dataRanges?.map((r) => ({
        id: r.id,
        min: r.min,
        max: r.max,
      })) ?? [],
  };
}

function encodeData(data: Data): string {
  const message: dataProto.Attribute = {
    selected:
      data.selected?.map((s) => ({
        value: protoFromDataValue(s.value),
        name: s.name,
      })) ?? [],
    dataRanges:
      data.dataRanges?.map((r) => ({
        id: r.id,
        min: r.min,
        max: r.max,
      })) ?? [],
  };
  return bytesToBase64(dataProto.Attribute.encode(message).finish());
}
