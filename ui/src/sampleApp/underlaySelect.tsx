import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import { UnderlaysApiContext } from "apiContext";
import Loading from "components/loading";
import GridLayout from "layout/gridLayout";
import "plugins";
import { useCallback, useContext } from "react";
import { underlayURL } from "router";
import { Header } from "sampleApp/header";
import useSWRImmutable from "swr/immutable";
import { RouterLink } from "util/searchState";

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
      <Header />
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
