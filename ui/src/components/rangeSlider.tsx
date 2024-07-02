import DeleteIcon from "@mui/icons-material/Delete";
import IconButton from "@mui/material/IconButton";
import Input from "@mui/material/Input";
import InputAdornment from "@mui/material/InputAdornment";
import Slider from "@mui/material/Slider";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import React, { useRef, useState } from "react";

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
  unit?: string;

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

  const updateTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  const updateValue = (newMin: number, newMax: number) => {
    setMinValue(newMin);
    setMaxValue(newMax);

    if (updateTimeout.current) {
      clearTimeout(updateTimeout.current);
    }

    updateTimeout.current = setTimeout(() => {
      props.onUpdate(range, index, newMin, newMax);
    }, 500);
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
    <GridBox
      sx={{
        maxWidth: "700px",
        height: "auto",
        mt: 0.5,
      }}
      onClick={(e) => {
        e.preventDefault();
        e.stopPropagation();
      }}
    >
      <GridLayout cols fillCol={1} spacing={3} height="auto">
        <Input
          value={minInputValue}
          size="medium"
          onChange={handleMinInputChange}
          onBlur={handleMinInputBlur}
          endAdornment={
            props.unit ? (
              <InputAdornment position="end">{props.unit}</InputAdornment>
            ) : undefined
          }
          inputProps={{
            min: minBound,
            max: maxBound,
            type: "number",
            "aria-labelledby": "input-slider",
          }}
        />
        <Slider
          value={[minValue, maxValue]}
          onChange={handleChange}
          valueLabelDisplay="auto"
          getAriaValueText={(value) => value.toString()}
          min={minBound}
          max={maxBound}
          disableSwap
        />
        <Input
          value={maxInputValue}
          onChange={handleMaxInputChange}
          onBlur={handleMaxInputBlur}
          endAdornment={
            props.unit ? (
              <InputAdornment position="end">{props.unit}</InputAdornment>
            ) : undefined
          }
          inputProps={{
            min: minBound,
            max: maxBound,
            type: "number",
            "aria-labelledby": "input-slider",
          }}
        />
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
      </GridLayout>
    </GridBox>
  );
}
