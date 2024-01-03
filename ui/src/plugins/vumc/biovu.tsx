import InfoIcon from "@mui/icons-material/Info";
import FormControl from "@mui/material/FormControl";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import Checkbox from "components/checkbox";
import {
  Filter,
  FilterType,
  makeArrayFilter,
  UnaryFilterOperator,
} from "data/filter";
import { UnderlaySource } from "data/source";
import { DataEntry } from "data/types";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import React from "react";
import { CriteriaConfig } from "underlaysSlice";
import { safeRegExp } from "util/safeRegExp";

enum SampleFilter {
  NONE = "None",
  ANY = "Any",
  ONE_HUNDRED = "100",
  FIVE_HUNDRED = "500",
}

const sampleFilterDescriptions = {
  [SampleFilter.NONE]: "Any SD Records (not requiring BioVU resources)",
  [SampleFilter.ANY]: "Any BioVU DNA (no minimum amount or concentration)",
  [SampleFilter.ONE_HUNDRED]: "At least 100ng with at least ng/µL BioVU DNA",
  [SampleFilter.FIVE_HUNDRED]: "At least 500ng with at least ng/µL BioVU DNA",
};

const sampleFilterFilters: { [key: string]: Filter | null } = {
  [SampleFilter.NONE]: null,
  [SampleFilter.ANY]: {
    type: FilterType.Attribute,
    attribute: "has_biovu_sample",
    values: [true],
  },
  [SampleFilter.ONE_HUNDRED]: {
    type: FilterType.Attribute,
    attribute: "biovu_sample_dna_yield",
    ranges: [{ min: 100, max: Number.MAX_SAFE_INTEGER }],
  },
  [SampleFilter.FIVE_HUNDRED]: {
    type: FilterType.Attribute,
    attribute: "biovu_sample_dna_yield",
    ranges: [{ min: 500, max: Number.MAX_SAFE_INTEGER }],
  },
};

const EXCLUDE_COMPROMISED = "Exclude Compromised DNA";
const EXCLUDE_INTERNAL =
  "Include only samples available for external processing";
const PLASMA = "Any existing banked BioVU Plasma";

const EXCLUDE_COMPROMISED_TOOLTIP = `"Compromised" DNA may not represent
  anticipated germline genetic profiles of the subject due to select medical
  procedures (such as transfusions) or conditions with blood cell somatic
  mutations. This filter only applies to DNA queries.`;
const EXCLUDE_INTERNAL_TOOLTIP =
  "Some BioVU sample cannot be tested outsize of Vanderbilt";

interface Data {
  sampleFilter: SampleFilter;
  excludeCompromised?: boolean;
  excludeInternal?: boolean;
  plasma?: boolean;
}

@registerCriteriaPlugin(
  "biovu",
  (
    underlaySource: UnderlaySource,
    c: CriteriaConfig,
    dataEntry?: DataEntry
  ) => {
    return {
      sampleFilter: (dataEntry?.key as SampleFilter) ?? SampleFilter.NONE,
    };
  },
  search
)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;
  private config: CriteriaConfig;

  constructor(
    public id: string,
    config: CriteriaConfig,
    data: unknown,
    private entity?: string
  ) {
    this.config = config;
    this.data = data as Data;
  }

  renderInline(groupId: string) {
    return (
      <BioVUInline
        groupId={groupId}
        criteriaId={this.id}
        data={this.data}
        config={this.config}
      />
    );
  }

  displayDetails() {
    const additionalText: string[] = [];
    if (this.data.sampleFilter !== SampleFilter.NONE) {
      if (this.data.excludeCompromised) {
        additionalText.push(EXCLUDE_COMPROMISED);
      }
      if (this.data.excludeInternal) {
        additionalText.push(EXCLUDE_INTERNAL);
      }
    }
    if (this.data.plasma) {
      additionalText.push(PLASMA);
    }

    return {
      title: sampleFilterDescriptions[this.data.sampleFilter],
      additionalText: additionalText,
    };
  }

  generateFilter() {
    const filters: (Filter | null)[] = [
      sampleFilterFilters[this.data.sampleFilter] ?? null,
    ];
    if (this.data.sampleFilter !== SampleFilter.NONE) {
      if (this.data.excludeCompromised) {
        filters.push({
          type: FilterType.Unary,
          operator: UnaryFilterOperator.Not,
          operand: {
            type: FilterType.Attribute,
            attribute: "biovu_sample_is_compromised",
            values: [true],
          },
        });
      }
      if (this.data.excludeInternal) {
        filters.push({
          type: FilterType.Unary,
          operator: UnaryFilterOperator.Not,
          operand: {
            type: FilterType.Attribute,
            attribute: "biovu_sample_is_nonshippable",
            values: [true],
          },
        });
      }
    }
    if (this.data.plasma) {
      filters.push({
        type: FilterType.Attribute,
        attribute: "biovu_sample_has_plasma",
        values: [true],
      });
    }

    return makeArrayFilter({}, filters);
  }

  filterEntityIds() {
    return [""];
  }
}

