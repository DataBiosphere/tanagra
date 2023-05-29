import ErrorIcon from "@mui/icons-material/Error";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import KeyboardArrowRightIcon from "@mui/icons-material/KeyboardArrowRight";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import { useTheme } from "@mui/material/styles";
import Typography from "@mui/material/Typography";
import { GridBox } from "layout/gridBox";
import { ReactNode, useEffect, useRef } from "react";
import { useImmer } from "use-immer";

export type TreeGridId = string | number;
export type TreeGridValue =
  | undefined
  | null
  | string
  | number
  | boolean
  | object
  | JSX.Element
  | Date;

export type TreeGridRowData = {
  [key: string]: TreeGridValue;
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

export type ColumnCustomization = {
  column: number;
  prefixElements?: ReactNode;
  onClick?: () => void;
  content?: ReactNode;
};

export type TreeGridProps = {
  columns: TreeGridColumn[];
  data: TreeGridData;
  defaultExpanded?: TreeGridId[];
  rowCustomization?: (
    id: TreeGridId,
    data: TreeGridRowData
  ) => ColumnCustomization[] | undefined;
  loadChildren?: (id: TreeGridId) => Promise<void>;
  minWidth?: boolean;
  wrapBodyText?: boolean;
};

export function TreeGrid(props: TreeGridProps) {
  const theme = useTheme();

  const [state, updateState] = useImmer<TreeGridState>(
    new Map<TreeGridId, TreeGridItemState>()
  );

  const cancel = useRef<boolean>(false);
  useEffect(
    () => () => {
      cancel.current = true;
    },
    []
  );

  const toggleExpanded = (draft: TreeGridState, id: TreeGridId) => {
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
              if (!cancel.current) {
                updateState((draft) => {
                  draft.set(id, {
                    status: Status.Expanded,
                    loaded: true,
                  });
                });
              }
            })
            .catch((error) => {
              if (!cancel.current) {
                updateState((draft) => {
                  draft.set(id, {
                    status: Status.Failed,
                    errorMessage: error,
                  });
                });
              }
            });
        }
    }
    draft.set(id, itemState);
  };

  useEffect(() => {
    const de = props?.defaultExpanded;
    if (de && de.length > 0) {
      updateState((draft) => {
        draft.clear();
        de.forEach((id) => toggleExpanded(draft, id));
      });
    }
  }, [props.defaultExpanded]);

  const paperColor = theme.palette.background.paper;
  const dividerColor = theme.palette.divider;

  return (
    <GridBox sx={{ overflowY: "auto", px: 1 }}>
      <table
        style={{
          tableLayout: "fixed",
          ...(props.minWidth ? { minWidth: "100%" } : { width: "100%" }),
          textAlign: "left",
          borderCollapse: "collapse",
        }}
      >
        <thead>
          <tr>
            {props.columns.map((col, i) => (
              <th
                key={i}
                style={{
                  position: "sticky",
                  top: 0,
                  ...(col.width && {
                    maxWidth: col.width,
                    width: col.width,
                    minWidth: col.width,
                  }),
                  backgroundColor: paperColor,
                  boxShadow: `inset 0 -1px 0 ${dividerColor}`,
                  zIndex: 1,
                }}
              >
                <Box
                  sx={{
                    textOverflow: "ellipsis",
                    whiteSpace: "nowrap",
                    overflow: "hidden",
                  }}
                >
                  <Typography
                    variant="overline"
                    title={String(col.title)}
                    sx={{
                      display: "inline",
                    }}
                  >
                    {col.title}
                  </Typography>
                </Box>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {renderChildren(
            props,
            state,
            (id: TreeGridId) =>
              updateState((draft) => toggleExpanded(draft, id)),
            "root",
            0,
            false
          )}
        </tbody>
      </table>
    </GridBox>
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
    const rowCustomization = props.rowCustomization?.(childId, child.data);

    const renderColumn = (
      column: number,
      value: TreeGridValue,
      title: string
    ) => {
      const columnCustomization = rowCustomization?.find(
        (c) => c.column === column
      );

      const textSx = {
        width: "100%",
        textAlign: "initial",
        ...(props.wrapBodyText
          ? { overflowWrap: "break-word", wordBreak: "normal" }
          : {
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
              overflow: "hidden",
            }),
      };

      return (
        <>
          {column === 0 &&
            (!!child.children?.length ||
              (props.loadChildren && !child.children)) && (
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
          {columnCustomization?.prefixElements}
          {columnCustomization?.onClick ? (
            <Link
              component="button"
              variant="body2"
              color="inherit"
              underline="hover"
              title={title}
              onClick={columnCustomization.onClick}
              sx={textSx}
            >
              {value}
            </Link>
          ) : (
            columnCustomization?.content ?? (
              <Typography variant="body2" title={title} sx={textSx}>
                {value}
              </Typography>
            )
          )}
        </>
      );
    };

    results.push(
      <tr
        key={id + "-" + childId}
        style={{
          ...(collapse && { visibility: "collapse" }),
        }}
      >
        {props.columns.map((col, i) => {
          let value = child.data[col.key];
          const isNull = value === null;
          if (isNull) {
            value = "NULL";
          } else if (value === undefined) {
            value = "";
          }

          let title = "";
          // Stringify values other than Elements.
          if (value instanceof Date) {
            value = value.toDateString();
            title = value;
          } else if (!(value instanceof Object)) {
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
              <Stack
                direction="row"
                alignItems="center"
                sx={{
                  ...(isNull && {
                    color: (theme) => theme.palette.text.disabled,
                  }),
                  ...(i === 0 && {
                    // TODO(tjennison): The removal of checkboxes revealed that
                    // the inline-block style on the <thead> that's use to keep
                    // the header in place while scrolling causes a small amount
                    // of padding to appear around it that isn't present on the
                    // <tbody>. Investigate other options for dealing with this.
                    paddingLeft: `${indent + 0.2}em`,
                  }),
                }}
              >
                {renderColumn(i, value, title)}
              </Stack>
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
