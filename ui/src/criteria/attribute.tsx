import { Checkbox, FormControlLabel, ListItem } from "@mui/material";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CriteriaConfig, CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import produce from "immer";
import React from "react";
import * as tanagra from "tanagra-api";
import { useAppDispatch, useUnderlay } from "../hooks";

type Selection = {
  entity: string;
  id: number;
  name: string;
};


interface Config extends CriteriaConfig {
  entity: string;
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
class _ implements CriteriaPlugin<Data> {
  public data: Data;

  constructor(public id: string, data: unknown) {
    this.data = data as Data;
  }

  renderEdit(dispatchFn: (data: Data) => void) {
    console.log(this.data)
    return <AttributeEdit dispatchFn={dispatchFn} data={this.data} />;
  }

  renderDetails() {
    return <ConceptDetails data={this.data} />;
  }

  generateFilter(entityVar: string, fromOccurrence: boolean) {
    return null;
  }

  occurrenceEntities() {
    return [];
  }
}

function AttributeEdit(props: AttributeEditProps) {
  const dispatch = useAppDispatch();
  const underlay = useUnderlay();
  const attribute = props.data.entity;
  const attributeName = verifyName(attribute);

  function verifyName(demographic: string) {
    return demographic.concat("_concept_id");
  }

  function hintDisplayName(hint: tanagra.EnumHintValue) {
    return hint.displayName || "Unknown Value";
  }

  const enumHintValues = underlay?.entities
    .filter((g) => g.name === underlay.primaryEntity)[0]
    .attributes?.filter((g) => g.name === attributeName)[0].attributeFilterHint
    ?.enumHint?.enumHintValues;
  const index =
    enumHintValues?.map((hint) => {
      return props.data.selected.findIndex(
        (row) =>
          row.entity === attribute &&
          row.id === (hint.attributeValue?.int64Val as number)
      );
    }) || [];
  console.log(index);

  return (
    <>
      {enumHintValues?.map((hint, idx) => (
        <ListItem key={hintDisplayName(hint)}>
          <FormControlLabel
            label={hintDisplayName(hint)}
            control={
              <Checkbox
                size="small"
                checked={index[idx] > -1}
                onChange={() => {
                  props.dispatchFn(
                    produce(props.data, (data) => {
                      console.log(idx, index[idx]);
                      if (index[idx] > -1) {
                        data.selected.splice(index[idx], 1);
                      } else {
                        data.selected.push({
                          entity: attribute,
                          id: hint.attributeValue?.int64Val as number,
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
