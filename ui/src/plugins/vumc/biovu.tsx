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
import { CommonSelectorConfig, UnderlaySource } from "data/source";
import { DataEntry } from "data/types";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import * as configProto from "proto/criteriaselector/configschema/biovu";
import * as dataProto from "proto/criteriaselector/dataschema/biovu";
import React, { useCallback, useMemo } from "react";
import { base64ToBytes, bytesToBase64 } from "util/base64";
import { safeRegExp } from "util/safeRegExp";

enum SampleFilter {
  ANY = "Any",
  ONE_HUNDRED = "100",
  FIVE_HUNDRED = "500",
}

const sampleFilterDescriptions = {
  [SampleFilter.ANY]: "Any BioVU DNA (no minimum amount or concentration)",
  [SampleFilter.ONE_HUNDRED]: "At least 100ng with at least 2 ng/µL BioVU DNA",
  [SampleFilter.FIVE_HUNDRED]: "At least 500ng with at least 2 ng/µL BioVU DNA",
};

const sampleFilterFilters: { [key: string]: Filter } = {
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
const PLASMA = "Any banked BioVU Plasma";

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
    c: CommonSelectorConfig,
    dataEntry?: DataEntry
  ) => {
    return encodeData({
      sampleFilter: (dataEntry?.key as SampleFilter) ?? SampleFilter.ANY,
    });
  },
  search
)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<string> {
  public data: string;
  private selector: CommonSelectorConfig;
  private config: configProto.BioVU;

  constructor(
    public id: string,
    selector: CommonSelectorConfig,
    data: string,
    private entity?: string
  ) {
    this.selector = selector;
    this.config = decodeConfig(selector);
    try {
      this.data = encodeData(JSON.parse(data));
    } catch (e) {
      this.data = data;
    }
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
    const decodedData = decodeData(this.data);

    const additionalText: string[] = [];
    if (decodedData.excludeCompromised) {
      additionalText.push(EXCLUDE_COMPROMISED);
    }
    if (decodedData.excludeInternal) {
      additionalText.push(EXCLUDE_INTERNAL);
    }

    return {
      title: this.config.plasmaFilter
        ? PLASMA
        : sampleFilterDescriptions[decodedData.sampleFilter],
      additionalText: additionalText,
    };
  }

  generateFilter() {
    const decodedData = decodeData(this.data);

    const filters: (Filter | null)[] = [
      sampleFilterFilters[decodedData.sampleFilter] ?? null,
    ];
    if (decodedData.excludeCompromised) {
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
    if (decodedData.excludeInternal) {
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
    if (this.config.plasmaFilter) {
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
  config: configProto.BioVU;
  data: string;
};

function BioVUInline(props: BioVUInlineProps) {
  const updateEncodedCriteria = useUpdateCriteria(
    props.groupId,
    props.criteriaId
  );
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);

  const onSelectOperator = (event: SelectChangeEvent<string>) => {
    const {
      target: { value: sel },
    } = event;
    updateCriteria(
      produce(decodedData, (data) => {
        data.sampleFilter = sel as SampleFilter;
      })
    );
  };

  return !props.config.plasmaFilter ? (
    <GridLayout rows spacing={1} height="auto">
      <FormControl
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
        }}
      >
        <Select
          value={decodedData.sampleFilter}
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
      <GridLayout cols rowAlign="middle">
        <GridBox
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
          }}
        >
          <Checkbox
            checked={decodedData.excludeCompromised}
            onChange={() =>
              updateCriteria(
                produce(decodedData, (data) => {
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
      <GridLayout cols rowAlign="middle">
        <GridBox
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
          }}
        >
          <Checkbox
            checked={decodedData.excludeInternal}
            onChange={() =>
              updateCriteria(
                produce(decodedData, (data) => {
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
    </GridLayout>
  ) : null;
}

async function search(
  underlaySource: UnderlaySource,
  c: CommonSelectorConfig,
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

function decodeSampleFilter(
  sampleFilter: dataProto.BioVU_SampleFilter
): SampleFilter {
  switch (sampleFilter) {
    case dataProto.BioVU_SampleFilter.SAMPLE_FILTER_UNKNOWN:
    case dataProto.BioVU_SampleFilter.SAMPLE_FILTER_ANY:
      return SampleFilter.ANY;
    case dataProto.BioVU_SampleFilter.SAMPLE_FILTER_ONE_HUNDRED:
      return SampleFilter.ONE_HUNDRED;
    case dataProto.BioVU_SampleFilter.SAMPLE_FILTER_FIVE_HUNDRED:
      return SampleFilter.FIVE_HUNDRED;
  }
  throw new Error(`Unknown sample filter value ${sampleFilter}.`);
}

function decodeData(data: string): Data {
  const message = dataProto.BioVU.decode(base64ToBytes(data));
  return {
    sampleFilter: decodeSampleFilter(message.sampleFilter),
    excludeCompromised: message.excludeCompromised,
    excludeInternal: message.excludeInternal,
    plasma: message.plasma,
  };
}

function encodeSampleFilter(
  sampleFilter: SampleFilter
): dataProto.BioVU_SampleFilter {
  switch (sampleFilter) {
    case SampleFilter.ANY:
      return dataProto.BioVU_SampleFilter.SAMPLE_FILTER_ANY;
    case SampleFilter.ONE_HUNDRED:
      return dataProto.BioVU_SampleFilter.SAMPLE_FILTER_ONE_HUNDRED;
    case SampleFilter.FIVE_HUNDRED:
      return dataProto.BioVU_SampleFilter.SAMPLE_FILTER_FIVE_HUNDRED;
  }
  throw new Error(`Unknown internal sample filter value ${sampleFilter}.`);
}

function encodeData(data: Data): string {
  const message: dataProto.BioVU = {
    sampleFilter: encodeSampleFilter(data.sampleFilter),
    excludeCompromised: !!data.excludeCompromised,
    excludeInternal: !!data.excludeInternal,
    plasma: !!data.plasma,
  };
  return bytesToBase64(dataProto.BioVU.encode(message).finish());
}

function decodeConfig(selector: CommonSelectorConfig): configProto.BioVU {
  return configProto.BioVU.fromJSON(JSON.parse(selector.pluginConfig));
}
