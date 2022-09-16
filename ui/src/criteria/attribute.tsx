import DeleteIcon from "@mui/icons-material/Delete";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import FormControl from "@mui/material/FormControl";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import Input from "@mui/material/Input";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Slider from "@mui/material/Slider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, generateId, registerCriteriaPlugin } from "cohort";
import Loading from "components/loading";
import { DataValue } from "data/configuration";
import { FilterType } from "data/filter";
import { EnumHintOption, IntegerHint, useSource } from "data/source";
import { useAsyncWithApi } from "errors";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import React, { useCallback, useMemo, useState } from "react";
import { CriteriaConfig } from "underlaysSlice";
import { isValid } from "util/valid";

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
  multiRange?: boolean;
}

interface Data {
  // Selected is valid for enum attributes.
  selected: Selection[];

  // dataRanges is valid for integer attributes.
  dataRanges: DataRange[];
}

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

  renderInline(criteriaId: string) {
    return (
      <AttributeInline
        criteriaId={criteriaId}
        data={this.data}
        config={this.config}
      />
    );
  }

  displayDetails() {
    if (this.data.selected.length > 0) {
      return {
        title:
          this.data.selected.length === 1
            ? this.data.selected[0].name
            : "(multiple)",
        additionalText: this.data.selected.map((s) => s.name),
      };
    }

    if (this.data.dataRanges.length > 0) {
      const additionalText = [
        this.data.dataRanges.map((r) => `${r.min}-${r.max}`).join(", "),
      ];
      return {
        title:
          this.data.dataRanges.length === 1 ? additionalText[0] : "(multiple)",
        additionalText,
      };
    }

    return {
      title: "(any)",
    };
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
  criteriaId?: string;
  index: number;
  multiRange?: boolean;
};

function AttributeSlider(props: SliderProps) {
  const updateCriteria = useUpdateCriteria(props.criteriaId);
  const { minBound, maxBound, range, data, index } = props;

  const initialMin = Math.max(range.min, minBound);
  const initialMax = Math.min(range.max, maxBound);

  // Two sets of values are needed due to the input box and slider is isolated.
  const [minInputValue, setMinInputValue] = useState(String(initialMin));
  const [maxInputValue, setMaxInputValue] = useState(String(initialMax));
  const [minValue, setMinValue] = useState(initialMin);
  const [maxValue, setMaxValue] = useState(initialMax);

  const updateValue = (newMin: number, newMax: number) => {
    setMinValue(newMin);
    setMaxValue(newMax);
    updateCriteria(
      produce(data, (oldData) => {
        if (oldData.dataRanges.length === 0) {
          oldData.dataRanges.push(range);
        }

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
    updateValue(minValue, Math.max(minValue, Math.min(maxBound, newMax)));
  };
  const handleMaxInputBlur = () => {
    setMaxInputValue(String(maxValue));
  };

  const handleDeleteRange = () => {
    updateCriteria(
      produce(data, (oldData) => {
        oldData.dataRanges.splice(index, 1);
      })
    );
  };

  return (
    <Box sx={{ width: "30%", minWidth: 500 }}>
      <Grid container spacing={1} direction="row">
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
        {props.multiRange && (
          <IconButton
            color="primary"
            aria-label="delete"
            onClick={handleDeleteRange}
            style={{ marginLeft: 25 }}
          >
            <DeleteIcon fontSize="medium" />
          </IconButton>
        )}
      </Grid>
    </Box>
  );
}

type AttributeInlineProps = {
  criteriaId: string;
  config: Config;
  data: Data;
};

function AttributeInline(props: AttributeInlineProps) {
  const source = useSource();
  const updateCriteria = useUpdateCriteria(props.criteriaId);

  const fetchHintData = useCallback(() => {
    return source.getHintData("", props.config.attribute);
  }, [props.config.attribute]);
  const hintDataState = useAsyncWithApi(fetchHintData);

  const handleAddRange = useCallback(
    (hint: IntegerHint) => {
      updateCriteria(
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

  const emptyRange = useMemo(
    () => ({
      id: generateId(),
      min: Number.MIN_SAFE_INTEGER,
      max: Number.MAX_SAFE_INTEGER,
    }),
    [props.criteriaId]
  );

  const listRanges = () => {
    if (!hintDataState.data?.integerHint) {
      return null;
    }

    if (!props.config.multiRange && props.data.dataRanges.length === 0) {
      return (
        <AttributeSlider
          key={emptyRange.id}
          index={0}
          minBound={hintDataState.data.integerHint.min}
          maxBound={hintDataState.data.integerHint.max}
          range={emptyRange}
          data={props.data}
          criteriaId={props.criteriaId}
        />
      );
    }

    return props.data.dataRanges.map(
      (range, index) =>
        hintDataState.data?.integerHint && (
          <AttributeSlider
            key={range.id}
            index={index}
            minBound={hintDataState.data.integerHint.min}
            maxBound={hintDataState.data.integerHint.max}
            range={range}
            data={props.data}
            criteriaId={props.criteriaId}
          />
        )
    );
  };

  const onSelect = (event: SelectChangeEvent<string[]>) => {
    const {
      target: { value: sel },
    } = event;
    updateCriteria(
      produce(props.data, (data) => {
        if (typeof sel === "string") {
          // This case is only for selects with text input.
          return;
        }
        data.selected = sel
          .map((name) => {
            const value = hintDataState.data?.enumHintOptions?.find(
              (hint: EnumHintOption) => hint.name === name
            )?.value;
            if (!isValid(value)) {
              return undefined;
            }
            return {
              name,
              value,
            };
          })
          .filter(isValid);
      })
    );
  };

  return (
    <Loading status={hintDataState}>
      <Box>
        <Stack spacing={1}>{listRanges()}</Stack>
        {props.config.multiRange && (
          <Button
            variant="contained"
            size="large"
            sx={{ mt: 2 }}
            onClick={() =>
              hintDataState.data?.integerHint &&
              handleAddRange(hintDataState.data.integerHint)
            }
          >
            Add Range
          </Button>
        )}
      </Box>

      {!!hintDataState.data?.enumHintOptions && (
        <FormControl size="small" sx={{ maxWidth: 500 }}>
          <Select
            multiple
            displayEmpty
            value={props.data.selected.map((s) => s.name)}
            input={<OutlinedInput />}
            renderValue={(selected) => (
              <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5 }}>
                {selected.length > 0 ? (
                  selected.map((s) => <Chip key={s} label={s} />)
                ) : (
                  <em>None selected</em>
                )}
              </Box>
            )}
            onChange={onSelect}
          >
            {hintDataState.data?.enumHintOptions?.map(
              (hint: EnumHintOption) => (
                <MenuItem key={hint.name} value={hint.name}>
                  {hint.name}
                </MenuItem>
              )
            )}
          </Select>
        </FormControl>
      )}

      {!hintDataState.data && (
        <Typography>
          No information for attribute {props.config.attribute}.
        </Typography>
      )}
    </Loading>
  );
}
