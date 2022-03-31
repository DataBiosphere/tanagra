import { Checkbox, FormControlLabel, ListItem } from "@mui/material";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import {
  Cohort,
  CriteriaConfig,
  CriteriaPlugin,
  Group,
  registerCriteriaPlugin,
} from "cohort";
import produce from "immer";
import React from "react";
import * as tanagra from "tanagra-api";
import { updateCriteriaData } from "../cohortsSlice";
import { useAppDispatch, useUnderlay } from "../hooks";

type Selection = {
  entity: string;
  id: number;
  name: string;
};

type ListChildrenConfig = {
  entity: string;
  idPath: string;
  filter: tanagra.Filter;
};

type EntityConfig = {
  name: string;
  selectable?: boolean;
  sourceConcepts?: boolean;
  attributes?: string[];
  hierarchical?: boolean;
  listChildren?: ListChildrenConfig;
};

interface Config extends CriteriaConfig {
  entities: EntityConfig[];
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
    return <AttributeEdit dispatchFn={dispatchFn} data={this.data} />;
  }

  renderDetails() {
    return <ConceptDetails data={this.data} />;
  };

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
  const demographic = props.data.entities[0].name;
  const demographicName = verifyName(demographic);

  function verifyName(demographic: string) {
    if (demographic == "sex assigned at birth") {
      demographic = "sex_at_birth";
    } else if (demographic == "gender identity") {
      demographic = "gender";
    }
    return demographic.concat("_concept_id");
  }

  const categoryList = underlay?.entities
    .filter((g) => g.name === "person")[0]
    .attributes?.filter((g) => g.name === demographicName)[0]
    .attributeFilterHint?.enumHint?.enumHintValues;
  const index =
    categoryList?.map((hint) => {
      return props.data.selected.findIndex(
        (row) =>
          row.entity === demographic &&
          row.id === (hint.attributeValue?.int64Val as number)
      );
    }) || [];
  console.log(index);

  return (
    <>
      {categoryList?.map((hint, idx) => (
        <ListItem key={hint.displayName || ""}>
          <FormControlLabel
            label={hint.displayName || ""}
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
                        const name = hint.displayName;
                        data.selected.push({
                          entity: demographic,
                          id: hint.attributeValue?.int64Val as number,
                          name: !!name ? String(name) : "",
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