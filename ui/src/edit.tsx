import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import React from "react";
import { Link as RouterLink, useParams } from "react-router-dom";
import { DataSet } from "./dataSet";

type EditProps = {
  dataSet: DataSet;
};

export default function Edit(props: EditProps) {
  const params = useParams<{ group: string; criteria: string }>();

  const group = props.dataSet.findGroup(params.group);
  const criteria = !!group ? group.findCriteria(params.criteria) : null;

  return (
    <>
      <Stack direction="row" alignItems="flex-start">
        <IconButton aria-label="back" component={RouterLink} to="/">
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h4">
          {!!criteria ? criteria.name : "Unknown"}
        </Typography>
      </Stack>
      {!!criteria && !!group ? criteria.edit(props.dataSet, group) : null}
    </>
  );
}
