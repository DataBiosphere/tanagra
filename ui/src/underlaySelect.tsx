import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import ActionBar from "actionBar";
import { UnderlaysApiContext } from "apiContext";
import Loading from "components/loading";
import GridLayout from "layout/gridLayout";
import "plugins";
import { useCallback, useContext } from "react";
import { Link as RouterLink } from "react-router-dom";
import { underlayURL } from "router";
import useSWRImmutable from "swr/immutable";

export function UnderlaySelect() {
  const underlaysApi = useContext(UnderlaysApiContext);

  const underlaysState = useSWRImmutable(
    { type: "underlay" },
    useCallback(async () => {
      const res = await underlaysApi.listUnderlays({});
      if (!res?.underlays?.length) {
        throw new Error("No underlays are configured.");
      }

      return res.underlays;
    }, [])
  );

  return (
    <GridLayout rows>
      <ActionBar title="Select Dataset" backURL={null} />
      <Loading status={underlaysState}>
        <List>
          {underlaysState.data?.map((underlay) => (
            <ListItem key={underlay.name}>
              <ListItemButton
                component={RouterLink}
                to={underlayURL(underlay.name)}
              >
                <ListItemText primary={underlay.name}></ListItemText>
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      </Loading>
    </GridLayout>
  );
}
