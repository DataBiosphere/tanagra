import Box from "@mui/material/Box";
import { Theme, useTheme } from "@mui/material/styles";
import { Children, PropsWithChildren } from "react";
import { isValid } from "util/valid";

type axisDesc = undefined | number | string | ((theme: Theme) => string);

export type GridLayoutProps = {
  rows?: axisDesc;
  cols?: axisDesc;

  fillRow?: number;
  fillCol?: number;
};

function parseDesc(
  theme: Theme,
  ad: axisDesc,
  fillIndex?: number
): [string, number] {
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

export default function GridLayout(props: PropsWithChildren<GridLayoutProps>) {
  const theme = useTheme();

  const [rowTmpl] = parseDesc(theme, props.rows, props.fillRow);
  const [colTmpl, colCount] = parseDesc(theme, props.cols, props.fillCol);

  const children = Children.toArray(props.children);

  return (
    <Box
      sx={{
        width: "100%",
        height: "100%",
        display: "grid",
        gridTemplateRows: rowTmpl,
        gridTemplateColumns: colTmpl,
      }}
    >
      {children.map((child, i) => (
        <Box
          key={i}
          sx={{ gridArea: `${Math.floor(i / colCount)}/${i % colCount}` }}
        >
          {child}
        </Box>
      ))}
    </Box>
  );
}
