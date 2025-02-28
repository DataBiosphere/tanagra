import { DataRange } from "components/rangeSlider";
import { dataValueFromProto, HintData, protoFromDataValue } from "data/source";
import { DataValue } from "data/types";
import * as dataProto from "proto/criteriaselector/value_data";
import * as underlayConfig from "tanagra-underlay/underlayConfig";

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

export function defaultValueData(
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

export function minBound(
  attribute?: underlayConfig.SZAttribute,
  hintData?: HintData
) {
  return (
    attribute?.displayHintRangeMin ??
    hintData?.integerHint?.min ??
    Number.MIN_SAFE_INTEGER
  );
}

export function maxBound(
  attribute?: underlayConfig.SZAttribute,
  hintData?: HintData
) {
  return (
    attribute?.displayHintRangeMax ??
    hintData?.integerHint?.max ??
    Number.MAX_SAFE_INTEGER
  );
}
