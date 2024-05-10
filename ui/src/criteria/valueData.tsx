import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Typography from "@mui/material/Typography";
import { HintDataSelect } from "components/hintDataSelect";
import Loading from "components/loading";
import { DataRange, RangeSlider } from "components/rangeSlider";
import { FilterType, makeArrayFilter } from "data/filter";
import { dataValueFromProto, HintData, protoFromDataValue } from "data/source";
import { DataKey, DataValue } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import * as dataProto from "proto/criteriaselector/value_data";
import { ReactNode, useMemo } from "react";
import useSWRImmutable from "swr/immutable";
import { isValid } from "util/valid";

export type ValueConfig = {
  attribute: string;
  title: string;
  unit?: string;
};

export type ValueSelection = {
  value: DataValue;
  name: string;
};

export const ANY_VALUE = "t_any";

export type ValueData = {
  attribute: string;
  numeric: boolean;
  selected: ValueSelection[];
  range: DataRange;
};

export const ANY_VALUE_DATA = {
  attribute: ANY_VALUE,
  numeric: false,
  selected: [],
  range: {
    id: "",
    min: 0,
    max: 0,
  },
};

export type ValueDataEditProps = {
  hintEntity: string;
  relatedEntity?: string;
  hintKey?: DataKey;

  singleValue?: boolean;
  valueConfigs: ValueConfig[];
  valueData: ValueData[];
  update: (data: ValueData[]) => void;
};

export function ValueDataEdit(props: ValueDataEditProps) {
  const underlaySource = useUnderlaySource();

  const hintDataState = useSWRImmutable(
    {
      type: "hintData",
      hintEntity: props.hintEntity,
      relatedEntity: props.relatedEntity,
      key: props.hintKey,
    },
    async (key) => {
      const hintData = props.valueConfigs
        ? await underlaySource.getAllHintData(
            key.hintEntity,
            key.relatedEntity,
            key.key
          )
        : undefined;
      return {
        hintData,
      };
    }
  );

  const onSelect = (event: SelectChangeEvent<string>) => {
    const {
      target: { value: sel },
    } = event;
    const attribute =
      props.valueConfigs.find((c) => c.attribute === sel)?.attribute ??
      ANY_VALUE;
    if (props.valueData[0] && attribute === props.valueData[0]?.attribute) {
      return;
    }

    const hintData = hintDataState.data?.hintData?.find(
      (hint) => hint.attribute === attribute
    );

    if (hintData) {
      props.update([defaultValueData(hintData)]);
    } else {
      props.update([ANY_VALUE_DATA]);
    }
  };

  const selectedConfigs = useMemo(() => {
    return props.valueConfigs
      .map((valueConfig) => {
        const hintData = hintDataState.data?.hintData?.find(
          (hint) => hint.attribute === valueConfig.attribute
        );
        if (!hintData) {
          return null;
        }

        let valueData = props.valueData.find(
          (data) => data.attribute === valueConfig.attribute
        );
        if (props.singleValue && !valueData) {
          return null;
        }
        if (!valueData) {
          valueData = defaultValueData(hintData);
        }

        return {
          valueConfig,
          valueData,
          hintData,
        };
      })
      .filter(isValid);
  }, [
    props.singleValue,
    props.valueConfigs,
    props.valueData,
    hintDataState.data,
  ]);

  const onValueSelect = (sel: ValueSelection[], valueData: ValueData) => {
    props.update(
      produce(props.valueData, (data) => {
        if (!props.singleValue && sel.length === 0) {
          return data.filter(
            (vd) =>
              vd.attribute != valueData.attribute && vd.attribute != ANY_VALUE
          );
        }

        const existing = data.find(
          (vd) =>
            vd.attribute === valueData.attribute || vd.attribute === ANY_VALUE
        );
        if (existing) {
          existing.attribute = valueData.attribute;
          existing.selected = sel;
        } else {
          valueData.selected = sel;
          data.push(valueData);
        }
      })
    );
  };

  const onUpdateRange = (
    range: DataRange,
    index: number,
    min: number,
    max: number,
    valueData: ValueData
  ) => {
    props.update(
      produce(props.valueData, (data) => {
        const hintData = selectedConfigs.find(
          (c) => c.hintData.attribute === valueData.attribute
        )?.hintData;
        if (
          !props.singleValue &&
          hintData?.integerHint &&
          hintData.integerHint.min === min &&
          hintData.integerHint.max === max
        ) {
          return data.filter(
            (vd) =>
              vd.attribute != valueData.attribute && vd.attribute != ANY_VALUE
          );
        }

        const existing = data.find(
          (vd) =>
            vd.attribute === valueData.attribute || vd.attribute === ANY_VALUE
        );
        if (existing) {
          existing.attribute = valueData.attribute;
          existing.range.min = min;
          existing.range.max = max;
        } else {
          valueData.range.min = min;
          valueData.range.max = max;
          data.push(valueData);
        }
      })
    );
  };

  return (
    <Loading status={hintDataState}>
      {hintDataState.data?.hintData?.length ? (
        <GridLayout
          cols={!!props.singleValue ? true : undefined}
          rows={!props.singleValue ? true : undefined}
          height="auto"
        >
          {!!props.valueData.length && props.singleValue ? (
            <FormControl
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
              }}
            >
              <Select
                value={props.valueData[0].attribute}
                input={<OutlinedInput />}
                disabled={!hintDataState.data?.hintData?.length}
                onChange={onSelect}
              >
                <MenuItem key={ANY_VALUE} value={ANY_VALUE}>
                  Any value
                </MenuItem>
                {props.valueConfigs?.map((c) =>
                  hintDataState.data?.hintData?.find(
                    (hint) => hint.attribute === c.attribute
                  ) ? (
                    <MenuItem key={c.attribute} value={c.attribute}>
                      {c.title}
                    </MenuItem>
                  ) : null
                )}
              </Select>
            </FormControl>
          ) : null}
          {selectedConfigs.map((c, i) => {
            let component: ReactNode = null;
            if (c.hintData.enumHintOptions) {
              component = (
                <HintDataSelect
                  key={c.valueConfig.attribute}
                  hintData={c.hintData}
                  selected={c.valueData.selected}
                  onSelect={(sel) => onValueSelect(sel, c.valueData)}
                />
              );
            }
            if (c.hintData.integerHint) {
              component = (
                <RangeSlider
                  key={c.valueConfig.attribute}
                  index={0}
                  minBound={c.hintData.integerHint.min}
                  maxBound={c.hintData.integerHint.max}
                  range={c.valueData.range}
                  unit={c.valueConfig.unit}
                  onUpdate={(range, index, min, max) =>
                    onUpdateRange(range, index, min, max, c.valueData)
                  }
                />
              );
            }
            if (!component || props.singleValue) {
              return component;
            }

            return (
              <GridLayout key={c.valueData.attribute} rows height="auto">
                {i !== 0 ? (
                  <Divider variant="middle">
                    <Chip label="AND" />
                  </Divider>
                ) : null}
                <GridLayout cols rowAlign="middle" spacing={3} height="auto">
                  {!!c.valueConfig.title ? (
                    <Typography variant="body1">
                      {c.valueConfig.title}
                    </Typography>
                  ) : null}
                  {component}
                </GridLayout>
              </GridLayout>
            );
          })}
          <GridBox />
        </GridLayout>
      ) : null}
    </Loading>
  );
}

