import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import ActionBar from "actionBar";
import { Link as RouterLink } from "react-router-dom";
import { underlayURL } from "router";
import {useAppSelector} from "./hooks";

export function UnderlaySelect() {
  const underlays = useAppSelector((state) => state.present.underlays);
  return (
    <>
      <ActionBar title="Select Dataset" backURL={null} />
      <List>
        {underlays.map((underlay) => (
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
    </>
  );
}
