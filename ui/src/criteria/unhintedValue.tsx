import FormControl from "@mui/material/FormControl";
import Input from "@mui/material/Input";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import { CommonSelectorConfig } from "data/source";
import { ComparisonOperator } from "data/types";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import GridLayout from "layout/gridLayout";
import * as configProto from "proto/criteriaselector/configschema/unhinted_value";
import * as dataProto from "proto/criteriaselector/dataschema/unhinted_value";
import React, { useCallback, useMemo, useState } from "react";
import { base64ToBytes } from "util/base64";

interface Data {
  operator: ComparisonOperator;
  min: number;
  max: number;
}

const operatorTitles = {
  [ComparisonOperator.Equal]: "Equals",
  [ComparisonOperator.LessThanEqual]: "Less than or equals",
  [ComparisonOperator.GreaterThanEqual]: "Greater than or equals",
  [ComparisonOperator.Between]: "Between",
};

@registerCriteriaPlugin("unhinted-value", () => {
  return encodeData({
    operator: ComparisonOperator.GreaterThanEqual,
    min: 1,
    max: 10,
  });
})
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<string> {
  public data: string;
  private selector: CommonSelectorConfig;
  private config: configProto.UnhintedValue;

  constructor(
    public id: string,
    selector: CommonSelectorConfig,
    data: string,
    private entity?: string
  ) {
    this.selector = selector;
    this.config = decodeConfig(selector);
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
    const decodedData = decodeData(this.data);

    let title = `${operatorTitles[decodedData.operator]} ${decodedData.min}`;
    if (decodedData.operator === ComparisonOperator.Between) {
      title = `${operatorTitles[decodedData.operator]} ${decodedData.min} and ${
        decodedData.max
      }`;
    }

    return {
      title,
    };
  }

  filterEntityIds() {
    return Object.keys(this.config.attributes);
  }
}

type UnhintedValueInlineProps = {
  groupId: string;
  criteriaId: string;
  config: configProto.UnhintedValue;
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
    ComparisonOperator.Equal,
    ComparisonOperator.LessThanEqual,
    ComparisonOperator.GreaterThanEqual,
    ...(!props.config.groupByCount ? [ComparisonOperator.Between] : []),
  ];

  const onSelectOperator = (event: SelectChangeEvent<string>) => {
    const {
      target: { value: sel },
    } = event;
    updateCriteria(
      produce(decodedData, (data) => {
        data.operator = sel as ComparisonOperator;
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
        onMouseUp={(e) => {
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
          onClick={(e) => e.stopPropagation()}
          inputProps={{
            type: "number",
          }}
        />
        {decodedData.operator === ComparisonOperator.Between ? (
          <GridLayout cols rowAlign="middle" height="auto">
            <Typography variant="body1">&nbsp;and&nbsp;</Typography>
            <Input
              value={maxInputValue}
              size="medium"
              onChange={handleMaxInputChange}
              onBlur={handleMaxInputBlur}
              onClick={(e) => e.stopPropagation()}
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
): ComparisonOperator {
  switch (operator) {
    case dataProto.UnhintedValue_ComparisonOperator.COMPARISON_OPERATOR_UNKNOWN:
    case dataProto.UnhintedValue_ComparisonOperator.COMPARISON_OPERATOR_EQUAL:
      return ComparisonOperator.Equal;
    case dataProto.UnhintedValue_ComparisonOperator.COMPARISON_OPERATOR_BETWEEN:
      return ComparisonOperator.Between;
    case dataProto.UnhintedValue_ComparisonOperator
      .COMPARISON_OPERATOR_LESS_THAN_EQUAL:
      return ComparisonOperator.LessThanEqual;
    case dataProto.UnhintedValue_ComparisonOperator
      .COMPARISON_OPERATOR_GREATER_THAN_EQUAL:
      return ComparisonOperator.GreaterThanEqual;
  }
  throw new Error(`Unknown comparison operator value ${operator}.`);
}

function decodeData(data: string): Data {
  const message =
    data[0] === "{"
      ? dataProto.UnhintedValue.fromJSON(JSON.parse(data))
      : dataProto.UnhintedValue.decode(base64ToBytes(data));

  return {
    operator: decodeComparisonOperator(message.operator),
    min: message.min,
    max: message.max,
  };
}

function encodeComparisonOperator(
  operator: ComparisonOperator
): dataProto.UnhintedValue_ComparisonOperator {
  switch (operator) {
    case ComparisonOperator.Equal:
      return dataProto.UnhintedValue_ComparisonOperator
        .COMPARISON_OPERATOR_EQUAL;
    case ComparisonOperator.Between:
      return dataProto.UnhintedValue_ComparisonOperator
        .COMPARISON_OPERATOR_BETWEEN;
    case ComparisonOperator.LessThanEqual:
      return dataProto.UnhintedValue_ComparisonOperator
        .COMPARISON_OPERATOR_LESS_THAN_EQUAL;
    case ComparisonOperator.GreaterThanEqual:
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
  return JSON.stringify(dataProto.UnhintedValue.toJSON(message));
}

function decodeConfig(
  selector: CommonSelectorConfig
): configProto.UnhintedValue {
  return configProto.UnhintedValue.fromJSON(JSON.parse(selector.pluginConfig));
}
