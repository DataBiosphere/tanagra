import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import FormControl from "@mui/material/FormControl";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import TextField from "@mui/material/TextField";
import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import Loading from "components/loading";
import { FilterType, makeArrayFilter } from "data/filter";
import {
  CommonSelectorConfig,
  dataValueFromProto,
  EnumHintOption,
  protoFromDataValue,
} from "data/source";
import { DataValue } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import GridLayout from "layout/gridLayout";
import * as configProto from "proto/criteriaselector/configschema/text_search";
import * as dataProto from "proto/criteriaselector/dataschema/text_search";
import { useCallback, useMemo, useRef } from "react";
import useSWRImmutable from "swr/immutable";
import { base64ToBytes } from "util/base64";
import { isValid } from "util/valid";

type Selection = {
  value: DataValue;
  name: string;
};

interface Data {
  query: string;
  categories: Selection[];
}

// "search" plugins select entities using a text based search of the fields in
// the entity.
@registerCriteriaPlugin("search", () => {
  return encodeData({
    query: "",
    categories: [],
  });
})
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<string> {
  public data: string;
  private selector: CommonSelectorConfig;
  private config: configProto.TextSearch;

  constructor(public id: string, selector: CommonSelectorConfig, data: string) {
    this.selector = selector;
    this.config = decodeConfig(selector);
    this.data = data;
  }

  renderInline(groupId: string) {
    return (
      <TextSearchInline
        groupId={groupId}
        data={this.data}
        config={this.config}
      />
    );
  }

  displayDetails() {
    const decodedData = decodeData(this.data);

    let additionalText = ["Any category"];
    if (decodedData.categories.length > 0) {
      additionalText = [...decodedData.categories.map((c) => c.name)];
    }

    let title = "Any";
    if (decodedData.query.length > 0) {
      title = `"${decodedData.query}"`;
    }

    if (decodedData.categories.length > 0) {
      if (decodedData.categories.length < 3) {
        title = `${title} in [${decodedData.categories
          .map((c) => c.name)
          .join(", ")}]`;
      } else {
        title = `${title} in ${decodedData.categories.length} categories`;
      }
    }

    return {
      title,
      additionalText,
    };
  }

  generateFilter() {
    const decodedData = decodeData(this.data);

    return makeArrayFilter({}, [
      {
        type: FilterType.Text,
        attribute: this.config.searchAttribute,
        text: decodedData.query,
      },
      {
        type: FilterType.Attribute,
        attribute: this.config.categoryAttribute ?? "",
        values: decodedData.categories.map((c) => c.value),
      },
    ]);
  }

  filterEntityIds() {
    return [this.config.entity];
  }
}

type TextSearchInlineProps = {
  groupId: string;
  config: configProto.TextSearch;
  data: string;
};

function TextSearchInline(props: TextSearchInlineProps) {
  const underlaySource = useUnderlaySource();
  const updateEncodedCriteria = useUpdateCriteria(props.groupId);
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);

  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  const onUpdateQuery = (query: string) => {
    if (searchTimeout.current) {
      clearTimeout(searchTimeout.current);
    }

    updateCriteria(
      produce(decodedData, (data) => {
        data.query = query;
      })
    );
  };

  const onQueryChange = (query: string) => {
    if (searchTimeout.current) {
      clearTimeout(searchTimeout.current);
    }

    searchTimeout.current = setTimeout(() => {
      onUpdateQuery(query);
    }, 300);
  };

  const hintDataState = useSWRImmutable(
    { type: "hintData", attribute: props.config.categoryAttribute },
    async () => {
      const hintData = props.config.categoryAttribute
        ? await underlaySource.getHintData(
            props.config.entity,
            props.config.categoryAttribute
          )
        : undefined;
      return {
        hintData,
      };
    }
  );

  const onSelect = (event: SelectChangeEvent<string[]>) => {
    const {
      target: { value: sel },
    } = event;
    updateCriteria(
      produce(decodedData, (data) => {
        if (typeof sel === "string") {
          // This case is only for selects with text input.
          return;
        }
        data.categories = sel
          .map((name) => {
            const value = hintDataState.data?.hintData?.enumHintOptions?.find(
              (hint: EnumHintOption) => hint.name === name
            )?.value;
            if (!isValid(value)) {
              return undefined;
            }
            return {
              name,
              value,
            };
          })
          .filter(isValid);
      })
    );
  };

  const onDelete = (category: string) => {
    updateCriteria(
      produce(decodedData, (data) => {
        data.categories = data.categories.filter((c) => c.name !== category);
      })
    );
  };

  return (
    <Loading status={hintDataState}>
      {!!hintDataState.data?.hintData?.enumHintOptions ? (
        <FormControl
          sx={{ maxWidth: 500 }}
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
          }}
        >
          <GridLayout rows spacing={1} height="auto">
            <TextField
              label="Search text"
              defaultValue={decodedData.query}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                onQueryChange(event.target.value);
              }}
            />
            <Select
              fullWidth
              multiple
              displayEmpty
              value={decodedData.categories.map((c) => c.name)}
              input={<OutlinedInput />}
              renderValue={(categories) => (
                <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5 }}>
                  {categories.length > 0 ? (
                    categories.map((c) => (
                      <Chip
                        key={c}
                        label={c}
                        onMouseDown={(e) => {
                          e.stopPropagation();
                          e.preventDefault();
                        }}
                        onDelete={(e) => {
                          e.stopPropagation();
                          onDelete(c);
                        }}
                      />
                    ))
                  ) : (
                    <em>Any category</em>
                  )}
                </Box>
              )}
              onChange={onSelect}
            >
              {hintDataState.data?.hintData?.enumHintOptions?.map(
                (hint: EnumHintOption) => (
                  <MenuItem key={hint.name} value={hint.name}>
                    {hint.name}
                  </MenuItem>
                )
              )}
            </Select>
          </GridLayout>
        </FormControl>
      ) : null}
    </Loading>
  );
}

function decodeData(data: string): Data {
  const message =
    data[0] === "{"
      ? dataProto.TextSearch.fromJSON(JSON.parse(data))
      : dataProto.TextSearch.decode(base64ToBytes(data));

  return {
    categories:
      message.categories?.map((s) => ({
        value: dataValueFromProto(s.value),
        name: s.name,
      })) ?? [],
    query: message.query,
  };
}

function encodeData(data: Data): string {
  const message: dataProto.TextSearch = {
    categories:
      data.categories?.map((s) => ({
        value: protoFromDataValue(s.value),
        name: s.name,
      })) ?? [],
    query: data.query,
  };
  return JSON.stringify(dataProto.TextSearch.toJSON(message));
}

function decodeConfig(selector: CommonSelectorConfig): configProto.TextSearch {
  return configProto.TextSearch.fromJSON(JSON.parse(selector.pluginConfig));
}
