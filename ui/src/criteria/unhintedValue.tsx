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
import React, { useState } from "react";
import * as tanagraUI from "tanagra-ui";
import { CriteriaConfig } from "underlaysSlice";

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
  return {
    operator: tanagraUI.UIComparisonOperator.GreaterThanEqual,
    min: 1,
    max: 10,
  };
})
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
    if (this.config.groupByCount) {
      return null;
    }

    return {
      type: FilterType.Attribute,
      attribute: this.config.attribute,
      ranges: [rangeFromData(this.data)],
    };
  }

  groupByCountFilter() {
    if (!this.config.groupByCount) {
      return null;
    }

    return {
      attribute: this.config.attribute,
      operator: this.data.operator,
      value: this.data.min,
    };
  }

  filterOccurrenceIds() {
    return [this.config.attribute ?? ""];
  }
}

type UnhintedValueInlineProps = {
  groupId: string;
  criteriaId: string;
  config: Config;
  data: Data;
  entity?: string;
};

function UnhintedValueInline(props: UnhintedValueInlineProps) {
  const updateCriteria = useUpdateCriteria(props.groupId, props.criteriaId);

  const [minInputValue, setMinInputValue] = useState(String(props.data.min));
  const [maxInputValue, setMaxInputValue] = useState(String(props.data.max));
  const [minValue, setMinValue] = useState(String(props.data.max));
  const [maxValue, setMaxValue] = useState(String(props.data.max));

  // Make sure empty input won't get changed.
  // TODO(tjennison): Consider if this can be refactored along with the code in
  // criteria/attribute.tsx.
  const handleMinInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setMinInputValue(event.target.value);

    const newMin = event.target.value === "" ? 0 : Number(event.target.value);
    setMinValue(String(newMin));

    updateCriteria(
      produce(props.data, (data) => {
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
      produce(props.data, (data) => {
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

  const onSelectOperator = (event: SelectChangeEvent<string>) => {
    const {
      target: { value: sel },
    } = event;
    updateCriteria(
      produce(props.data, (data) => {
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
          value={props.data.operator}
          input={<OutlinedInput />}
          onChange={onSelectOperator}
        >
          {operatorOptions.map((o) => (
            <MenuItem key={o} value={o}>
              {o}
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
        {props.data.operator === tanagraUI.UIComparisonOperator.Between ? (
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
