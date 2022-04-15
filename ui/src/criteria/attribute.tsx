import { Checkbox, FormControlLabel, ListItem, Slider } from "@mui/material";
import Box from "@mui/material/Box";
import Grid from "@mui/material/Grid";
import Input from "@mui/material/Input";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import produce from "immer";
import React from "react";
import * as tanagra from "tanagra-api";
import { CriteriaConfig } from "underlaysSlice";
import { useUnderlay } from "../hooks";
import { Underlay } from "../underlaysSlice";
import { isValid } from "../util/valid";

type Selection = {
  id: number;
  name: string;
};

interface Config extends CriteriaConfig {
  attribute: string;
}

interface Data extends Config {
  selected: Selection[];
  min: number | undefined;
  max: number | undefined;
}

type AttributeEditProps = {
  dispatchFn: (data: Data) => void;
  data: Data;
};

@registerCriteriaPlugin(
  "attribute",
  (underlay: Underlay, config: CriteriaConfig) => {
    const data = config.plugin as Config;
    const attributeFilterHint = underlay.entities
      .find((g) => g.name === underlay.primaryEntity)
      ?.attributes?.find(
        (attribute) => attribute.name === data.attribute
      )?.attributeFilterHint;
    const integerBoundsHint = attributeFilterHint?.integerBoundsHint;
    const minBound = integerBoundsHint?.min;
    const maxBound = integerBoundsHint?.max;

    return {
      ...(config.plugin as Config),
      selected: [],
      min: minBound,
      max: maxBound,
    };
  }
)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;

  constructor(public id: string, data: unknown) {
    this.data = data as Data;
  }

  renderEdit(dispatchFn: (data: Data) => void) {
    return <AttributeEdit dispatchFn={dispatchFn} data={this.data} />;
  }

  renderDetails() {
    return <ConceptDetails data={this.data} />;
  }

  generateFilter() {
    let operands: tanagra.Filter[] | undefined;
    if (isValid(this.data.min) && isValid(this.data.max)) {
      operands = [
        {
          binaryFilter: {
            attributeVariable: {
              variable: "person",
              name: this.data.attribute,
            },
            operator: tanagra.BinaryFilterOperator.LessThan,
            attributeValue: {
              int64Val: this.data.max,
            },
          },
        },
        {
          binaryFilter: {
            attributeVariable: {
              variable: "person",
              name: this.data.attribute,
            },
            operator: tanagra.BinaryFilterOperator.GreaterThan,
            attributeValue: {
              int64Val: this.data.min,
            },
          },
        },
      ];

      return {
        arrayFilter: {
          operands: operands,
          operator: tanagra.ArrayFilterOperator.And,
        },
      };
    } else if (this.data.selected.length >= 0) {
      operands = this.data.selected.map(({ id }) => ({
        binaryFilter: {
          attributeVariable: {
            variable: "person",
            name: this.data.attribute,
          },
          operator: tanagra.BinaryFilterOperator.Equals,
          attributeValue: {
            int64Val: id,
          },
        },
      }));

      return {
        arrayFilter: {
          operands: operands,
          operator: tanagra.ArrayFilterOperator.Or,
        },
      };
    } else {
      return null;
    }
  }

  occurrenceEntities() {
    return [];
  }
}

