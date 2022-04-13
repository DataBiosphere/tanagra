import { Checkbox, FormControlLabel, ListItem, Slider } from "@mui/material";
import Box from "@mui/material/Box";
import Grid from "@mui/material/Grid";
import Input from "@mui/material/Input";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CriteriaConfig, CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import produce from "immer";
import React from "react";
import * as tanagra from "tanagra-api";
import { useUnderlay } from "../hooks";
import { isValid } from "../util/valid";

type Selection = {
  id: number;
  name: string;
};

interface Config extends CriteriaConfig {
  attribute: string;
}

type ListChildrenConfig = {
  entity: string;
  idPath: string;
  filter: tanagra.Filter;
};

type EntityConfig = {
  name: string;
  selectable?: boolean;
  sourceConcepts?: boolean;
  attributes?: string[];
  hierarchical?: boolean;
  listChildren?: ListChildrenConfig;
};

interface Config extends CriteriaConfig {
  entities: EntityConfig[];
}

interface Data extends Config {
  selected: Selection[];
  min: number;
  max: number;
}

type AttributeEditProps = {
  dispatchFn: (data: Data) => void;
  data: Data;
};

@registerCriteriaPlugin("attribute", (config: CriteriaConfig) => ({
  ...(config.plugin as Config),
  selected: [],
}))
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
    if (
      this.data.selected.length === 0 &&
      !isValid(this.data.min) &&
      !isValid(this.data.max)
    ) {
      return null;
    }

    const year = new Date().getFullYear();
    const max = this.data.min || 0;
    const operands =
      isValid(this.data.min) && isValid(this.data.max)
        ? [
            {
              binaryFilter: {
                attributeVariable: {
                  variable: "person",
                  name: this.data.attribute,
                },
                operator: tanagra.BinaryFilterOperator.LessThan,
                attributeValue: {
                  int64Val: year - max,
                },
              },
            },
            // TODO: add GreaterThan in the filter
          ]
        : this.data.selected.map(({ id }) => ({
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

  const year = new Date().getFullYear();

  if (isValid(attributeFilterHint?.integerBoundsHint)) {
    const integerBoundsHint = attributeFilterHint?.integerBoundsHint;

    if (
      integerBoundsHint?.max !== null &&
      integerBoundsHint?.max !== undefined &&
      integerBoundsHint?.min !== null &&
      integerBoundsHint?.min !== undefined
    ) {
      const min = year - integerBoundsHint?.max;
      const max = year - integerBoundsHint.min;

      const [minValue, setMinValue] = React.useState<number>(min);
      const [maxValue, setMaxValue] = React.useState<number>(max);

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

      React.useEffect(() => updateValue(min, max), []);

      const handleChange = (event: Event, newValue: number | number[]) => {
        newValue = newValue as number[];
        const [newMin, newMax] = newValue;
        updateValue(newMin, newMax);
      };

      const handleInputChange = (
        event: React.ChangeEvent<HTMLInputElement>
      ) => {
        const newMin =
          event.target.value === "" ? min : Number(event.target.value);
        const newMax =
          event.target.value === "" ? max : Number(event.target.value);
        updateValue(Math.min(newMin, minValue), Math.max(newMax, maxValue));
      };

      return (
        <Box sx={{ width: "30%", minWidth: 100 }}>
          <Typography id="input-slider" gutterBottom>
            Age
          </Typography>
          <Grid container spacing={2} alignItems="center">
            <Grid item>
              <Input
                value={minValue}
                size="small"
                onChange={handleInputChange}
                inputProps={{
                  step: 5,
                  min: min,
                  max: max,
                  type: "number",
                  "aria-labelledby": "input-slider",
                }}
              />
            </Grid>
            <Grid item xs>
              <Slider
                getAriaLabel={() => "Temperature range"}
                value={[minValue, maxValue]}
                onChange={handleChange}
                valueLabelDisplay="auto"
                getAriaValueText={(value) => value.toString()}
                min={min}
                max={max}
              />
            </Grid>
            <Grid item>
              <Input
                value={maxValue}
                size="small"
                onChange={handleInputChange}
                inputProps={{
                  step: 5,
                  min: min,
                  max: max,
                  type: "number",
                  "aria-labelledby": "input-slider",
                }}
              />
            </Grid>
          </Grid>
        </Box>
      );
    }
  }

  const selectionIndex = (hint: tanagra.EnumHintValue) =>
    props.data.selected.findIndex(
      (row) => row.id === hint.attributeValue?.int64Val
    );

  if (attributeFilterHint?.enumHint?.enumHintValues?.length === 0) {
    return (
      <Typography>
        No information for attribute {props.data.attribute}.
      </Typography>
    );
  }
  return (
    <>
      {attributeFilterHint?.enumHint?.enumHintValues?.map(
        (hint: tanagra.EnumHintValue) => (
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
        )
      )}
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
            Current Age in Range &nbsp; {props.data.min}&nbsp;
          </Typography>
          <Typography variant="body2">to&nbsp; {props.data.max}</Typography>
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
