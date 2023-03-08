import Box from "@mui/material/Box";
import List from "@mui/material/List";
import ListItemButton from "@mui/material/ListItemButton";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import Empty from "components/empty";
import Loading from "components/loading";
import { useSource } from "data/source";
import { Link as RouterLink } from "react-router-dom";
import useSWR from "swr";

export function StudiesList() {
  const source = useSource();

  const studiesState = useSWR({ component: "StudiesList" }, async () => {
    return await source.listStudies();
  });

  return (
    <>
      <ActionBar title={"Studies"} />
      <Loading status={studiesState}>
        <Box sx={{ p: 1 }}>
          {!!studiesState.data?.length ? (
            <List sx={{ p: 0 }}>
              {studiesState.data?.map((study) => (
                <ListItemButton
                  sx={{ p: 0, mb: 1 }}
                  component={RouterLink}
                  key={study.id}
                  to={"studies/" + study.id}
                >
                  <Paper sx={{ p: 1 }}>
                    <Stack direction="row">
                      <Stack>
                        <Typography variant="h4">
                          {study.displayName}
                        </Typography>
                        <Typography variant="body2">
                          {study.created.toLocaleString()}
                        </Typography>
                      </Stack>
                    </Stack>
                  </Paper>
                </ListItemButton>
              ))}
            </List>
          ) : (
            <Empty
              maxWidth="90%"
              minHeight="400px"
              title="No studies created"
            />
          )}
        </Box>
      </Loading>
    </>
  );
}
