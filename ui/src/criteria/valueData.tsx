import FilterListIcon from "@mui/icons-material/FilterList";
import FilterListOffIcon from "@mui/icons-material/FilterListOff";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Typography from "@mui/material/Typography";
import Checkbox from "components/checkbox";
import { HintDataSelect } from "components/hintDataSelect";
import Loading from "components/loading";
import { DataRange, RangeSlider } from "components/rangeSlider";
import { dataValueFromProto, HintData, protoFromDataValue } from "data/source";
import { DataKey, DataValue } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import * as dataProto from "proto/criteriaselector/value_data";
import { ReactNode, useMemo } from "react";
import useSWRImmutable from "swr/immutable";
import * as underlayConfig from "tanagra-underlay/underlayConfig";
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
  // If hintData is supplied it will be used instead of fetching it.
  hintData?: HintData[];

  relatedEntity?: string;
  hintKey?: DataKey;

  singleValue?: boolean;
  valueConfigs: ValueConfig[];

  // The list of ValueData is optional so callers aren't responsible for
  // providing a default value, which is awkward for multiselect when a list is
  // required.
  valueData?: ValueData[];
  update: (data?: ValueData[]) => void;

  // If a title is set, it's assumed that the value data is part of a list and
  // the UI is configured accordingly.
  title?: string;
};

export function ValueDataEdit(props: ValueDataEditProps) {
  const underlaySource = useUnderlaySource();

  const entity = underlaySource.lookupEntity(props.hintEntity);

  const valueDataList = useMemo((): ValueData[] => {
    if (props.valueData) {
      return props.valueData;
    }

    return props.singleValue ? [ANY_VALUE_DATA] : [];
  }, [props.valueData]);

  const hintDataState = useSWRImmutable(
    {
      type: "hintData",
      hintEntity: props.hintEntity,
      relatedEntity: props.relatedEntity,
      key: props.hintKey,
    },
    async (key) => {
      const hintData =
        props.hintData ??
        (props.valueConfigs
          ? await underlaySource.getAllHintData(
              key.hintEntity,
              key.relatedEntity,
              key.key
            )
          : undefined);
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
    if (valueDataList[0] && attribute === valueDataList[0]?.attribute) {
      return;
    }

    const hintData = hintDataState.data?.hintData?.find(
      (hint) => hint.attribute === attribute
    );

    const entityAttribute = entity.attributes.find((a) => a.name === attribute);

    if (hintData) {
      props.update([defaultValueData(hintData, entityAttribute)]);
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

        const attribute = entity.attributes.find(
          (a) => a.name === hintData.attribute
        );

        let valueData = valueDataList.find(
          (data) => data.attribute === valueConfig.attribute
        );
        if (props.singleValue && !valueData) {
          return null;
        }
        if (!valueData) {
          valueData = defaultValueData(hintData, attribute);
        }

        return {
          valueConfig,
          valueData,
          hintData,
          attribute,
        };
      })
      .filter(isValid);
  }, [
    props.singleValue,
    props.valueConfigs,
    valueDataList,
    hintDataState.data,
    entity,
  ]);

  const onValueSelect = (sel: ValueSelection[], valueData: ValueData) => {
    props.update(
      produce(valueDataList, (data) => {
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
      produce(valueDataList, (data) => {
        const config = selectedConfigs.find(
          (c) => c.hintData.attribute === valueData.attribute
        );

        if (
          !props.singleValue &&
          config?.hintData?.integerHint &&
          minBound(config?.attribute, config?.hintData) === min &&
          maxBound(config?.attribute, config?.hintData) === max
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

  const hasHints = !!hintDataState.data?.hintData?.length;

  // Use two Loading components so that the big spinner will only be shown for
  // single select value data but the controls are hidden in both cases.
  return (
    <GridLayout rows height="auto">
      {props.title ? (
        <GridLayout cols rowAlign="middle">
          <Typography variant="body2">{props.title}</Typography>
          <Loading status={hintDataState} size="small">
            {hasHints ? (
              <GridBox
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                }}
                onMouseUp={(e) => {
                  e.stopPropagation();
                }}
              >
                <Checkbox
                  size="small"
                  fontSize="inherit"
                  checked={!!props.valueData}
                  checkedIcon={<FilterListIcon />}
                  uncheckedIcon={<FilterListOffIcon />}
                  onChange={() =>
                    props.update(props.valueData ? undefined : [ANY_VALUE_DATA])
                  }
                />
              </GridBox>
            ) : null}
          </Loading>
          <GridBox />
        </GridLayout>
      ) : null}
      <Loading status={hintDataState} noProgress={!!props.title}>
        {hasHints && (!props.title || isValid(props.valueData)) ? (
          <GridLayout rows height="auto">
            {!!valueDataList.length && props.singleValue ? (
              <FormControl
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                }}
                onMouseUp={(e) => {
                  e.stopPropagation();
                }}
              >
                <Select
                  value={valueDataList[0].attribute}
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
                    minBound={minBound(c.attribute, c.hintData)}
                    maxBound={maxBound(c.attribute, c.hintData)}
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
    </GridLayout>
  );
}

export function decodeValueDataOptional(
  valueData?: dataProto.ValueData
): ValueData | undefined {
  if (!valueData || !valueData.range) {
    return undefined;
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

export function decodeValueData(valueData?: dataProto.ValueData): ValueData {
  const decoded = decodeValueDataOptional(valueData);
  if (!decoded) {
    throw new Error(`Invalid value data proto ${JSON.stringify(valueData)}`);
  }
  return decoded;
}

export function encodeValueDataOptional(
  valueData?: ValueData
): dataProto.ValueData | undefined {
  if (!valueData) {
    return undefined;
  }

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

export function encodeValueData(valueData: ValueData): dataProto.ValueData {
  const encoded = encodeValueDataOptional(valueData);
  if (!encoded) {
    throw new Error(`Invalid value data ${JSON.stringify(valueData)}`);
  }
  return encoded;
}

function defaultValueData(
  hintData: HintData,
  attribute?: underlayConfig.SZAttribute
): ValueData {
  return {
    attribute: hintData.attribute,
    numeric: !!hintData?.integerHint,
    selected: [],
    range: {
      id: "",
      min: minBound(attribute, hintData),
      max: maxBound(attribute, hintData),
    },
  };
}

function minBound(attribute?: underlayConfig.SZAttribute, hintData?: HintData) {
  return (
    attribute?.displayHintRangeMin ??
    hintData?.integerHint?.min ??
    Number.MIN_SAFE_INTEGER
  );
}

function maxBound(attribute?: underlayConfig.SZAttribute, hintData?: HintData) {
  return (
    attribute?.displayHintRangeMax ??
    hintData?.integerHint?.max ??
    Number.MAX_SAFE_INTEGER
  );
}
