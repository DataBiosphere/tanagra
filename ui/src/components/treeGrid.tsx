import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import ErrorIcon from "@mui/icons-material/Error";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import KeyboardArrowRightIcon from "@mui/icons-material/KeyboardArrowRight";
import SwapVertIcon from "@mui/icons-material/SwapVert";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import { SxProps, Theme, useTheme } from "@mui/material/styles";
import TableCell from "@mui/material/TableCell";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { produce } from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import {
  ChangeEvent,
  isValidElement,
  MutableRefObject,
  ReactNode,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { useImmer } from "use-immer";
import { standardDateString } from "util/date";
import { spacing } from "util/spacing";
import { TreeGridSortDirection } from "./treeGridHelpers";
import { isValid } from "util/valid";

export type TreeGridId = string | number | bigint;
export type TreeGridValue =
  | undefined
  | null
  | string
  | number
  | bigint
  | boolean
  | object
  | JSX.Element
  | Date;

export type TreeGridRowData = {
  [key: string]: TreeGridValue;
};

export type TreeGridItem<RowType extends TreeGridRowData = TreeGridRowData> = {
  children?: TreeGridId[];
  data: RowType;
};

export type TreeGridData<ItemType extends TreeGridItem = TreeGridItem> = {
  rows: Map<TreeGridId, ItemType>;
  children: TreeGridId[];
};

export type TreeGridColumn = {
  key: string;
  width: string | number;
  title?: string | JSX.Element;
  sortable?: boolean;
  filterable?: boolean;
  suffixElements?: ReactNode;
};

export type ColumnCustomization = {
  column: number;
  prefixElements?: ReactNode;
  onClick?: () => void;
  content?: ReactNode;
  backgroundSx?: SxProps<Theme>;
};

export type TreeGridSortOrder = {
  column: string;
  direction: TreeGridSortDirection;
};

export type TreeGridFilters = {
  [col: string]: string;
};

export type TreeGridProps<ItemType extends TreeGridItem = TreeGridItem> = {
  columns: TreeGridColumn[];
  data: TreeGridData<ItemType>;
  defaultExpanded?: TreeGridId[];
  highlightId?: TreeGridId;
  rowCustomization?: (
    id: TreeGridId,
    item: ItemType
  ) => ColumnCustomization[] | undefined;
  loadChildren?: (id: TreeGridId) => Promise<void>;
  minWidth?: boolean;
  wrapBodyText?: boolean;
  rowHeight?: number | string;
  padding?: number | string;
  expandable?: boolean;
  reserveExpansionSpacing?: boolean;

  sortOrders?: TreeGridSortOrder[];
  onSort?: (orders: TreeGridSortOrder[]) => void;
  sortLevels?: number;

  filters?: TreeGridFilters;
  onFilter?: (filters: TreeGridFilters) => void;
};

export function TreeGrid<ItemType extends TreeGridItem = TreeGridItem>(
  props: TreeGridProps<ItemType>
) {
  const theme = useTheme();

  const [state, updateState] = useImmer<TreeGridState>(
    new Map<TreeGridId, TreeGridItemState>()
  );

  const [pendingFilters, setPendingFilters] = useState<TreeGridFilters>({});
  const filterTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  const cancel = useRef<boolean>(false);
  useEffect(
    () => () => {
      cancel.current = true;
    },
    []
  );

  const scrolledToHighlightId = useRef<TreeGridId>();
  const highlightRef = useRef<HTMLTableRowElement>(null);

  const toggleExpanded = useCallback(
    (draft: TreeGridState, id: TreeGridId) => {
      const loadChildren = props.loadChildren;

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
          if (!loadChildren || !!itemState.loaded) {
            itemState.status = Status.Expanded;
          } else {
            itemState.status = Status.Loading;
            loadChildren(id)
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
    },
    [props.loadChildren, updateState]
  );

  // Ensure default expansions take effect before rendering.
  useLayoutEffect(() => {
    const de = props?.defaultExpanded;
    if (de && de.length > 0) {
      updateState((draft) => {
        draft.clear();
        de.forEach((id) => toggleExpanded(draft, id));
      });
    }
    // Removed toggleExpanded from the deps array to prevent the hierarchy from reloading on each criteria selection/deletion
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [props.defaultExpanded, updateState]);

  useEffect(() => {
    if (
      scrolledToHighlightId.current !== props.highlightId &&
      highlightRef.current
    ) {
      // Check if all items are loaded before scrolling to highlighted item
      const itemsLoaded: boolean[] = [];
      state.forEach((item) => itemsLoaded.push(!!item.loaded));
      if (itemsLoaded.every(Boolean) && highlightRef.current) {
        highlightRef.current.scrollIntoView({
          block: "center",
          behavior: "smooth",
        });
        scrolledToHighlightId.current = props.highlightId;
      }
    }
  }, [props.highlightId, state]);

  const onSort = useCallback(
    (col: TreeGridColumn, orders?: TreeGridSortOrder[]) => {
      const onSort = props.onSort;
      const sortLevels = props.sortLevels;
      if (!orders) {
        return;
      }

      const index = orders.findIndex((o) => o.column === col.key);
      const dir = nextDirection(orders[index]?.direction);

      const newOrders = produce(orders, (o) => {
        if (index >= 0) {
          o.splice(index, 1);
        }
        if (dir) {
          o.unshift({
            column: col.key,
            direction: dir,
          });
          o.splice(sortLevels ?? 2);
        }
      });

      onSort?.(newOrders);
    },
    [props.onSort, props.sortLevels]
  );

  const onFilter = useCallback(
    (filters: TreeGridFilters) => {
      const onFilter = props.onFilter;
      setPendingFilters({});
      onFilter?.(filters);
    },
    [props.onFilter]
  );

  const onChangeFilter = useCallback(
    (col: string, value: string, filters: TreeGridFilters) => {
      if (filterTimeout.current) {
        clearTimeout(filterTimeout.current);
      }

      const newFilters = produce(filters, (filters) => {
        filters[col] = value;
      });
      setPendingFilters(newFilters);

      filterTimeout.current = setTimeout(() => {
        onFilter(newFilters);
      }, 500);
    },
    [onFilter]
  );

  const paperColor = theme.palette.background.paper;

  return (
    <GridBox sx={{ overflowY: "auto", px: spacing(theme, props.padding ?? 5) }}>
      <table
        style={{
          tableLayout: "fixed",
          ...(props.minWidth ? { minWidth: "100%" } : { width: "100%" }),
          textAlign: "left",
          borderCollapse: "collapse",
        }}
      >
        <colgroup>
          {props.columns.flatMap((col, i) => [
            <col
              key={i}
              style={{
                width: col.width === "100%" ? "200px" : col.width,
              }}
            />,
            ...(col.width === "100%" ? [<col key="resize" />] : []),
          ])}
        </colgroup>
        <thead>
          <tr
            style={{
              height: spacing(theme, props.rowHeight ?? 6),
            }}
          >
            {props.columns.map((col, i) => (
              <th
                key={i}
                colSpan={col.width === "100%" ? 2 : undefined}
                style={{
                  position: "sticky",
                  top: 0,
                  backgroundColor: paperColor,
                  boxShadow: `inset 0 -2px 0 ${theme.palette.divider}`,
                  zIndex: 1,
                }}
              >
                <GridLayout
                  rows
                  spacing={0.5}
                  sx={{
                    py: 1,
                  }}
                >
                  <GridLayout cols rowAlign="middle" sx={{ px: 1 }}>
                    <GridBox
                      sx={{
                        pr:
                          col.sortable || col.suffixElements ? 0.5 : undefined,
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
                    </GridBox>
                    {col.sortable ? (
                      <GridBox
                        sx={{ mr: col.suffixElements ? 0.5 : undefined }}
                      >
                        <SortIconButton
                          col={col}
                          orders={props.sortOrders}
                          onClick={() => onSort(col, props.sortOrders)}
                        />
                      </GridBox>
                    ) : null}
                    {col.suffixElements}
                    <GridBox />
                  </GridLayout>
                  {col.filterable ? (
                    <GridBox sx={{ px: 1, pb: 1 }}>
                      <TextField
                        fullWidth
                        variant="outlined"
                        value={
                          pendingFilters[col.key] ?? props.filters?.[col.key]
                        }
                        onChange={(event: ChangeEvent<HTMLInputElement>) => {
                          onChangeFilter(
                            col.key,
                            event.target.value,
                            pendingFilters
                          );
                        }}
                        sx={{
                          "& .MuiOutlinedInput-input": {
                            py: "2px",
                          },
                        }}
                      />
                    </GridBox>
                  ) : null}
                </GridLayout>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {renderChildren(
            theme,
            props,
            state,
            (id: TreeGridId) =>
              updateState((draft) => toggleExpanded(draft, id)),
            0,
            false, // collapse
            true, // first
            highlightRef,
            props.highlightId
          )}
        </tbody>
      </table>
    </GridBox>
  );
}

function renderChildren<ItemType extends TreeGridItem = TreeGridItem>(
  theme: Theme,
  props: TreeGridProps<ItemType>,
  state: TreeGridState,
  toggleExpanded: (id: TreeGridId) => void,
  indent: number,
  collapse: boolean,
  first: boolean,
  highlightRef: MutableRefObject<HTMLTableRowElement | null>,
  highlightId?: TreeGridId,
  id?: TreeGridId,
  key?: string
): JSX.Element[] {
  const results: JSX.Element[] = [];

  const children = isValid(id)
    ? props.data.rows.get(id)?.children
    : props.data.children;
  children?.forEach((childId) => {
    const child = props.data.rows.get(childId);
    if (!child) {
      return;
    }

    const childState = state.get(childId);

    if (!child.data) {
      throw new Error(`'data' is undefined for ${JSON.stringify(child)}`);
    }
    const rowCustomization = props.rowCustomization?.(childId, child);

    const renderColumn = (
      column: number,
      value: TreeGridValue,
      title: string,
      columnCustomization?: ColumnCustomization
    ) => {
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

      const expandable =
        (props.expandable && !!child.children?.length) ||
        (props.loadChildren && !child.children);
      let content: ReactNode = null;
      if (columnCustomization?.content) {
        content = columnCustomization?.content;
      } else {
        let displayableValue: ReactNode;
        if (isValidElement(value)) {
          displayableValue = value;
        } else if (value instanceof Date) {
          displayableValue = value.toLocaleString();
        } else if (typeof value == "object") {
          displayableValue = JSON.stringify(value);
        } else if (typeof value === "bigint") {
          displayableValue = String(value);
        } else {
          displayableValue = value;
        }

        if (columnCustomization?.onClick) {
          content = (
            <Link
              component="button"
              variant={childId === highlightId ? "body2em" : "body2"}
              color="inherit"
              underline="hover"
              title={title}
              onClick={columnCustomization.onClick}
              sx={textSx}
            >
              {displayableValue}
            </Link>
          );
        } else if (expandable && column === 0) {
          content = (
            <Link
              component="button"
              variant={childId === highlightId ? "body2em" : "body2"}
              color="inherit"
              underline="none"
              title={title}
              sx={textSx}
              onClick={() => {
                toggleExpanded(childId);
              }}
            >
              {displayableValue}
            </Link>
          );
        } else if (typeof value === "object" && !(value instanceof Date)) {
          content = displayableValue;
        } else {
          content = (
            <Typography
              variant={childId === highlightId ? "body2em" : "body2"}
              title={title}
              sx={textSx}
            >
              {displayableValue}
            </Typography>
          );
        }
      }

      return (
        <>
          {props.expandable && column === 0 ? (
            <IconButton
              size="small"
              title={childState?.errorMessage}
              onClick={() => {
                toggleExpanded(childId);
              }}
              sx={{
                visibility: !expandable ? "hidden" : undefined,
                display:
                  !expandable && !props.reserveExpansionSpacing
                    ? "none"
                    : undefined,
              }}
            >
              <ItemIcon state={childState} />
            </IconButton>
          ) : undefined}
          {columnCustomization?.prefixElements}
          {content}
        </>
      );
    };

    const childKey = `${key ?? "root"}~${childId}`;
    results.push(
      <tr
        key={childKey}
        ref={
          !highlightRef.current && highlightId === childId
            ? highlightRef
            : undefined
        }
        style={{
          ...(collapse ? { display: "none" } : undefined),
          height: spacing(theme, props.rowHeight ?? 6),
        }}
      >
        {props.columns.map((col, i) => {
          if (!child.data) {
            throw new Error(`'data' is undefined for ${JSON.stringify(child)}`);
          }
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
            value = standardDateString(value);
            title = value;
          } else if (!(value instanceof Object)) {
            value = String(value);
            title = value;
          }

          const columnCustomization = rowCustomization?.find(
            (c) => c.column === i
          );

          const sx: SxProps<Theme> | undefined =
            columnCustomization?.backgroundSx;
          return (
            <TableCell
              key={i}
              colSpan={col.width === "100%" ? 2 : undefined}
              sx={[
                {
                  boxShadow: !first
                    ? `inset 0 1px 0 ${theme.palette.divider}`
                    : undefined,
                },
                ...(Array.isArray(sx) ? sx : [sx]),
              ]}
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
                {renderColumn(i, value, title, columnCustomization)}
              </Stack>
            </TableCell>
          );
        })}
      </tr>
    );

    first = false;

    results.push(
      ...renderChildren(
        theme,
        props,
        state,
        toggleExpanded,
        indent + 1,
        collapse || childState?.status !== Status.Expanded,
        false,
        highlightRef,
        highlightId,
        childId,
        childKey
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

type SortIconButtonProps = {
  col: TreeGridColumn;
  orders?: TreeGridSortOrder[];
  onClick?: () => void;
};

function SortIconButton(props: SortIconButtonProps) {
  const theme = useTheme();

  if (!props.col.sortable) {
    return null;
  }

  const index =
    props.orders?.findIndex((o) => o.column === props.col.key) ?? -1;
  const order = props.orders?.[index];

  let sx: SxProps = {};
  if (index === 0) {
    sx = {
      "&.MuiIconButton-root": {
        backgroundColor: theme.palette.primary.main,
        color: theme.palette.primary.contrastText,
      },
      "&.MuiIconButton-root:hover": {
        backgroundColor: theme.palette.primary.dark,
      },
    };
  } else if (index > 0) {
    sx = {
      "&.MuiIconButton-root": {
        outline: `1px solid ${theme.palette.primary.main}`,
        outlineOffset: "-1px",
      },
    };
  }

  return (
    <IconButton size="small" onClick={() => props.onClick?.()} sx={{ ...sx }}>
      {directionIcon(order?.direction)}
    </IconButton>
  );
}

function directionIcon(dir?: TreeGridSortDirection) {
  switch (dir) {
    case TreeGridSortDirection.Asc:
      return <ArrowUpwardIcon fontSize="inherit" />;
    case TreeGridSortDirection.Desc:
      return <ArrowDownwardIcon fontSize="inherit" />;
  }
  return <SwapVertIcon fontSize="inherit" />;
}

function nextDirection(dir?: TreeGridSortDirection) {
  switch (dir) {
    case TreeGridSortDirection.Asc:
      return TreeGridSortDirection.Desc;
    case TreeGridSortDirection.Desc:
      return undefined;
  }
  return TreeGridSortDirection.Asc;
}
