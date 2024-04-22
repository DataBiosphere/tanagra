import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import { CommonSelectorConfig } from "data/source";
import * as configProto from "proto/criteriaselector/configschema/output_unfiltered";
import * as dataProto from "proto/criteriaselector/dataschema/output_unfiltered";
import { base64ToBytes } from "util/base64";

interface Data {
  entities: string[];
}

@registerCriteriaPlugin("outputUnfiltered", () => {
  return encodeData({
    entities: [],
  });
})
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<string> {
  public data: string;
  private selector: CommonSelectorConfig;
  private config: configProto.OutputUnfiltered;

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

  renderInline() {
    return null;
  }

  displayDetails() {
    return {
      title: "",
    };
  }

  generateFilter() {
    return null;
  }

  filterEntityIds() {
    const decodedData = decodeData(this.data);

    if (decodedData.entities?.length) {
      return decodedData.entities;
    }

    if (this.config.entities?.length) {
      return this.config.entities;
    }

    return [""];
  }
}

function decodeData(data: string): Data {
  const message =
    data[0] === "{"
      ? dataProto.OutputUnfiltered.fromJSON(JSON.parse(data))
      : dataProto.OutputUnfiltered.decode(base64ToBytes(data));

  return {
    entities: message.entities,
  };
}

function encodeData(data: Data): string {
  const message: dataProto.OutputUnfiltered = {
    entities: data.entities,
  };
  return JSON.stringify(dataProto.OutputUnfiltered.toJSON(message));
}

function decodeConfig(
  selector: CommonSelectorConfig
): configProto.OutputUnfiltered {
  return configProto.OutputUnfiltered.fromJSON(
    JSON.parse(selector.pluginConfig)
  );
}
