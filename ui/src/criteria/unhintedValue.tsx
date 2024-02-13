import FormControl from "@mui/material/FormControl";
import Input from "@mui/material/Input";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import { FilterType } from "data/filter";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import GridLayout from "layout/gridLayout";
import * as dataProto from "proto/criteriaselector/dataschema/unhinted_value";
import React, { useCallback, useMemo, useState } from "react";
import * as tanagraUI from "tanagra-ui";
import { CriteriaConfig } from "underlaysSlice";
import { base64ToBytes, bytesToBase64 } from "util/base64";

interface Config extends CriteriaConfig {
  groupByCount?: boolean;
  attribute: string;
}

interface Data {
  operator: tanagraUI.UIComparisonOperator;
  min: number;
  max: number;
}

function rangeFromData(data: Data) {
  if (data.operator === tanagraUI.UIComparisonOperator.Equal) {
    return { min: data.min, max: data.min };
  }

  if (data.operator === tanagraUI.UIComparisonOperator.Between) {
    return { min: data.min, max: data.max };
  }

  return {
    min:
      data.operator === tanagraUI.UIComparisonOperator.LessThanEqual
        ? Number.MIN_SAFE_INTEGER
        : data.min,
    max:
      data.operator === tanagraUI.UIComparisonOperator.GreaterThanEqual
        ? Number.MAX_SAFE_INTEGER
        : data.min,
  };
}

@registerCriteriaPlugin("unhinted-value", () => {
  return encodeData({
    operator: tanagraUI.UIComparisonOperator.GreaterThanEqual,
    min: 1,
    max: 10,
  });
})
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
      <UnhintedValueInline
        groupId={groupId}
        criteriaId={this.id}
        data={this.data}
        config={this.config}
        entity={this.entity}
      />
    );
  }

  displayDetails() {
    return {
      title: "",
    };
  }

  generateFilter() {
    const decodedData = decodeData(this.data);

    if (this.config.groupByCount) {
      return null;
    }

    return {
      type: FilterType.Attribute,
      attribute: this.config.attribute,
      ranges: [rangeFromData(decodedData)],
    };
  }

  groupByCountFilter() {
    const decodedData = decodeData(this.data);

    if (!this.config.groupByCount) {
      return null;
    }

    return {
      attribute: this.config.attribute,
      operator: decodedData.operator,
      value: decodedData.min,
    };
  }

  filterEntityIds() {
    return [this.config.attribute ?? ""];
  }
}

type UnhintedValueInlineProps = {
  groupId: string;
  criteriaId: string;
  config: Config;
  data: string;
  entity?: string;
};

