import AccountTreeIcon from "@mui/icons-material/AccountTree";
import CheckBoxIcon from "@mui/icons-material/CheckBox";
import IconButton from "@mui/material/IconButton";
import renderer from "react-test-renderer";
import { TreeGrid, TreeGridData, TreeGridId } from "components/treegrid";

test("Table renders correctly", () => {
  const columns = [
    {
      key: "col1",
      width: "100%",
      title: "Column 1",
    },
    {
      key: "col2",
      width: 150,
      title: "Column 2",
    },
    {
      key: "col3",
      width: 50,
      title: <AccountTreeIcon />,
    },
  ];

  const data: TreeGridData = {
    root: {
      data: {},
      children: [1, 3],
    },
    1: {
      data: {
        col1: "1-col1",
        col2: "1-col2",
      },
      children: [2],
    },
    2: {
      data: {
        col1: "2-col1",
        col2: "2-col2",
        col3: "2-col3",
      },
    },
    3: {
      data: {
        col1: "3-col1",
        col2: "3-col2",
        col3: <AccountTreeIcon />,
      },
    },
  };

  const prefixElements = (id: TreeGridId) => {
    // Add variety to the rows.
    if (id !== 2) {
      return undefined;
    }
    return;
    <IconButton size="small">
      <CheckBoxIcon fontSize="inherit" />
    </IconButton>;
  };

  const tree = renderer
    .create(
      <TreeGrid columns={columns} data={data} prefixElements={prefixElements} />
    )
    .toJSON();
  expect(tree).toMatchSnapshot();
});
