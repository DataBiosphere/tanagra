import SearchIcon from "@mui/icons-material/Search";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
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
        initialValues={{ query: "" }}
        onSubmit={({ query }) => props.onSearch(query)}
        render={({ handleSubmit }) => (
          <form onSubmit={handleSubmit}>
            <Stack direction="row" justifyContent="center" alignItems="center">
              <TextField
                autoFocus
                fullWidth
                name="query"
                placeholder={props.placeholder}
                autoComplete="off"
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