export function generateValueDataFilter(valueData: ValueData[]) {
  if (!valueData.length || valueData[0].attribute === ANY_VALUE) {
    return {
      type: FilterType.Attribute,
      attribute: "id",
      nonNull: true,
    };
  }

  return makeArrayFilter(
    {},
    valueData.map((vd) => ({
      type: FilterType.Attribute,
      attribute: vd.attribute,
      values: !vd.numeric ? vd.selected.map((s) => s.value) : undefined,
      ranges: vd.numeric ? [vd.range] : undefined,
    }))
  );
}

export function decodeValueData(valueData?: dataProto.ValueData): ValueData {
  if (!valueData || !valueData.range) {
    throw new Error(`Invalid value data proto ${JSON.stringify(valueData)}`);
  }

  return {
    attribute: valueData.attribute,
    numeric: valueData.numeric,
    selected:
      valueData.selected?.map((s) => ({
        value: dataValueFromProto(s.value),
        name: s.name,
      })) ?? [],
    range: {
      id: valueData.range.id,
      min: valueData.range.min,
      max: valueData.range.max,
    },
  };
}

export function encodeValueData(valueData: ValueData): dataProto.ValueData {
  return {
    attribute: valueData.attribute,
    numeric: valueData.numeric,
    selected: valueData.selected.map((s) => ({
      value: protoFromDataValue(s.value),
      name: s.name,
    })),
    range: {
      id: valueData.range.id,
      min: valueData.range.min,
      max: valueData.range.max,
    },
  };
}

function defaultValueData(hintData: HintData): ValueData {
  return {
    attribute: hintData.attribute,
    numeric: !!hintData?.integerHint,
    selected: [],
    range: {
      id: "",
      min: hintData?.integerHint?.min ?? Number.MIN_SAFE_INTEGER,
      max: hintData?.integerHint?.max ?? Number.MAX_SAFE_INTEGER,
    },
  };
}
