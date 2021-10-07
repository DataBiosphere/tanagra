import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import React from "react";
import { Link as RouterLink, useParams } from "react-router-dom";
import { Dataset } from "./dataset";

type EditProps = {
  dataset: Dataset;
};

export default function Edit(props: EditProps) {
  const params = useParams<{ group: string; criteria: string }>();

  const group = props.dataset.findGroup(params.group);
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
      {!!criteria && !!group ? criteria.renderEdit(props.dataset, group) : null}
    </>
  );
}
