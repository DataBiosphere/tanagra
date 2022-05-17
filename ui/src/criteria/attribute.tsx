import {
  Button,
  Checkbox,
  FormControlLabel,
  ListItem,
  Slider,
} from "@mui/material";
import Box from "@mui/material/Box";
import Grid from "@mui/material/Grid";
import Input from "@mui/material/Input";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, generateId, registerCriteriaPlugin } from "cohort";
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

type DataRange = {
  id: string;
  min: number | undefined;
  max: number | undefined;
};

interface Config extends CriteriaConfig {
  attribute: string;
  name: string;
}

interface Data extends Config {
  // Selected is valid for enum attributes.
  selected: Selection[];

  // The min/max are valid for integer attributes.
  dataRanges: DataRange[];
}

type AttributeEditProps = {
  dispatchFn: (data: Data) => void;
  data: Data;
};

@registerCriteriaPlugin(
  "attribute",
  (underlay: Underlay, config: CriteriaConfig) => {
    const data = { ...(config.plugin as Config), name: config.title };

    const integerBoundsHint = underlay.entities
      .find((g) => g.name === underlay.primaryEntity)
      ?.attributes?.find((attribute) => attribute.name === data.attribute)
      ?.attributeFilterHint?.integerBoundsHint;

    let initialDataRange: DataRange = {
      id: generateId(),
      min: undefined,
      max: undefined,
    }

    if (integerBoundsHint?.min && integerBoundsHint?.max) {
      initialDataRange = {
        ...initialDataRange,
        min: integerBoundsHint.min,
        max: integerBoundsHint.max,
      };
    }

    return {
      ...data,
      selected: !integerBoundsHint ? [] : undefined,
      dataRanges: [initialDataRange],
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

  generateFilter(entityVar: string) {
    const dataRangeFilter: tanagra.Filter = {
      arrayFilter: {
        operands: this.data.dataRanges
          .filter((range) => isValid(range.min) && isValid(range.max))
          .map((range) => ({
            arrayFilter: {
              operands: [
                {
                  binaryFilter: {
                    attributeVariable: {
                      variable: entityVar,
                      name: this.data.attribute,
                    },
                    operator: tanagra.BinaryFilterOperator.GreaterThan,
                    attributeValue: {
                      int64Val: range.min,
                    },
                  },
                },
                {
                  binaryFilter: {
                    attributeVariable: {
                      variable: entityVar,
                      name: this.data.attribute,
                    },
                    operator: tanagra.BinaryFilterOperator.GreaterThan,
                    attributeValue: {
                      int64Val: range.min,
                    },
                  },
                },
              ],
              operator: tanagra.ArrayFilterOperator.And,
            },
          })),
        operator: tanagra.ArrayFilterOperator.Or,
      },
    };

    if (
      dataRangeFilter.arrayFilter?.operands &&
      dataRangeFilter.arrayFilter?.operands.length > 0
    ) {
      return dataRangeFilter;
    } else if (this.data.selected.length >= 0) {
      return {
        arrayFilter: {
          operands: this.data.selected.map(({ id }) => ({
            binaryFilter: {
              attributeVariable: {
                variable: entityVar,
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

type SliderProps = {
  minBound: number;
  maxBound: number;
  range: DataRange;
  data: Data;
  dispatchFn: (data: Data) => void;
  index: number;
};

function AttributeSlider(props: SliderProps) {
  const { minBound, maxBound, range, data, dispatchFn, index } = props;

  // Two sets of values are needed due to the input box and slider is isolated.
  const [minInputValue, setMinInputValue] = useState(
    String(range.min || minBound)
  );
  const [maxInputValue, setMaxInputValue] = useState(
    String(range.max || maxBound)
  );
  const [minValue, setMinValue] = useState(range.min || minBound);
  const [maxValue, setMaxValue] = useState(range.max || maxBound);

  const updateValue = (newMin: number, newMax: number) => {
    setMinValue(newMin);
    setMaxValue(newMax);
    dispatchFn(
      produce(data, (oldData) => {
        oldData.dataRanges[index].min = newMin;
        oldData.dataRanges[index].max = newMax;
      })
    );
  };

  const handleChange = (event: Event, newValue: number | number[]) => {
    const [newMin, newMax] = newValue as number[];
    setMinInputValue(String(newMin));
    setMaxInputValue(String(newMax));
    updateValue(newMin, newMax);
  };

  // Make sure empty input won't get changed.

  const handleMinInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setMinInputValue(event.target.value);
    const newMin = event.target.value === "" ? 0 : Number(event.target.value);
    updateValue(Math.min(maxValue, Math.max(minBound, newMin)), maxValue);
  };
  const handleMinInputBlur = () => {
    setMinInputValue(String(minValue));
  };

  const handleMaxInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setMaxInputValue(event.target.value);
    const newMax = event.target.value === "" ? 0 : Number(event.target.value);
    updateValue(Math.max(minValue, Math.min(maxBound, newMax)), minValue);
  };
  const handleMaxInputBlur = () => {
    setMaxInputValue(String(maxValue));
  };

  const handleDeleteRange = () => {
    dispatchFn(
      produce(data, (oldData) => {
        oldData.dataRanges.splice(index, 1);
      })
    );
  };

  return (
    <Box sx={{ width: "30%", minWidth: 500, margin: 5 }}>
      <Grid container spacing={2} direction="row">
        <Grid item>
          <Input
            value={minInputValue}
            size="medium"
            onChange={handleMinInputChange}
            onBlur={handleMinInputBlur}
            inputProps={{
              step: Math.ceil((maxBound - minBound) / 20),
              min: minBound,
              max: maxBound,
              type: "number",
              "aria-labelledby": "input-slider",
            }}
          />
        </Grid>
        <Grid item xs>
          <Slider
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
            onChange={handleMaxInputChange}
            onBlur={handleMaxInputBlur}
            inputProps={{
              step: Math.ceil((maxBound - minBound) / 20),
              min: minBound,
              max: maxBound,
              type: "number",
              "aria-labelledby": "input-slider",
            }}
          />
        </Grid>
        <Button variant="outlined" sx={{ ml: 5 }} onClick={handleDeleteRange}>
          Delete
        </Button>
      </Grid>
    </Box>
  );
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

  if (isValid(integerBoundsHint?.min) && isValid(integerBoundsHint?.max)) {
    // TODO: The comments can be removed once isValid is fixed.

    const handleAddRange = () => {
      props.dispatchFn(
        produce(props.data, (data) => {
          // TODO: Ask about a more efficient way to handle non-null values
          //       Without the guard, there is a type mismatch (number | null | undefined vs number | undefined)
          //       Changing type to be number | null | undefined creates problems in other places in the code
          if (integerBoundsHint?.min && integerBoundsHint?.max) {
            data.dataRanges.push({
              id: generateId(),
              min: integerBoundsHint.min,
              max: integerBoundsHint.max,
            });
          }
        })
      );
    };

    return (
      <Box>
        <Grid container spacing={2} direction="column">
          {props.data.dataRanges.map((range, index) => {
            return (
              <AttributeSlider
                key={range.id}
                index={index}
                minBound={integerBoundsHint?.min || 0}
                maxBound={integerBoundsHint?.max || 0}
                range={range}
                data={props.data}
                dispatchFn={props.dispatchFn}
              />
            );
          })}
        </Grid>
        <Button
          variant="contained"
          size="large"
          sx={{ mt: 5 }}
          onClick={handleAddRange}
        >
          Add Range
        </Button>
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
  if (props.data.selected?.length) {
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
  } else if (
    isValid(props.data.dataRanges[0].min) &&
    isValid(props.data.dataRanges[0].max)
  ) {
    return (
      <>
        <Stack direction="row" alignItems="baseline">
          <Typography variant="body1">
            Current {props.data.name} in Range {props.data.dataRanges[0].min} to{" "}
            {props.data.dataRanges[0].max}
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
