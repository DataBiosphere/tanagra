import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import ActionBar from "actionBar";
import { useAppSelector } from "hooks";
import { Link as RouterLink } from "react-router-dom";

export function UnderlaySelect() {
  const underlays = useAppSelector((state) => state.underlays);

  return (
    <>
      <ActionBar title="Select Dataset" />
      <List>
        {underlays.map((underlay) => (
          <ListItem key={underlay.name}>
            <ListItemButton component={RouterLink} to={`/${underlay.name}`}>
              <ListItemText primary={underlay.name}></ListItemText>
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </>
  );
}
