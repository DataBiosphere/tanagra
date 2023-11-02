import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import {
  ANY_VALUE_DATA,
  generateValueDataFilter,
  ValueConfig,
  ValueData,
  ValueDataEdit,
} from "criteria/valueData";
import { ROLLUP_COUNT_ATTRIBUTE } from "data/configuration";
import { Source } from "data/source";
import { DataEntry } from "data/types";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import { CriteriaConfig } from "underlaysSlice";
import { isValid } from "util/valid";

export interface Config extends CriteriaConfig {
  occurrence: string;
  singleValue?: boolean;
  valueConfigs: ValueConfig[];
}

export interface Data {
  valueData: ValueData[];
}

// "multiAttribute" plugins select occurrences based on one or more attributes
// that can be selected by the user. It supports two modes, one where a single
// value is switched between multiple attributes and another where multiple
// attributes can be set simultaneously.
@registerCriteriaPlugin(
  "multiAttribute",
  (source: Source, c: CriteriaConfig, dataEntry?: DataEntry) => {
    const valueData: ValueData[] = [];
    if (dataEntry) {
      valueData.push({
        ...ANY_VALUE_DATA,
        attribute: String(dataEntry.t_attribute),
        numeric: false,
        selected: [{ name: String(dataEntry.name), value: dataEntry.key }],
      });
    }

    return {
      valueData,
    };
  },
  search
)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;
  private config: Config;

  constructor(public id: string, config: CriteriaConfig, data: unknown) {
    this.config = config as Config;
    this.data = data as Data;
  }

  renderInline(groupId: string) {
    if (!this.config.valueConfigs) {
      return null;
    }

    return (
      <MultiAttributeInline
        groupId={groupId}
        criteriaId={this.id}
        data={this.data}
        config={this.config}
      />
    );
  }

  displayDetails() {
    if (this.data.valueData.length === 0) {
      return {
        title: "(any)",
      };
    }

    const details = this.data.valueData.map((vd) => {
      if (vd.numeric) {
        return {
          title: `${vd.range.min} - ${vd.range.max}`,
        };
      }

      if (vd.selected.length === 0) {
        return {
          title: "(any)",
        };
      }

      if (vd.selected.length === 1) {
        return {
          title: vd.selected[0].name,
        };
      }

      return {
        title: `(${vd.selected.length} selected)`,
        additionalText: vd.selected.map((s) => s.name),
      };
    });

    if (this.config.singleValue || this.config.valueConfigs.length === 1) {
      return details[0];
    }

    const title = this.data.valueData
      .map((vd, i) => {
        const title = this.config.valueConfigs.find(
          (c) => c.attribute === vd.attribute
        )?.title;
        return (title ? title + ": " : "") + details[i].title;
      })
      .join(", ");
    const additionalText = this.data.valueData
      .map((vd, i) => {
        const at = details[i].additionalText;
        if (!at) {
          return undefined;
        }
        const title = this.config.valueConfigs.find(
          (c) => c.attribute === vd.attribute
        )?.title;
        return (title ? title + ": " : "") + at.join(", ");
      })
      .filter(isValid);

    return {
      title,
      additionalText,
    };
  }

  generateFilter() {
    return generateValueDataFilter(this.data.valueData);
  }

  filterOccurrenceIds() {
    return [this.config.occurrence];
  }
}

type MultiAttributeInlineProps = {
  groupId: string;
  criteriaId: string;
  data: Data;
  config: Config;
};

function MultiAttributeInline(props: MultiAttributeInlineProps) {
  const updateCriteria = useUpdateCriteria(props.groupId, props.criteriaId);

  if (!props.config.valueConfigs) {
    return null;
  }

  return (
    <ValueDataEdit
      occurrence={props.config.occurrence}
      valueConfigs={props.config.valueConfigs}
      valueData={props.data.valueData}
      update={(valueData) =>
        updateCriteria(
          produce(props.data, (data) => {
            data.valueData = valueData;
          })
        )
      }
    />
  );
}

async function search(
  source: Source,
  c: CriteriaConfig,
  query: string
): Promise<DataEntry[]> {
  const config = c as Config;

  const allHintData = await source.getAllHintData(config.occurrence);

  const re = new RegExp(query, "i");
  const results: DataEntry[] = [];
  allHintData.forEach((hintData) => {
    if (!hintData?.enumHintOptions) {
      return;
    }

    hintData.enumHintOptions.forEach((hint) => {
      const key = hint.value;
      if (
        (typeof key === "string" || typeof key === "number") &&
        hint.name.search(re) >= 0
      ) {
        results.push({
          key: key,
          t_attribute: hintData.attribute,
          name: hint.name,
          [ROLLUP_COUNT_ATTRIBUTE]: hint.count,
        });
      }
    });
  });

  return results.sort(
    (a, b) =>
      (b[ROLLUP_COUNT_ATTRIBUTE] as number) -
      (a[ROLLUP_COUNT_ATTRIBUTE] as number)
  );
}
