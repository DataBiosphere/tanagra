import AccountTreeIcon from "@mui/icons-material/AccountTree";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
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
import { DataKey } from "data/configuration";
import { FilterType } from "data/filter";
import {
  ClassificationNode,
  SearchClassificationResult,
  Source,
  useSource,
} from "data/source";
import { useAsyncWithApi } from "errors";
import produce from "immer";
import React, { useCallback, useMemo, useState } from "react";
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
}

// Exported for testing purposes.
export interface Data extends Config {
  selected: Selection[];
}

@registerCriteriaPlugin(
  "concept",
  (source: Source, config: CriteriaConfig) => ({
    ...(config.plugin as Config),
    selected: [],
  })
)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;

  constructor(public id: string, data: unknown) {
    this.data = data as Data;
  }

  renderEdit(dispatchFn: (data: Data) => void) {
    return <ConceptEdit dispatchFn={dispatchFn} data={this.data} />;
  }

  renderDetails() {
    return <ConceptDetails data={this.data} />;
  }

  generateFilter() {
    return {
      type: FilterType.Classification,
      occurrenceID: this.data.occurrence,
      classificationID: this.data.classification,
      keys: this.data.selected.map(({ key }) => key),
    };
  }

  occurrenceID() {
    return this.data.occurrence;
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
  dispatchFn: (data: Data) => void;
  data: Data;
};

function ConceptEdit(props: ConceptEditProps) {
  const source = useSource();
  const occurrence = source.lookupOccurrence(props.data.occurrence);
  const classification = source.lookupClassification(
    props.data.occurrence,
    props.data.classification
  );

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
              <IconButton
                size="small"
                onClick={() => {
                  setHierarchy(node.ancestors);
                }}
              >
                <AccountTreeIcon fontSize="inherit" />
              </IconButton>
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
    () => props.data.columns.map(({ key }) => key),
    [props.data.columns]
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
    const columns: TreeGridColumn[] = [...(props.data.hierarchyColumns ?? [])];
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
  }, [props.data.hierarchyColumns]);

  const allColumns: TreeGridColumn[] = useMemo(
    () => [
      ...props.data.columns,
      ...(classification.hierarchical
        ? [{ key: "view_hierarchy", width: 160, title: "View Hierarchy" }]
        : []),
    ],
    [props.data.columns]
  );

  return (
    <>
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
          prefixElements={(id: TreeGridId, rowData: TreeGridRowData) => {
            // TODO(tjennison): Make TreeGridData's type generic so we can avoid
            // this type assertion. Also consider passing the TreeGridItem to
            // the callback instead of the TreeGridRowData.
            const item = data[id] as ClassificationNodeItem;
            if (!item || item.node.grouping) {
              return null;
            }

            const index = props.data.selected.findIndex(
              (sel) => item.node.data.key === sel.key
            );

            return (
              <Checkbox
                size="small"
                fontSize="inherit"
                checked={index > -1}
                onChange={() => {
                  props.dispatchFn(
                    produce(props.data, (data) => {
                      if (index > -1) {
                        data.selected.splice(index, 1);
                      } else {
                        const column =
                          props.data.columns[props.data.nameColumnIndex ?? 0];
                        const name = rowData[column.key];
                        data.selected.push({
                          key: item.node.data.key,
                          name: !!name ? String(name) : "",
                        });
                      }
                    })
                  );
                }}
              />
            );
          }}
          loadChildren={(id: TreeGridId) => {
            const item = data[id] as ClassificationNodeItem;
            const key = item ? keyForNode(item.node) : id;
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
        props.data.selected.map(({ key, name }) => (
          <Stack direction="row" alignItems="baseline" key={key}>
            <Typography variant="body1">{key}</Typography>&nbsp;
            <Typography variant="body2">{name}</Typography>
          </Stack>
        ))
      )}
    </>
  );
}
