import AccountTreeIcon from "@mui/icons-material/AccountTree";
import CheckBoxIcon from "@mui/icons-material/CheckBox";
import IconButton from "@mui/material/IconButton";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { TreeGrid, TreeGridData, TreeGridId } from "components/treegrid";
import React from "react";

test("Table renders correctly", async () => {
  const columns = [
    {
      key: "col1",
      width: "100%",
      title: "Column 1",
      sortable: true,
    },
    {
      key: "col2",
      width: 150,
      title: "Column 2",
      sortable: true,
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

  const rowCustomization = (id: TreeGridId) => {
    // Add variety to the rows.
    if (id !== 2) {
      return undefined;
    }
    return [
      {
        column: 0,
        prefixElements: (
          <IconButton size="small">
            <CheckBoxIcon fontSize="inherit" />
          </IconButton>
        ),
      },
    ];
  };

  const onSort = jest.fn();
  const { asFragment } = render(
    <TreeGrid
      columns={columns}
      data={data}
      sortOrders={[]}
      onSort={onSort}
      rowCustomization={rowCustomization}
    />
  );
  expect(asFragment()).toMatchSnapshot();

  const sortIcons = await screen.findAllByTestId("SwapVertIcon");
  expect(sortIcons.length).toBe(2);

  userEvent.click(sortIcons[0]);
  expect(asFragment()).toMatchSnapshot();
  expect(onSort).toHaveBeenCalled();
});
