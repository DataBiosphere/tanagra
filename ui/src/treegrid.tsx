import ErrorIcon from "@mui/icons-material/Error";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import KeyboardArrowRightIcon from "@mui/icons-material/KeyboardArrowRight";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import { ReactNode, useCallback } from "react";
import { useImmer } from "use-immer";

export type TreeGridId = string | number;

export type TreeGridRowData = {
  [key: string]: undefined | string | number | boolean | JSX.Element;
};

export type TreeGridItem = {
  children?: TreeGridId[];
  data: TreeGridRowData;
};

export type TreeGridData = {
  [key: TreeGridId]: TreeGridItem;
};

export type TreeGridColumn = {
  key: string;
  width: string | number;
  title?: string | JSX.Element;
};

export type TreeGridProps = {
  columns: TreeGridColumn[];
  data: TreeGridData;
  prefixElements?: (id: TreeGridId, data: TreeGridRowData) => ReactNode;
  // The returned string is displayed as an error message for failures and
  // ignored for successes.
  loadChildren?: (id: TreeGridId) => Promise<string>;
};

export function TreeGrid(props: TreeGridProps) {
  const [state, updateState] = useImmer<TreeGridState>(
    new Map<TreeGridId, TreeGridItemState>()
  );

  const toggleExpanded = useCallback(
    (id: TreeGridId) => {
      updateState((draft) => {
        const itemState = draft.get(id) || {
          status: Status.Collapsed,
        };
        switch (itemState.status) {
          case Status.Loading:
            return;
          case Status.Expanded:
            itemState.status = Status.Collapsed;
            break;
          default:
            if (!props.loadChildren || !!itemState.loaded) {
              itemState.status = Status.Expanded;
            } else {
              itemState.status = Status.Loading;
              props
                .loadChildren(id)
                .then(() => {
                  updateState((draft) => {
                    draft.set(id, {
                      status: Status.Expanded,
                      loaded: true,
                    });
                  });
                })
                .catch((error) => {
                  updateState((draft) => {
                    draft.set(id, {
                      status: Status.Failed,
                      errorMessage: error,
                    });
                  });
                });
            }
        }
        draft.set(id, itemState);
      });
    },
    [props.loadChildren]
  );

  return (
    <>
      <table
        style={{
          tableLayout: "fixed",
          width: "100%",
        }}
      >
        <thead
          style={{
            display: "block",
          }}
        >
          <tr>
            {props.columns.map((col, i) => (
              <td
                key={i}
                style={{
                  ...(col.width && {
                    maxWidth: col.width,
                    width: col.width,
                    minWidth: col.width,
                  }),
                }}
              >
                <Typography variant="h6">{col.title}</Typography>
              </td>
            ))}
          </tr>
        </thead>
        <tbody
          style={{
            overflowY: "auto",
            display: "block",
          }}
        >
          {renderChildren(props, state, toggleExpanded, "root", 0, false)}
        </tbody>
      </table>
    </>
  );
}

function renderChildren(
  props: TreeGridProps,
  state: TreeGridState,
  toggleExpanded: (id: TreeGridId) => void,
  id: TreeGridId,
  indent: number,
  collapse: boolean
): JSX.Element[] {
  const results: JSX.Element[] = [];

  props.data[id]?.children?.forEach((childId) => {
    const child = props.data[childId];
    if (!child) {
      return;
    }
    const childState = state.get(childId);

    results.push(
      <tr
        key={childId}
        style={{
          ...(collapse && { visibility: "collapse" }),
        }}
      >
        {props.columns.map((col, i) => {
          let value = child.data[col.key] || "";
          let title = "";
          // Stringify values other than Elements.
          if (!(value instanceof Object)) {
            value = String(value);
            title = value;
          }

          return (
            <td
              key={i}
              style={{
                ...(col.width && {
                  maxWidth: col.width,
                  width: col.width,
                  minWidth: col.width,
                }),
              }}
            >
              <div
                style={{
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                  overflow: "hidden",
                  ...(i === 0 && { paddingLeft: `${indent}em` }),
                }}
              >
                <Typography
                  variant="body1"
                  noWrap
                  title={title}
                  style={{
                    display: "inline",
                  }}
                >
                  {i === 0 && (
                    <>
                      {child.children?.length && (
                        <IconButton
                          size="small"
                          title={childState?.errorMessage}
                          onClick={() => {
                            toggleExpanded(childId);
                          }}
                        >
                          <ItemIcon state={childState} />
                        </IconButton>
                      )}
                      {props.prefixElements?.(childId, child.data)}
                    </>
                  )}
                  {value}
                </Typography>
              </div>
            </td>
          );
        })}
      </tr>
    );

    results.push(
      ...renderChildren(
        props,
        state,
        toggleExpanded,
        childId,
        indent + 1,
        collapse || childState?.status !== Status.Expanded
      )
    );
  });
  return results;
}

enum Status {
  Collapsed,
  Expanded,
  Loading,
  Failed,
}

type TreeGridItemState = {
  status: Status;
  loaded?: boolean;
  errorMessage?: string;
};

type TreeGridState = Map<TreeGridId, TreeGridItemState>;

type ItemIconProps = {
  state?: TreeGridItemState;
};

function ItemIcon(props: ItemIconProps) {
  switch (props.state?.status) {
    case Status.Expanded:
      return <KeyboardArrowDownIcon fontSize="inherit" />;
    case Status.Loading:
      return <CircularProgress style={{ width: "1em", height: "1em" }} />;
    case Status.Failed:
      return <ErrorIcon fontSize="inherit" />;
  }
  return <KeyboardArrowRightIcon fontSize="inherit" />;
}
