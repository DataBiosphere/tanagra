import DeleteIcon from "@mui/icons-material/Delete";
import Box from "@mui/material/Box";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import Input from "@mui/material/Input";
import Slider from "@mui/material/Slider";
import React, { useState } from "react";

export type DataRange = {
  id: string;
  min: number;
  max: number;
};

export type RangeSliderProps = {
  minBound: number;
  maxBound: number;
  range: DataRange;
  index: number;
  multiRange?: boolean;

  onUpdate: (range: DataRange, index: number, min: number, max: number) => void;
  onDelete?: (range: DataRange, index: number) => void;
};

export function RangeSlider(props: RangeSliderProps) {
  const { minBound, maxBound, range, index } = props;

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
    props.onUpdate(range, index, newMin, newMax);
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

  return (
    <Box sx={{ width: "30%", minWidth: 400, mt: 0.5 }}>
      <Grid container spacing={3} direction="row">
        <Grid item>
          <Input
            value={minInputValue}
            size="medium"
            onChange={handleMinInputChange}
            onBlur={handleMinInputBlur}
            inputProps={{
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
            onClick={() => props.onDelete?.(range, index)}
            style={{ marginLeft: 25 }}
          >
            <DeleteIcon fontSize="medium" />
          </IconButton>
        )}
      </Grid>
    </Box>
  );
}
