import { Checkbox, FormControlLabel, ListItem } from "@mui/material";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CriteriaConfig, CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import produce from "immer";
import React from "react";
import * as tanagra from "tanagra-api";
import { useUnderlay } from "../hooks";

type Selection = {
  id: number;
  name: string;
};

interface Config extends CriteriaConfig {
  attribute: string;
}

interface Data extends Config {
  selected: Selection[];
}

type AttributeEditProps = {
  dispatchFn: (data: Data) => void;
  data: Data;
};

@registerCriteriaPlugin("attribute", (config: CriteriaConfig) => ({
  ...(config.plugin as Config),
  selected: [],
}))
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;

  constructor(public id: string, data: unknown) {
    this.data = data as Data;
  }

  renderEdit(dispatchFn: (data: Data) => void) {
    return <AttributeEdit dispatchFn={dispatchFn} data={this.data} />;
  }

  renderDetails() {
    return <ConceptDetails data={this.data} />;
  }

  generateFilter() {
    if (this.data.selected.length === 0) {
      return null;
    }

    const operands = this.data.selected.map(({ id }) => ({
      binaryFilter: {
        attributeVariable: {
          variable: "person",
          name: this.data.attribute,
        },
        operator: tanagra.BinaryFilterOperator.Equals,
        attributeValue: {
          int64Val: id,
        },
      },
    }));

    return {
      arrayFilter: {
        operands: operands,
        operator: tanagra.ArrayFilterOperator.Or,
      },
    };
  }

  occurrenceEntities() {
    return [];
  }
}

function AttributeEdit(props: AttributeEditProps) {
  const underlay = useUnderlay();
  const hintDisplayName = (hint: tanagra.EnumHintValue) =>
    hint.displayName || "Unknown Value";

  const enumHintValues = underlay.entities
    .find((g) => g.name === underlay.primaryEntity)
    ?.attributes?.find((attribute) => attribute.name === props.data.attribute)
    ?.attributeFilterHint?.enumHint?.enumHintValues;

  const selectionIndex = (hint: tanagra.EnumHintValue) =>
    props.data.selected.findIndex(
      (row) => row.id === hint.attributeValue?.int64Val
    );

  if (enumHintValues?.length === 0) {
    return (
      <Typography>
        No information for attribute {props.data.attribute}.
      </Typography>
    );
  }
  return (
    <>
      {enumHintValues?.map((hint: tanagra.EnumHintValue) => (
        <ListItem key={hintDisplayName(hint)}>
          <FormControlLabel
            label={hintDisplayName(hint)}
            control={
              <Checkbox
                size="small"
                checked={selectionIndex(hint) > -1}
                onChange={() => {
                  props.dispatchFn(
                    produce(props.data, (data) => {
                      if (selectionIndex(hint) > -1) {
                        data.selected.splice(selectionIndex(hint), 1);
                      } else {
                        data.selected.push({
                          id: hint.attributeValue?.int64Val || -1,
                          name: hintDisplayName(hint),
                        });
                      }
                    })
                  );
                }}
              />
            }
          />
        </ListItem>
      ))}
    </>
  );
}

type ConceptDetailsProps = {
  data: Data;
};

function ConceptDetails(props: ConceptDetailsProps) {
  return (
    <>
      {props.data.selected.length === 0 ? (
        <Typography variant="body1">None selected</Typography>
      ) : (
        props.data.selected.map(({ id, name }) => (
          <Stack direction="row" alignItems="baseline" key={id}>
            <Typography variant="body1">{id}</Typography>&nbsp;
            <Typography variant="body2">{name}</Typography>
          </Stack>
        ))
      )}
    </>
  );
}