type BioVUInlineProps = {
  groupId: string;
  criteriaId: string;
  config: CriteriaConfig;
  data: Data;
};

function BioVUInline(props: BioVUInlineProps) {
  const updateCriteria = useUpdateCriteria(props.groupId, props.criteriaId);

  const onSelectOperator = (event: SelectChangeEvent<string>) => {
    const {
      target: { value: sel },
    } = event;
    updateCriteria(
      produce(props.data, (data) => {
        data.sampleFilter = sel as SampleFilter;
      })
    );
  };

  return (
    <GridLayout rows spacing={1} height="auto">
      <FormControl
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
        }}
      >
        <Select
          value={props.data.sampleFilter}
          input={<OutlinedInput />}
          onChange={onSelectOperator}
        >
          {Object.values(SampleFilter).map((f) => (
            <MenuItem key={f} value={f}>
              {sampleFilterDescriptions[f as SampleFilter]}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      {props.data.sampleFilter !== SampleFilter.NONE ? (
        <GridLayout cols rowAlign="middle">
          <GridBox
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
            }}
          >
            <Checkbox
              checked={props.data.excludeCompromised}
              onChange={() =>
                updateCriteria(
                  produce(props.data, (data) => {
                    data.excludeCompromised = !data.excludeCompromised;
                  })
                )
              }
            />
          </GridBox>
          <Typography variant="body1">{EXCLUDE_COMPROMISED}</Typography>
          <Tooltip title={EXCLUDE_COMPROMISED_TOOLTIP}>
            <InfoIcon sx={{ display: "flex", ml: 1 }} />
          </Tooltip>
        </GridLayout>
      ) : null}
      {props.data.sampleFilter !== SampleFilter.NONE ? (
        <GridLayout cols rowAlign="middle">
          <GridBox
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
            }}
          >
            <Checkbox
              checked={props.data.excludeInternal}
              onChange={() =>
                updateCriteria(
                  produce(props.data, (data) => {
                    data.excludeInternal = !data.excludeInternal;
                  })
                )
              }
            />
          </GridBox>
          <Typography variant="body1">{EXCLUDE_INTERNAL}</Typography>
          <Tooltip title={EXCLUDE_INTERNAL_TOOLTIP}>
            <InfoIcon sx={{ display: "flex", ml: 1 }} />
          </Tooltip>
        </GridLayout>
      ) : null}
      <GridLayout cols rowAlign="middle">
        <GridBox
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
          }}
        >
          <Checkbox
            checked={props.data.plasma}
            onChange={() =>
              updateCriteria(
                produce(props.data, (data) => {
                  data.plasma = !data.plasma;
                })
              )
            }
          />
        </GridBox>
        <Typography variant="body1">{PLASMA}</Typography>
      </GridLayout>
    </GridLayout>
  );
}

async function search(
  underlaySource: UnderlaySource,
  c: CriteriaConfig,
  query: string
): Promise<DataEntry[]> {
  const [re] = safeRegExp(query);
  const results: DataEntry[] = [];

  Object.values(SampleFilter).forEach((value) => {
    const name = sampleFilterDescriptions[value];
    if (name.search(re) >= 0) {
      results.push({
        key: value,
        name: name,
      });
    }
  });

  return results;
}
