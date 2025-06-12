import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import FormControl from "@mui/material/FormControl";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Typography from "@mui/material/Typography";
import { EnumHintOption, HintData } from "data/source";
import { DataValue } from "data/types";
import React from "react";
import { isValid } from "util/valid";

export type Selection = {
  value: DataValue;
  name: string;
};

export type HintDataSelectProps = {
  hintData?: HintData;
  selected?: Selection[];
  maxChips?: number;
  onSelect?: (sel: Selection[]) => void;
};

export function HintDataSelect(props: HintDataSelectProps) {
  const update = (sel: string[]) => {
    props.onSelect?.(
      sel
        .map((name) => {
          let value = props.hintData?.enumHintOptions?.find(
            (hint: EnumHintOption) => hint.name === name
          )?.value;
          if (value === undefined) {
            return undefined;
          }
          return {
            name,
            value: typeof value === "bigint" ? value.toString() : value,
          };
        })
        .filter(isValid)
    );
  };

  const onSelect = (event: SelectChangeEvent<string[]>) => {
    const {
      target: { value: sel },
    } = event;
    if (typeof sel === "string") {
      // This case is only for selects with text input.
      return;
    }
    update(sel);
  };

  const onDelete = (name: string) => {
    update(props.selected?.map((s) => s.name)?.filter((n) => n !== name) ?? []);
  };

  return (
    <FormControl variant="outlined">
      <Select
        variant="outlined"
        multiple
        displayEmpty
        disabled={!props.hintData}
        value={props.selected?.map((s) => s.name)}
        input={<OutlinedInput />}
        renderValue={(selected) => (
          <Box sx={{ mt: 0.25, display: "flex", flexWrap: "wrap", gap: 0.5 }}>
            {selected?.length ? (
              !props.maxChips || selected.length <= props.maxChips ? (
                selected.map((s) => (
                  <Chip
                    key={s}
                    label={s}
                    onDelete={() => {
                      onDelete(s);
                    }}
                  />
                ))
              ) : (
                <Typography variant="overline" component="em">
                  {selected.length} selected
                </Typography>
              )
            ) : (
              <Typography variant="overline" component="em">
                Any value
              </Typography>
            )}
          </Box>
        )}
        onChange={onSelect}
      >
        {props.hintData?.enumHintOptions?.map((hint: EnumHintOption) => (
          <MenuItem key={hint.name} value={hint.name}>
            {hint.name}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