function UnhintedValueInline(props: UnhintedValueInlineProps) {
  const updateEncodedCriteria = useUpdateCriteria(
    props.groupId,
    props.criteriaId
  );
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);

  const [minInputValue, setMinInputValue] = useState(String(decodedData.min));
  const [maxInputValue, setMaxInputValue] = useState(String(decodedData.max));
  const [minValue, setMinValue] = useState(String(decodedData.max));
  const [maxValue, setMaxValue] = useState(String(decodedData.max));

  // Make sure empty input won't get changed.
  // TODO(tjennison): Consider if this can be refactored along with the code in
  // criteria/attribute.tsx.
  const handleMinInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setMinInputValue(event.target.value);

    const newMin = event.target.value === "" ? 0 : Number(event.target.value);
    setMinValue(String(newMin));

    updateCriteria(
      produce(decodedData, (data) => {
        data.min = newMin;
      })
    );
  };
  const handleMinInputBlur = () => {
    setMinInputValue(String(minValue));
  };

  const handleMaxInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setMaxInputValue(event.target.value);

    const newMax = event.target.value === "" ? 0 : Number(event.target.value);
    setMaxValue(String(newMax));

    updateCriteria(
      produce(decodedData, (data) => {
        data.max = newMax;
      })
    );
  };
  const handleMaxInputBlur = () => {
    setMaxInputValue(String(maxValue));
  };

  const operatorOptions = [
    tanagraUI.UIComparisonOperator.Equal,
    tanagraUI.UIComparisonOperator.LessThanEqual,
    tanagraUI.UIComparisonOperator.GreaterThanEqual,
    ...(!props.config.groupByCount
      ? [tanagraUI.UIComparisonOperator.Between]
      : []),
  ];

  const operatorTitles = {
    [tanagraUI.UIComparisonOperator.Equal]: "Equals",
    [tanagraUI.UIComparisonOperator.LessThanEqual]: "Less than or equals",
    [tanagraUI.UIComparisonOperator.GreaterThanEqual]: "Greater than or equals",
    [tanagraUI.UIComparisonOperator.Between]: "Between",
  };

  const onSelectOperator = (event: SelectChangeEvent<string>) => {
    const {
      target: { value: sel },
    } = event;
    updateCriteria(
      produce(decodedData, (data) => {
        data.operator = sel as tanagraUI.UIComparisonOperator;
      })
    );
  };

  return (
    <GridLayout rows height="auto">
      <FormControl
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
        }}
      >
        <Select
          value={decodedData.operator}
          input={<OutlinedInput />}
          onChange={onSelectOperator}
        >
          {operatorOptions.map((o) => (
            <MenuItem key={o} value={o}>
              {operatorTitles[o]}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      <GridLayout cols height="auto">
        <Input
          value={minInputValue}
          size="medium"
          onChange={handleMinInputChange}
          onBlur={handleMinInputBlur}
          inputProps={{
            type: "number",
          }}
        />
        {decodedData.operator === tanagraUI.UIComparisonOperator.Between ? (
          <GridLayout cols rowAlign="middle" height="auto">
            <Typography variant="body1">&nbsp;and&nbsp;</Typography>
            <Input
              value={maxInputValue}
              size="medium"
              onChange={handleMaxInputChange}
              onBlur={handleMaxInputBlur}
              inputProps={{
                type: "number",
              }}
            />
          </GridLayout>
        ) : null}
      </GridLayout>
    </GridLayout>
  );
}

function decodeComparisonOperator(
  operator: dataProto.UnhintedValue_ComparisonOperator
): tanagraUI.UIComparisonOperator {
  switch (operator) {
    case dataProto.UnhintedValue_ComparisonOperator.COMPARISON_OPERATOR_UNKNOWN:
    case dataProto.UnhintedValue_ComparisonOperator.COMPARISON_OPERATOR_EQUAL:
      return tanagraUI.UIComparisonOperator.Equal;
    case dataProto.UnhintedValue_ComparisonOperator.COMPARISON_OPERATOR_BETWEEN:
      return tanagraUI.UIComparisonOperator.Between;
    case dataProto.UnhintedValue_ComparisonOperator
      .COMPARISON_OPERATOR_LESS_THAN_EQUAL:
      return tanagraUI.UIComparisonOperator.LessThanEqual;
    case dataProto.UnhintedValue_ComparisonOperator
      .COMPARISON_OPERATOR_GREATER_THAN_EQUAL:
      return tanagraUI.UIComparisonOperator.GreaterThanEqual;
  }
  throw new Error(`Unknown comparison operator value ${operator}.`);
}

function decodeData(data: string): Data {
  const message = dataProto.UnhintedValue.decode(base64ToBytes(data));
  return {
    operator: decodeComparisonOperator(message.operator),
    min: message.min,
    max: message.max,
  };
}

function encodeComparisonOperator(
  operator: tanagraUI.UIComparisonOperator
): dataProto.UnhintedValue_ComparisonOperator {
  switch (operator) {
    case tanagraUI.UIComparisonOperator.Equal:
      return dataProto.UnhintedValue_ComparisonOperator
        .COMPARISON_OPERATOR_EQUAL;
    case tanagraUI.UIComparisonOperator.Between:
      return dataProto.UnhintedValue_ComparisonOperator
        .COMPARISON_OPERATOR_BETWEEN;
    case tanagraUI.UIComparisonOperator.LessThanEqual:
      return dataProto.UnhintedValue_ComparisonOperator
        .COMPARISON_OPERATOR_LESS_THAN_EQUAL;
    case tanagraUI.UIComparisonOperator.GreaterThanEqual:
      return dataProto.UnhintedValue_ComparisonOperator
        .COMPARISON_OPERATOR_GREATER_THAN_EQUAL;
  }
  throw new Error(`Unknown internal comparison operator value ${operator}.`);
}

function encodeData(data: Data): string {
  const message: dataProto.UnhintedValue = {
    operator: encodeComparisonOperator(data.operator),
    min: data.min,
    max: data.max,
  };
  return bytesToBase64(dataProto.UnhintedValue.encode(message).finish());
}