function AttributeEdit(props: AttributeEditProps) {
  const underlay = useUnderlay();
  const hintDisplayName = (hint: tanagra.EnumHintValue) =>
    hint.displayName || "Unknown Value";

  const attributeFilterHint = underlay.entities
    .find((g) => g.name === underlay.primaryEntity)
    ?.attributes?.find(
      (attribute) => attribute.name === props.data.attribute
    )?.attributeFilterHint;
  const enumHintValues = attributeFilterHint?.enumHint?.enumHintValues;
  const integerBoundsHint = attributeFilterHint?.integerBoundsHint;

  console.log(underlay);
  console.log(attributeFilterHint);

  if (!isValid(integerBoundsHint) && enumHintValues?.length === 0) {
    return (
      <Typography>
        No information for attribute {props.data.attribute}.
      </Typography>
    );
  }

  if (
    integerBoundsHint?.max !== null &&
    integerBoundsHint?.max !== undefined &&
    integerBoundsHint?.min !== null &&
    integerBoundsHint?.min !== undefined
  ) {
    const minBound = integerBoundsHint?.min;
    const maxBound = integerBoundsHint?.max;

    const [minInputValue, setMinInputValue] = React.useState<number>(minBound);
    const [maxInputValue, setMaxInputValue] = React.useState<number>(maxBound);
    const [minValue, setMinValue] = React.useState<number>(minBound);
    const [maxValue, setMaxValue] = React.useState<number>(maxBound);

    const updateValue = (newMin: number, newMax: number) => {
      setMinValue(newMin);
      setMaxValue(newMax);
      props.dispatchFn(
        produce(props.data, (data) => {
          data.min = newMin;
          data.max = newMax;
        })
      );
    };

    const handleChange = (event: Event, newValue: number | number[]) => {
      newValue = newValue as number[];
      const [newMin, newMax] = newValue;
      setMinInputValue(newMin);
      setMaxInputValue(newMax);
      updateValue(newMin, newMax);
    };

    const handleLeftInputChange = (
      event: React.ChangeEvent<HTMLInputElement>
    ) => {
      let newMin =
        event.target.value === "" ? minBound : Number(event.target.value);
      setMinInputValue(newMin);

      newMin = newMin < minBound ? minBound : newMin;
      newMin = newMin > maxValue ? maxValue : newMin;
      updateValue(newMin, maxValue);
    };
    const handleLeftInputBlur = () => {
      let newMin = minInputValue;
      newMin = newMin < minBound ? minBound : newMin;
      newMin = newMin > maxValue ? maxValue : newMin;
      setMinInputValue(newMin);
    };

    const handleRightInputChange = (
      event: React.ChangeEvent<HTMLInputElement>
    ) => {
      let newMax =
        event.target.value === "" ? maxBound : Number(event.target.value);
      setMaxInputValue(newMax);

      newMax = newMax < minValue ? minValue : newMax;
      newMax = newMax > maxBound ? maxBound : newMax;
      updateValue(minValue, newMax);
    };
    const handleRightInputBlur = () => {
      let newMax = maxInputValue;
      newMax = newMax < minValue ? minValue : newMax;
      newMax = newMax > maxBound ? maxBound : newMax;
      setMaxInputValue(newMax);
    };

    return (
      <Box sx={{ width: "30%", minWidth: 100 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item>
            <Input
              value={minInputValue}
              size="small"
              onChange={handleLeftInputChange}
              onBlur={handleLeftInputBlur}
              inputProps={{
                step: ((maxBound - minBound) / 20) | 0,
                min: minBound,
                max: maxBound,
                type: "number",
                "aria-labelledby": "input-slider",
              }}
            />
          </Grid>
          <Grid item xs>
            <Slider
              getAriaLabel={() => "Year at birth range"}
              value={[minValue, maxValue]}
              onChange={handleChange}
              valueLabelDisplay="auto"
              getAriaValueText={(value) => value.toString()}
              min={minBound}
              max={maxBound}
              disableSwap
            />
          </Grid>
          <Grid item>
            <Input
              value={maxInputValue}
              size="small"
              onChange={handleRightInputChange}
              onBlur={handleRightInputBlur}
              inputProps={{
                step: ((maxBound - minBound) / 20) | 0,
                min: minBound,
                max: maxBound,
                type: "number",
                "aria-labelledby": "input-slider",
              }}
            />
          </Grid>
        </Grid>
      </Box>
    );
  }

  const selectionIndex = (hint: tanagra.EnumHintValue) =>
    props.data.selected.findIndex(
      (row) => row.id === hint.attributeValue?.int64Val
    );

  return (
    <>
      {enumHintValues?.map((hint: tanagra.EnumHintValue) => (
        <ListItem key={hintDisplayName(hint)}>
          <FormControlLabel
            label={hintDisplayName(hint)}
            control={
              <Checkbox
                size="small"
                checked={selectionIndex(hint) > -1}
                onChange={() => {
                  props.dispatchFn(
                    produce(props.data, (data) => {
                      if (selectionIndex(hint) > -1) {
                        data.selected.splice(selectionIndex(hint), 1);
                      } else {
                        data.selected.push({
                          id: hint.attributeValue?.int64Val || -1,
                          name: hintDisplayName(hint),
                        });
                      }
                    })
                  );
                }}
              />
            }
          />
        </ListItem>
      ))}
    </>
  );
}

type ConceptDetailsProps = {
  data: Data;
};

function ConceptDetails(props: ConceptDetailsProps) {
  return (
    <>
      {props.data.selected.length === 0 &&
      !isValid(props.data.min) &&
      !isValid(props.data.max) ? (
        <Typography variant="body1">None selected</Typography>
      ) : isValid(props.data.min) && isValid(props.data.max) ? (
        <Stack direction="row" alignItems="baseline">
          <Typography variant="body1">
            Current {props.data.attribute} in Range {props.data.min} to{" "}
            {props.data.max}
          </Typography>
        </Stack>
      ) : (
        props.data.selected.map(({ id, name }) => (
          <Stack direction="row" alignItems="baseline" key={id}>
            <Typography variant="body1">{id}</Typography>&nbsp;
            <Typography variant="body2">{name}</Typography>
          </Stack>
        ))
      )}
    </>
  );
}
