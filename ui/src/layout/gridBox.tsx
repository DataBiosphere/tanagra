import { styled } from "@mui/system";

export const GridBox = styled("div")({
  width: "100%",
  height: "100%",
});

export const GridBoxPaper = styled("div")(({ theme }) => ({
  width: "100%",
  height: "100%",
  overflow: "hidden",
  borderRadius: theme.spacing(2),
  backgroundColor: theme.palette.background.paper,
}));
