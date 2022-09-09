import DeleteIcon from "@mui/icons-material/Delete";
import {
  Button,
  Checkbox,
  FormControlLabel,
  IconButton,
  ListItem,
  Slider,
} from "@mui/material";
import Box from "@mui/material/Box";
import Grid from "@mui/material/Grid";
import Input from "@mui/material/Input";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, generateId, registerCriteriaPlugin } from "cohort";
import Loading from "components/loading";
import { DataValue } from "data/configuration";
import { FilterType } from "data/filter";
import { EnumHintOption, IntegerHint, useSource } from "data/source";
import { useAsyncWithApi } from "errors";
import produce from "immer";
import React, { useCallback, useState } from "react";
import { CriteriaConfig } from "underlaysSlice";

type Selection = {
  value: DataValue;
  name: string;
};

type DataRange = {
  id: string;
  min: number;
  max: number;
};

interface Config extends CriteriaConfig {
  attribute: string;
}

interface Data {
  // Selected is valid for enum attributes.
  selected: Selection[];

  // dataRanges is valid for integer attributes.
  dataRanges: DataRange[];
}

type AttributeEditProps = {
  dispatchFn: (data: Data) => void;
  data: Data;
  config: Config;
};

@registerCriteriaPlugin("attribute", () => {
  return {
    selected: [],
    dataRanges: [],
  };
})
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;
  private config: Config;

  constructor(public id: string, config: CriteriaConfig, data: unknown) {
    this.config = config as Config;
    this.data = data as Data;
  }

  renderEdit(dispatchFn: (data: Data) => void) {
    return (
      <AttributeEdit
        dispatchFn={dispatchFn}
        data={this.data}
        config={this.config}
      />
    );
  }

  renderDetails() {
    return <AttributeDetails data={this.data} config={this.config} />;
  }

  generateFilter() {
    return {
      type: FilterType.Attribute,
      occurrenceID: "",
      attribute: this.config.attribute,
      values: this.data.selected?.map(({ value }) => value),
      ranges: this.data.dataRanges,
    };
  }

  occurrenceID() {
    return "";
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
        <IconButton
          color="primary"
          aria-label="delete"
          onClick={handleDeleteRange}
          style={{ marginLeft: 25 }}
        >
          <DeleteIcon fontSize="medium" />
        </IconButton>
      </Grid>
    </Box>
  );
}

function AttributeEdit(props: AttributeEditProps) {
  const source = useSource();

  const fetchHintData = useCallback(() => {
    return source.getHintData("", props.config.attribute);
  }, [props.config.attribute]);
  const hintDataState = useAsyncWithApi(fetchHintData);

  const handleAddRange = useCallback(
    (hint: IntegerHint) => {
      props.dispatchFn(
        produce(props.data, (data) => {
          data.dataRanges.push({
            id: generateId(),
            ...hint,
          });
        })
      );
    },
    [props.data]
  );

  const selectionIndex = useCallback(
    (hint: EnumHintOption) =>
      props.data.selected.findIndex((sel) => sel.value === hint.value) ?? -1,
    [props.data.selected]
  );

  return (
    <Loading status={hintDataState}>
      {hintDataState.data?.integerHint && (
        <Box>
          <Grid container spacing={2} direction="column">
            {props.data?.dataRanges?.map(
              (range, index) =>
                hintDataState.data?.integerHint && (
                  <AttributeSlider
                    key={range.id}
                    index={index}
                    minBound={hintDataState.data.integerHint.min}
                    maxBound={hintDataState.data.integerHint.max}
                    range={range}
                    data={props.data}
                    dispatchFn={props.dispatchFn}
                  />
                )
            )}
          </Grid>
          <Button
            variant="contained"
            size="large"
            sx={{ mt: 5 }}
            onClick={() =>
              hintDataState.data?.integerHint &&
              handleAddRange(hintDataState.data.integerHint)
            }
          >
            Add Range
          </Button>
        </Box>
      )}
      {hintDataState.data?.enumHintOptions?.map((hint: EnumHintOption) => (
        <ListItem key={hint.name}>
          <FormControlLabel
            label={hint.name}
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
                          value: hint.value,
                          name: hint.name,
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
      {!hintDataState.data && (
        <Typography>
          No information for attribute {props.config.attribute}.
        </Typography>
      )}
    </Loading>
  );
}

type AttributeDetailsProps = {
  config: Config;
  data: Data;
};

function AttributeDetails(props: AttributeDetailsProps) {
  if (props.data.selected.length > 0) {
    return (
      <>
        {props.data.selected.map(({ value, name }) => (
          <Stack direction="row" alignItems="baseline" key={String(value)}>
            <Typography variant="body1">{value}</Typography>&nbsp;
            <Typography variant="body2">{name}</Typography>
          </Stack>
        ))}
      </>
    );
  }

  if (props.data.dataRanges.length > 0) {
    return (
      <>
        {props.data.dataRanges.map(({ id, min, max }) => (
          <Stack direction="row" alignItems="baseline" key={id}>
            <Typography variant="body1">
              Current {props.config.title} in Range {min} to {max}
            </Typography>
          </Stack>
        ))}
      </>
    );
  }

  return (
    <>
      <Typography variant="body1">None selected</Typography>
    </>
  );
}
