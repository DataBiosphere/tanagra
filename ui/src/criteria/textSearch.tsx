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
import { EnumHintOption, useSource } from "data/source";
import { DataValue } from "data/types";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import { useRef } from "react";
import useSWRImmutable from "swr/immutable";
import { CriteriaConfig } from "underlaysSlice";
import { isValid } from "util/valid";

interface Config extends CriteriaConfig {
  occurrenceId: string;
  searchAttribute: string;
  categoryAttribute?: string;
}

type Selection = {
  value: DataValue;
  name: string;
};

interface Data {
  query: string;
  categories: Selection[];
}

// "search" plugins select occurrences using a text based search of the fields
// in the occurrence.
@registerCriteriaPlugin("search", () => {
  return {
    query: "",
    categories: [],
  };
})
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;
  private config: Config;

  constructor(public id: string, config: CriteriaConfig, data: unknown) {
    this.config = config as Config;
    this.data = data as Data;
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
    let additionalText = ["Any category"];
    if (this.data.categories.length > 0) {
      additionalText = [
        "Selected categories:",
        ...this.data.categories.map((c) => c.name),
      ];
    }

    let title = "Any";
    if (this.data.query.length > 0) {
      title = `"${this.data.query}"`;
    }

    if (this.data.categories.length > 0) {
      if (this.data.categories.length < 3) {
        title = `${title} in [${this.data.categories
          .map((c) => c.name)
          .join(", ")}]`;
      } else {
        title = `${title} in ${this.data.categories.length} categories`;
      }
    }

    return {
      title,
      additionalText,
    };
  }

  generateFilter() {
    return makeArrayFilter({}, [
      {
        type: FilterType.Text,
        attribute: this.config.searchAttribute,
        text: this.data.query,
      },
      {
        type: FilterType.Attribute,
        attribute: this.config.categoryAttribute ?? "",
        values: this.data.categories.map((c) => c.value),
      },
    ]);
  }

  filterOccurrenceId() {
    return this.config.occurrenceId;
  }
}

type TextSearchInlineProps = {
  groupId: string;
  config: Config;
  data: Data;
};

function TextSearchInline(props: TextSearchInlineProps) {
  const source = useSource();
  const updateCriteria = useUpdateCriteria(props.groupId);
  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  const onUpdateQuery = (query: string) => {
    if (searchTimeout.current) {
      clearTimeout(searchTimeout.current);
    }

    updateCriteria(
      produce(props.data, (data) => {
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
        ? await source.getHintData(
            props.config.occurrenceId,
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
      produce(props.data, (data) => {
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

  return (
    <Loading status={hintDataState}>
      {!!hintDataState.data?.hintData?.enumHintOptions ? (
        <FormControl sx={{ maxWidth: 500 }}>
          <TextField
            label="Search text"
            defaultValue={props.data.query}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
              onQueryChange(event.target.value);
            }}
          />
          <Select
            fullWidth
            multiple
            displayEmpty
            value={props.data.categories.map((c) => c.name)}
            input={<OutlinedInput />}
            renderValue={(categories) => (
              <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5 }}>
                {categories.length > 0 ? (
                  categories.map((c) => <Chip key={c} label={c} />)
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
        </FormControl>
      ) : null}
    </Loading>
  );
}
