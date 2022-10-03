import ClearIcon from "@mui/icons-material/Clear";
import SearchIcon from "@mui/icons-material/Search";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import Stack from "@mui/material/Stack";
import { TextField } from "mui-rff";
import { Form } from "react-final-form";

export type SearchProps = {
  placeholder?: string;
  onSearch: (query: string) => void;
};

export function Search(props: SearchProps) {
  return (
    <Box m={1}>
      <Form
        onSubmit={({ query }) => props.onSearch(query)}
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
                          props.onSearch("");
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
