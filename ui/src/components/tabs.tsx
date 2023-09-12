import TabContext from "@mui/lab/TabContext";
import TabList from "@mui/lab/TabList";
import TabPanel from "@mui/lab/TabPanel";
import Tab from "@mui/material/Tab";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { ReactNode, useState } from "react";

export type TabConfig = {
  id: string;
  title: string;
  render: () => ReactNode;
};

export type TabsProps = {
  configs: TabConfig[];
  center?: boolean;

  currentTab?: string;
  setCurrentTab?: (tab: string) => void;
};

export function Tabs(props: TabsProps) {
  const [currentTab, setCurrentTab] = useState(props.configs[0].id);

  const onChange = (event: React.SyntheticEvent, newValue: string) => {
    (props.setCurrentTab ?? setCurrentTab)(newValue);
  };

  return (
    <TabContext value={props.currentTab ?? currentTab}>
      <GridLayout rows>
        <GridLayout cols colAlign={props.center ? "center" : undefined}>
          <TabList onChange={onChange}>
            {props.configs.map((c) => (
              <Tab key={c.id} label={c.title} value={c.id} />
            ))}
          </TabList>
        </GridLayout>
        <GridBox
          sx={{
            borderTopStyle: "solid",
            borderColor: (theme) => theme.palette.divider,
            borderWidth: "1px",
          }}
        >
          {props.configs.map((c) => (
            <TabPanel key={c.id} value={c.id} sx={{ p: 0, height: "100%" }}>
              {c.render()}
            </TabPanel>
          ))}
        </GridBox>
      </GridLayout>
    </TabContext>
  );
}
