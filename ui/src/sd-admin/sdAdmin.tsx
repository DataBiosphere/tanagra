import Box from "@mui/material/Box";
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import { useState } from "react";
import { CohortAdmin } from "./cohortAdmin";
import { CohortAudit } from "./cohortAudit";
import { StudyAdmin } from "./studyAdmin";

const TabPanel = ({ index }: { index: number }) => {
  return (
    <div style={{ padding: "1rem" }}>
      {index === 0 && <StudyAdmin />}
      {index === 1 && <div>User content</div>}
      {index === 2 && <CohortAdmin />}
      {index === 3 && <CohortAudit />}
    </div>
  );
};

export function SdAdmin() {
  const [activeTab, setActiveTab] = useState(0);
  return (
    <Box>
      <Tabs
        value={activeTab}
        onChange={(e, newTab) => setActiveTab(newTab)}
        sx={{ position: "fixed", top: 0 }}
      >
        <Tab label="Studies" />
        <Tab label="Users" />
        <Tab label="Cohorts" />
        <Tab label="Cohort Audit" />
      </Tabs>
      <TabPanel index={activeTab} />
    </Box>
  );
}
