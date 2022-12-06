import ClearIcon from "@mui/icons-material/Clear";
import SearchIcon from "@mui/icons-material/Search";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import Stack from "@mui/material/Stack";
import { TextField } from "mui-rff";
import { useRef } from "react";
import { Form } from "react-final-form";
import { OnChange } from "react-final-form-listeners";
import { useSearchParams } from "react-router-dom";

export type SearchProps = {
  placeholder?: string;
  onSearch?: (query: string) => void;
  initialValue?: string;
  searchKey?: string;
};

export function Search(props: SearchProps) {
  const [, setSearchParams] = useSearchParams();
  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  const onSearch = (query: string) => {
    if (searchTimeout.current) {
      clearTimeout(searchTimeout.current);
    }

    if (props.onSearch) {
      props.onSearch(query);
    } else {
      const sk = props.searchKey ?? "search";
      setSearchParams((params) => {
        params.set(sk, query);
        return params;
      });
    }
  };

  const onChange = (query: string) => {
    if (searchTimeout.current) {
      clearTimeout(searchTimeout.current);
    }

    searchTimeout.current = setTimeout(() => {
      onSearch(query);
    }, 500);
  };

  return (
    <Box m={1}>
      <Form
        initialValues={{ query: props.initialValue }}
        onSubmit={({ query }) => onSearch(query)}
        render={({ handleSubmit, form }) => (
          <form onSubmit={handleSubmit}>
            <Stack direction="row" justifyContent="center" alignItems="center">
              <TextField
                autoFocus
                fullWidth
                name="query"
                placeholder={props.placeholder}
                autoComplete="off"
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => {
                          form.reset();
                          onSearch("");
                        }}
                        onMouseDown={(event) => event.preventDefault()}
                        edge="end"
                      >
                        <ClearIcon />
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
              <OnChange name="query">
                {(query: string) => onChange(query)}
              </OnChange>
              <IconButton type="submit">
                <SearchIcon />
              </IconButton>
            </Stack>
          </form>
        )}
      />
    </Box>
  );
}
