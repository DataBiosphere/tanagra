import ClearIcon from "@mui/icons-material/Clear";
import SearchIcon from "@mui/icons-material/Search";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import Stack from "@mui/material/Stack";
import { TextField } from "mui-rff";
import { useEffect, useRef } from "react";
import { Form, FormSpy } from "react-final-form";
import { useLocalSearchState } from "util/searchState";

type SearchState = Record<string, unknown>;

export type SearchProps = {
  placeholder?: string;
  onSearch?: (query: string) => void;
  initialValue?: string;
  searchKey?: string;
  delayMs?: number;

  disabled?: boolean;
  showSearchButton?: boolean;
};

export function Search(props: SearchProps) {
  const [, updateSearchState] = useLocalSearchState<SearchState>();
  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (searchTimeout.current) {
        clearTimeout(searchTimeout.current);
      }
    };
  }, []);

  const onSearch = (query: string) => {
    if (!searchTimeout.current) {
      return;
    }

    clearTimeout(searchTimeout.current);

    if (props.onSearch) {
      props.onSearch(query);
    } else {
      const sk = props.searchKey ?? "search";
      updateSearchState((state) => {
        state[sk] = query ?? "";
      });
    }
  };

  const onChange = (query: string) => {
    if (searchTimeout.current) {
      clearTimeout(searchTimeout.current);
    }

    searchTimeout.current = setTimeout(() => {
      onSearch(query);
    }, props.delayMs ?? 500);
  };

  return (
    <Box>
      <Form
        initialValues={{ query: props.initialValue }}
        onSubmit={({ query }) => onSearch(query)}
        render={({ handleSubmit, form, values }) => (
          <form onSubmit={handleSubmit}>
            <Stack direction="row" justifyContent="center" alignItems="center">
              <TextField
                disabled={props.disabled}
                autoFocus
                fullWidth
                name="query"
                placeholder={props.placeholder}
                autoComplete="off"
                sx={{
                  backgroundColor: (theme) => theme.palette.info.main,
                }}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon />
                    </InputAdornment>
                  ),
                  endAdornment: values.query?.length ? (
                    <InputAdornment position="end">
                      <IconButton
                        disabled={props.disabled}
                        onClick={() => {
                          form.reset();
                          onSearch("");
                        }}
                        onMouseDown={(event) => event.preventDefault()}
                        edge="end"
                        size="small"
                      >
                        <ClearIcon />
                      </IconButton>
                    </InputAdornment>
                  ) : undefined,
                }}
              />
              <FormSpy
                onChange={(state) => {
                  onChange(state.values.query);
                }}
              />
              {props.showSearchButton ? (
                <IconButton type="submit" size="small">
                  <SearchIcon />
                </IconButton>
              ) : null}
            </Stack>
          </form>
        )}
      />
    </Box>
  );
}
