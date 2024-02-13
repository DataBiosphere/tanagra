import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import {
  ANY_VALUE_DATA,
  decodeValueData,
  encodeValueData,
  generateValueDataFilter,
  ValueConfig,
  ValueData,
  ValueDataEdit,
} from "criteria/valueData";
import { ROLLUP_COUNT_ATTRIBUTE } from "data/configuration";
import { UnderlaySource } from "data/source";
import { DataEntry } from "data/types";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import * as dataProto from "proto/criteriaselector/dataschema/multi_attribute";
import { useCallback, useMemo } from "react";
import { CriteriaConfig } from "underlaysSlice";
import { base64ToBytes, bytesToBase64 } from "util/base64";
import { safeRegExp } from "util/safeRegExp";
import { isValid } from "util/valid";

export interface Config extends CriteriaConfig {
  entity: string;
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
  (
    underlaySource: UnderlaySource,
    c: CriteriaConfig,
    dataEntry?: DataEntry
  ) => {
    const valueData: ValueData[] = [];
    if (dataEntry) {
      valueData.push({
        ...ANY_VALUE_DATA,
        attribute: String(dataEntry.t_attribute),
        numeric: false,
        selected: [{ name: String(dataEntry.name), value: dataEntry.key }],
      });
    }

    return encodeData({
      valueData,
    });
  },
  search
)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<string> {
  public data: string;
  private config: Config;

  constructor(public id: string, config: CriteriaConfig, data: string) {
    this.config = config as Config;
    try {
      this.data = encodeData(JSON.parse(data));
    } catch (e) {
      this.data = data;
    }
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
    const decodedData = decodeData(this.data);

    if (decodedData.valueData.length === 0) {
      return {
        title: "(any)",
      };
    }

    const details = decodedData.valueData.map((vd) => {
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

    const title = decodedData.valueData
      .map((vd, i) => {
        const title = this.config.valueConfigs.find(
          (c) => c.attribute === vd.attribute
        )?.title;
        return (title ? title + ": " : "") + details[i].title;
      })
      .join(", ");
    const additionalText = decodedData.valueData
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
    const decodedData = decodeData(this.data);
    return generateValueDataFilter(decodedData.valueData);
  }

  filterEntityIds() {
    return [this.config.entity];
  }
}

type MultiAttributeInlineProps = {
  groupId: string;
  criteriaId: string;
  data: string;
  config: Config;
};

function MultiAttributeInline(props: MultiAttributeInlineProps) {
  const updateEncodedCriteria = useUpdateCriteria(
    props.groupId,
    props.criteriaId
  );
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);

  if (!props.config.valueConfigs) {
    return null;
  }

  return (
    <ValueDataEdit
      hintEntity={props.config.entity}
      valueConfigs={props.config.valueConfigs}
      valueData={decodedData.valueData}
      update={(valueData) =>
        updateCriteria(
          produce(decodedData, (data) => {
            data.valueData = valueData;
          })
        )
      }
    />
  );
}

async function search(
  underlaySource: UnderlaySource,
  c: CriteriaConfig,
  query: string
): Promise<DataEntry[]> {
  const config = c as Config;

  const allHintData = await underlaySource.getAllHintData(config.entity);

  const [re] = safeRegExp(query);
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

function decodeData(data: string): Data {
  const message = dataProto.MultiAttribute.decode(base64ToBytes(data));
  return {
    valueData: message.valueData?.map((vd) => decodeValueData(vd)) ?? [],
  };
}

function encodeData(data: Data): string {
  const message: dataProto.MultiAttribute = {
    valueData: data.valueData.map((vd) => encodeValueData(vd)),
  };
  return bytesToBase64(dataProto.MultiAttribute.encode(message).finish());
}
