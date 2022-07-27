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
import { DataKey, findByID } from "data/configuration";
import {
  ClassificationNode,
  SearchClassificationResult,
  useSource,
} from "data/source";
import { useAsyncWithApi } from "errors";
import { useUnderlay } from "hooks";
import produce from "immer";
import React, { useCallback, useMemo, useState } from "react";
import * as tanagra from "tanagra-api";
import { CriteriaConfig, Underlay } from "underlaysSlice";
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
  (underlay: Underlay, config: CriteriaConfig) => ({
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

  // Normally, filter generation assumes the concepts are liked to the entityVar
  // via an occurrence table (e.g. person -> condition_occurrence -> condition).
  // fromOccurrence causes entityVar to be treated as the occurrence instead
  // (e.g. condition_occurrence -> condition).
  generateFilter(
    underlay: Underlay,
    entityVar: string,
    fromOccurrence: boolean
  ) {
    // TODO(tjennison): Package the data config and helper functions into the
    // source then pass that in instead of the underlay which is only being used
    // to access the data config.
    const occurrence = findByID(
      this.data.occurrence,
      underlay.uiConfiguration.dataConfig.occurrences
    );
    const classification = findByID(
      this.data.classification,
      occurrence.classifications
    );

    const operands = this.data.selected.map(({ key }) => ({
      binaryFilter: {
        attributeVariable: {
          variable: classification.entity,
          name: classification.entityAttribute,
        },
        operator: tanagra.BinaryFilterOperator.DescendantOfInclusive,
        attributeValue: {
          // TODO(tjennison): Handle other key types.
          int64Val: key as number,
        },
      },
    }));

    if (!operands) {
      return null;
    }

    const filter = {
      relationshipFilter: {
        outerVariable: fromOccurrence ? entityVar : occurrence.entity,
        newVariable: classification.entity,
        newEntity: classification.entity,
        filter: {
          arrayFilter: {
            operands: operands,
            operator: tanagra.ArrayFilterOperator.Or,
          },
        },
      },
    };

    if (fromOccurrence) {
      return filter;
    }

    return {
      relationshipFilter: {
        outerVariable: entityVar,
        newVariable: occurrence.entity,
        newEntity: occurrence.entity,
        filter: filter,
      },
    };
  }

  // TODO(tjennison): Split filter generation into separate paths for
  // occurrences and primary entities. This will allow occurrence logic to be
  // centralized and remove the limitation of having a single selectable entity.
  occurrenceEntities(underlay: Underlay) {
    return [
      findByID(
        this.data.occurrence,
        underlay.uiConfiguration.dataConfig.occurrences
      ).entity,
    ];
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
  const underlay = useUnderlay();

  const occurrence = findByID(
    props.data.occurrence,
    underlay.uiConfiguration.dataConfig.occurrences
  );
  const classification = findByID(
    props.data.classification,
    occurrence.classifications
  );

  const [hierarchy, setHierarchy] = useState<DataKey[] | undefined>();
  const [query, setQuery] = useState<string>("");
  const [data, updateData] = useImmer<TreeGridData>({});
  const source = useSource();

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
            if (!item) {
              throw new Error(`Unknown TreeGridId "${id}"`);
            }

            const key = keyForNode(item.node);
            if (item.node.grouping) {
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
