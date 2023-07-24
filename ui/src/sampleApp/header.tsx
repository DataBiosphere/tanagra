import Box from "@mui/material/Box";
import Link from "@mui/material/Link";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { Link as RouterLink } from "react-router-dom";
import { useBaseParams } from "router";
import { StudyName } from "studyName";

export function Header() {
  const params = useBaseParams();

  // TODO(tjennison): Fetch the study name.
  return (
    <GridLayout
      cols
      spacing={1}
      rowAlign="middle"
      sx={{
        px: 2,
        backgroundColor: (theme) => theme.palette.primary.light,
        borderBottomColor: (theme) => theme.palette.divider,
        borderBottomStyle: "solid",
        borderBottomWidth: "1px",
      }}
    >
      <GridBox sx={{ height: (theme) => theme.spacing(4), my: 1 }}>
        <Link component={RouterLink} to="/">
          <Box component="img" src="verily.png" sx={{ height: "100%" }} />
        </Link>
      </GridBox>
      <GridLayout rows>
        {!!params.underlayName ? (
          <Link
            variant="body1"
            color="inherit"
            underline="hover"
            component={RouterLink}
            to={`/underlays/${params.underlayName}`}
          >
            Underlay: {params.underlayName}
          </Link>
        ) : null}
        {!!params.studyId ? (
          <Link
            variant="body1"
            color="inherit"
            underline="hover"
            component={RouterLink}
            to={`/underlays/${params.underlayName}/studies/${params.studyId}`}
          >
            <StudyName />
          </Link>
        ) : null}
      </GridLayout>
    </GridLayout>
  );
}
