import AccountTreeIcon from "@mui/icons-material/AccountTree";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import Checkbox from "components/checkbox";
import Loading from "components/loading";
import { Search } from "components/search";
import {
  TreeGrid,
  TreeGridColumn,
  TreeGridData,
  TreeGridId,
  TreeGridItem,
  TreeGridRowData,
} from "components/treegrid";
import { DataEntry, DataKey } from "data/configuration";
import { FilterType } from "data/filter";
import {
  ClassificationNode,
  SearchClassificationResult,
  Source,
  useSource,
} from "data/source";
import { useAsyncWithApi } from "errors";
import { useUpdateCriteria } from "hooks";
import produce from "immer";
import React, { useCallback, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { CriteriaConfig } from "underlaysSlice";
import { useImmer } from "use-immer";

type Selection = {
  key: DataKey;
  name: string;
};

// A custom TreeGridItem allows us to store the ClassificationNode along with
// the rest of the data.
type ClassificationNodeItem = TreeGridItem & {
  node: ClassificationNode;
};

interface Config extends CriteriaConfig {
  columns: TreeGridColumn[];
  nameColumnIndex?: number;
  hierarchyColumns?: TreeGridColumn[];
  occurrence: string;
  classification: string;
  multiSelect?: boolean;
}

// Exported for testing purposes.
export interface Data {
  selected: Selection[];
}

@registerCriteriaPlugin(
  "concept",
  (source: Source, c: CriteriaConfig, dataEntry?: DataEntry) => {
    const config = c as Config;

    const data: Data = {
      selected: [],
    };

    if (dataEntry) {
      const column = config.columns[config.nameColumnIndex ?? 0];
      data.selected.push({
        key: dataEntry.key,
        name: String(dataEntry[column.key]) ?? "",
      });
    }

    return data;
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

  renderEdit() {
    return <ConceptEdit data={this.data} config={this.config} />;
  }

  renderInline() {
    return <ConceptInline />;
  }

  displayDetails() {
    if (this.data.selected.length > 0) {
      return {
        title: this.data.selected[0].name,
        standaloneTitle: true,
        additionalText: this.data.selected.slice(1).map((s) => s.name),
      };
    }

    return {
      title: "(any)",
    };
  }

  generateFilter() {
    return {
      type: FilterType.Classification,
      occurrenceID: this.config.occurrence,
      classificationID: this.config.classification,
      keys: this.data.selected.map(({ key }) => key),
    };
  }

  occurrenceID() {
    return this.config.occurrence;
  }
}

function keyForNode(node: ClassificationNode): DataKey {
  let key = node.data.key;
  if (node.grouping) {
    key = `${node.grouping}~${key}`;
  }
  return key;
}

type ConceptEditProps = {
  data: Data;
  config: Config;
};

function ConceptEdit(props: ConceptEditProps) {
  const navigate = useNavigate();
  const source = useSource();
  const occurrence = source.lookupOccurrence(props.config.occurrence);
  const classification = source.lookupClassification(
    props.config.occurrence,
    props.config.classification
  );
  const updateCriteria = useUpdateCriteria();

  const [hierarchy, setHierarchy] = useState<DataKey[] | undefined>();
  const [query, setQuery] = useState<string>("");
  const [data, updateData] = useImmer<TreeGridData>({});

  const processEntities = useCallback(
    (
      res: SearchClassificationResult,
      hierarchy?: DataKey[],
      parent?: DataKey
    ) => {
      updateData((data) => {
        const children: DataKey[] = [];
        res.nodes.forEach((node) => {
          const rowData: TreeGridRowData = { ...node.data };
          if (node.ancestors) {
            rowData.view_hierarchy = (
              <Stack alignItems="center">
                <IconButton
                  size="small"
                  onClick={() => {
                    setHierarchy(node.ancestors);
                  }}
                >
                  <AccountTreeIcon fontSize="inherit" />
                </IconButton>
              </Stack>
            );
          }

          const key = keyForNode(node);
          children.push(key);

          // Copy over existing children in case they're being loaded in
          // parallel.
          let childChildren = data[key]?.children;
          if (!childChildren) {
            if (!node.grouping && !hierarchy) {
              childChildren = [];
            }
          }

          const cItem: ClassificationNodeItem = {
            data: rowData,
            children: childChildren,
            node: node,
          };
          data[key] = cItem;
        });

        if (parent) {
          // Store children even if the data isn't loaded yet.
          data[parent] = { ...data[parent], children };
        } else {
          data.root = {
            children: [...(data?.root?.children || []), ...children],
            data: {},
          };
        }
      });
    },
    []
  );

  const attributes = useMemo(
    () => props.config.columns.map(({ key }) => key),
    [props.config.columns]
  );

  const fetchClassification = useCallback(() => {
    updateData(() => ({}));
    return source
      .searchClassification(attributes, occurrence.id, classification.id, {
        query: !hierarchy ? query : undefined,
        includeGroupings: !hierarchy,
      })
      .then((res) => processEntities(res, hierarchy));
  }, [source, attributes, processEntities, hierarchy, query]);
  const classificationState = useAsyncWithApi<void>(fetchClassification);

  const hierarchyColumns = useMemo(() => {
    const columns: TreeGridColumn[] = [
      ...(props.config.hierarchyColumns ?? []),
    ];
    if (columns.length > 0) {
      columns[0] = {
        ...columns[0],
        title: (
          <Button
            variant="contained"
            onClick={() => {
              setHierarchy(undefined);
            }}
          >
            Return to List
          </Button>
        ),
      };
    }
    return columns;
  }, [props.config.hierarchyColumns]);

  const allColumns: TreeGridColumn[] = useMemo(
    () => [
      ...props.config.columns,
      ...(classification.hierarchical
        ? [{ key: "view_hierarchy", width: 70, title: "Hierarchy" }]
        : []),
    ],
    [props.config.columns]
  );

  const nameColumnIndex = props.config.nameColumnIndex ?? 0;

  return (
    <Box
      sx={{
        p: 1,
        minHeight: "100%",
        backgroundColor: (theme) => theme.palette.background.paper,
      }}
    >
      {!hierarchy && (
        <Search
          placeholder="Search by code or description"
          onSearch={setQuery}
        />
      )}
      <Loading status={classificationState}>
        <TreeGrid
          columns={hierarchy ? hierarchyColumns : allColumns}
          data={data}
          defaultExpanded={hierarchy}
          rowCustomization={(id: TreeGridId, rowData: TreeGridRowData) => {
            // TODO(tjennison): Make TreeGridData's type generic so we can avoid
            // this type assertion. Also consider passing the TreeGridItem to
            // the callback instead of the TreeGridRowData.
            const item = data[id] as ClassificationNodeItem;
            if (!item || item.node.grouping) {
              return undefined;
            }

            const column = props.config.columns[nameColumnIndex];
            const name = rowData[column.key];
            const newItem = {
              key: item.node.data.key,
              name: !!name ? String(name) : "",
            };

            if (props.config.multiSelect) {
              const index = props.data.selected.findIndex(
                (sel) => item.node.data.key === sel.key
              );

              return new Map([
                [
                  nameColumnIndex,
                  {
                    prefixElements: (
                      <Checkbox
                        size="small"
                        fontSize="inherit"
                        checked={index > -1}
                        onChange={() => {
                          updateCriteria(
                            produce(props.data, (data) => {
                              if (index > -1) {
                                data.selected.splice(index, 1);
                              } else {
                                data.selected.push(newItem);
                              }
                            })
                          );
                        }}
                      />
                    ),
                  },
                ],
              ]);
            }

            return new Map([
              [
                nameColumnIndex,
                {
                  onClick: () => {
                    updateCriteria(
                      produce(props.data, (data) => {
                        data.selected = [newItem];
                      })
                    );
                    navigate("..");
                  },
                },
              ],
            ]);
          }}
          loadChildren={(id: TreeGridId) => {
            const item = data[id] as ClassificationNodeItem;
            const key = item?.node ? keyForNode(item.node) : id;
            if (item?.node.grouping) {
              return source
                .searchGrouping(
                  attributes,
                  occurrence.id,
                  classification.id,
                  item.node
                )
                .then((res) => {
                  processEntities(res, hierarchy, key);
                });
            } else {
              return source
                .searchClassification(
                  attributes,
                  occurrence.id,
                  classification.id,
                  {
                    parent: key,
                  }
                )
                .then((res) => {
                  processEntities(res, hierarchy, key);
                });
            }
          }}
        />
      </Loading>
    </Box>
  );
}

function ConceptInline() {
  return null;
}

function search(
  source: Source,
  c: CriteriaConfig,
  query: string
): Promise<DataEntry[]> {
  const config = c as Config;
  return source
    .searchClassification(
      config.columns.map(({ key }) => key),
      config.occurrence,
      config.classification,
      {
        query,
      }
    )
    .then((res) => res.nodes.map((node) => node.data));
}
