import { Checkbox, FormControlLabel, ListItem, Slider } from "@mui/material";
import Box from "@mui/material/Box";
import Grid from "@mui/material/Grid";
import Input from "@mui/material/Input";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import { useUnderlay } from "hooks";
import produce from "immer";
import React, { useState } from "react";
import * as tanagra from "tanagra-api";
import { CriteriaConfig, Underlay } from "underlaysSlice";
import { isValid } from "util/valid";

type Selection = {
  id: number;
  name: string;
};

interface Config extends CriteriaConfig {
  attribute: string;
}

interface Data extends Config {
  //selected is valid for enum attributes
  selected: Selection[];

  //min/max are valid for integer attributes
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

    return {
      ...data,
      selected: !integerBoundsHint ? [] : undefined,
      min: integerBoundsHint?.min,
      max: integerBoundsHint?.max,
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
    return <AttributeDetails data={this.data} />;
  }

  generateFilter() {
    if (isValid(this.data.min) && isValid(this.data.max)) {
      return {
        arrayFilter: {
          operands: [
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
          ],
          operator: tanagra.ArrayFilterOperator.And,
        },
      };
    } else if (this.data.selected.length >= 0) {
      return {
        arrayFilter: {
          operands: this.data.selected.map(({ id }) => ({
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
          })),
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

  const hasValidIntegerBounds =
    isValid(integerBoundsHint?.min) && isValid(integerBoundsHint?.max);

  if (hasValidIntegerBounds) {
    const minBound = integerBoundsHint?.min || 0; // This is to ensure the compiler won't complain the object might be undefined.
    const maxBound = integerBoundsHint?.max || 0; // although we already know that min and max is valid.

    // two sets of values are needed due to the input box and slider is isolated.
    const [minInputValue, setMinInputValue] = useState<number | string>(
      props.data.min || minBound
    );
    const [maxInputValue, setMaxInputValue] = useState<number | string>(
      props.data.max || maxBound
    );

    const updateValue = (newMin: number, newMax: number) => {
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

    const handleMinInputChange = (
      event: React.ChangeEvent<HTMLInputElement>
    ) => {
      let newMin = event.target.value === "" ? "" : Number(event.target.value);
      setMinInputValue(newMin); // Make sure empty input won't get changed.
      newMin = typeof newMin === "number" ? newMin : 0;
      const maxValue = props.data?.max || maxBound;

      newMin = Math.max(minBound, newMin);
      newMin = Math.min(maxValue, newMin);
      updateValue(newMin, maxValue);
    };
    const handleMinInputBlur = () => {
      let newMin = typeof minInputValue === "number" ? minInputValue : 0;
      const maxValue = props.data?.max || maxBound;
      newMin = Math.max(newMin, minBound);
      newMin = Math.min(newMin, maxValue);
      setMinInputValue(newMin);
    };

    const handleMaxInputChange = (
      event: React.ChangeEvent<HTMLInputElement>
    ) => {
      let newMax = event.target.value === "" ? "" : Number(event.target.value);
      setMaxInputValue(newMax); // Make sure empty input won't get changed.
      newMax = typeof newMax === "number" ? newMax : 0;
      const minValue = props.data?.min || minBound;

      newMax = Math.max(minValue, newMax);
      newMax = Math.min(newMax, maxBound);
      updateValue(minValue, newMax);
    };
    const handleMaxInputBlur = () => {
      let newMax = typeof maxInputValue === "number" ? maxInputValue : 0;
      const minValue = props.data?.min || minBound;
      newMax = Math.max(minValue, newMax);
      newMax = Math.min(newMax, maxBound);
      setMaxInputValue(newMax);
    };

    return (
      <Box sx={{ width: "30%", minWidth: 300, margin: 2 }}>
        <Grid container spacing={2}>
          <Grid item>
            <Input
              value={minInputValue}
              size="medium"
              onChange={handleMinInputChange}
              onBlur={handleMinInputBlur}
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
              value={[props.data?.min || minBound, props.data?.max || maxBound]}
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
              onChange={handleMaxInputChange}
              onBlur={handleMaxInputBlur}
              inputProps={{
                step: Math.floor((maxBound - minBound) / 20),
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

  if (enumHintValues?.length && enumHintValues?.length > 0) {
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

  return (
    <Typography>
      No information for attribute {props.data.attribute}.
    </Typography>
  );
}

type AttributeDetailsProps = {
  data: Data;
};

function AttributeDetails(props: AttributeDetailsProps) {
  if (
    typeof props.data.selected !== "undefined" &&
    props.data.selected.length !== 0
  ) {
    return (
      <>
        {props.data.selected.map(({ id, name }) => (
          <Stack direction="row" alignItems="baseline" key={id}>
            <Typography variant="body1">{id}</Typography>&nbsp;
            <Typography variant="body2">{name}</Typography>
          </Stack>
        ))}
      </>
    );
  } else if (isValid(props.data.min) && isValid(props.data.max)) {
    return (
      <>
        <Stack direction="row" alignItems="baseline">
          <Typography variant="body1">
            Current {props.data.attribute} in Range {props.data.min} to{" "}
            {props.data.max}
          </Typography>
        </Stack>
      </>
    );
  }

  return (
    <>
      <Typography variant="body1">None selected</Typography>
    </>
  );
}
