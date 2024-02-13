import Box from "@mui/material/Box";
import { SxProps, Theme, useTheme } from "@mui/material/styles";
import {
  Children,
  cloneElement,
  isValidElement,
  PropsWithChildren,
} from "react";
import { spacing } from "util/spacing";
import { isValid } from "util/valid";

type axisDesc = undefined | number | string | ((theme: Theme) => string) | true;

export type GridLayoutProps = {
  rows?: axisDesc;
  cols?: axisDesc;

  fillRow?: number;
  fillCol?: number;

  colAlign?: "stretch" | "left" | "center" | "right";
  rowAlign?: "stretch" | "top" | "middle" | "bottom" | "baseline";

  width?: string | number;
  height?: string | number;

  spacing?: string | number;

  sx?: SxProps<Theme>;
};

function parseDesc(
  theme: Theme,
  ad: axisDesc,
  childCount: number,
  fillIndex?: number
): [string, number] {
  if (ad === true) {
    ad = childCount;
  }

  if (!isValid(ad)) {
    return ["1fr", 1];
  }

  if (typeof ad === "number") {
    const arr: string[] = [];
    const fi = fillIndex ?? ad - 1;
    for (let i = 0; i < ad; i++) {
      arr.push(i === fi ? "1fr" : "auto");
    }

    return [arr.join(" "), ad];
  }

  const tmpl = typeof ad === "string" ? ad : ad(theme);
  return [tmpl, tmpl.split(/\s+/).length];
}

function translateDirection(dir: string): string {
  switch (dir) {
    case "left":
    case "top":
      return "start";
    case "middle":
      return "center";
    case "right":
    case "bottom":
      return "end";
  }

  return dir;
}

export default function GridLayout(props: PropsWithChildren<GridLayoutProps>) {
  const theme = useTheme();
  const children = Children.toArray(props.children);

  const [rowTmpl] = parseDesc(
    theme,
    props.rows,
    children?.length ?? 0,
    props.fillRow
  );
  const [colTmpl, colCount] = parseDesc(
    theme,
    props.cols,
    children?.length ?? 0,
    props.fillCol
  );

  const ra = props.rowAlign ?? "stretch";
  const ca = props.colAlign ?? "stretch";

  return (
    <Box
      sx={[
        {
          width: spacing(theme, props.width ?? "100%"),
          height: spacing(theme, props.height ?? "100%"),
          gridGap: spacing(theme, props.spacing ?? 0),
          display: "grid",
          gridTemplateRows: rowTmpl,
          gridTemplateColumns: colTmpl,
          alignItems: translateDirection(ra),
          justifyItems: translateDirection(ca),
        },
        ...(Array.isArray(props.sx) ? props.sx : [props.sx]),
      ]}
    >
      {children.map((child, i) => {
        const gridArea = `${Math.floor(i / colCount) + 1}/${
          (i % colCount) + 1
        }`;

        // Elements nested inside other components can't always be baseline
        // aligned correctly so in that case embed them directly, which requires
        // adding the gridArea style to them.
        if (ra === "baseline" && isValidElement(child)) {
          return cloneElement(child, { sx: { gridArea, ...child.props.sx } });
        }

        return (
          <Box
            key={i}
            sx={{
              gridArea,
              minWidth: 0,
              minHeight: 0,
              width: ca === "stretch" ? "100%" : undefined,
              height: ra === "stretch" ? "100%" : undefined,
            }}
          >
            {child}
          </Box>
        );
      })}
    </Box>
  );
}
