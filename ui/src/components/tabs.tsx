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
  vertical?: boolean;
  hideDivider?: boolean;

  tabsPrefix?: ReactNode;
  tabsSuffix?: ReactNode;

  currentTab?: string;
  setCurrentTab?: (tab: string) => void;
};

export function Tabs(props: TabsProps) {
  const [currentTab, setCurrentTab] = useState(props?.configs?.[0]?.id);

  const onChange = (event: React.SyntheticEvent, newValue: string) => {
    (props.setCurrentTab ?? setCurrentTab)(newValue);
  };

  return (
    <TabContext
      value={props.currentTab ?? currentTab ?? props?.configs?.[0]?.id}
    >
      <GridLayout
        rows={!props.vertical || undefined}
        cols={props.vertical || undefined}
      >
        <GridLayout
          rows={props.vertical ? "auto 1fr auto" : undefined}
          rowAlign={
            !props.vertical || (props.vertical && props.center)
              ? "middle"
              : undefined
          }
          cols={!props.vertical ? "auto 1fr auto" : undefined}
          colAlign={!props.vertical && props.center ? "center" : undefined}
        >
          {props.tabsPrefix ?? <GridBox />}
          <TabList
            onChange={onChange}
            orientation={props.vertical ? "vertical" : "horizontal"}
          >
            {props.configs.map((c) => (
              <Tab key={c.id} label={c.title} value={c.id} />
            ))}
          </TabList>
          {props.tabsSuffix ?? <GridBox />}
        </GridLayout>
        <GridBox
          sx={{
            borderTopStyle: !props.vertical ? "solid" : undefined,
            borderLeftStyle: props.vertical ? "solid" : undefined,
            borderColor: (theme) => theme.palette.divider,
            borderWidth: !props.hideDivider ? "1px" : "0px",
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
