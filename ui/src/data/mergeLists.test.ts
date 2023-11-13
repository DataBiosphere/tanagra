import { mergeLists } from "data/mergeLists";
import { SortDirection } from "./configuration";

type List = [string, { id: number; count: number }[]];

test("interspersed items", async () => {
  const list1: List = [
    "list1",
    [
      { id: 1, count: 50 },
      { id: 2, count: 10 },
    ],
  ];

  const list2: List = [
    "list2",
    [
      { id: 3, count: 40 },
      { id: 4, count: 30 },
    ],
  ];

  const merged = mergeLists(
    [list1, list2],
    100,
    SortDirection.Desc,
    (value) => value.count
  );

  expect(merged.map((item) => item.source)).toStrictEqual([
    "list1",
    "list2",
    "list2",
    "list1",
  ]);
  expect(merged.map((item) => item.data.id)).toStrictEqual([1, 3, 4, 2]);
});

test("empty list", async () => {
  const list1: List = [
    "list1",
    [
      { id: 1, count: 50 },
      { id: 2, count: 10 },
    ],
  ];

  const list2: List = ["list2", []];

  const merged = mergeLists(
    [list1, list2],
    100,
    SortDirection.Desc,
    (value) => value.count
  );

  expect(merged.map((item) => item.source)).toStrictEqual(["list1", "list1"]);
  expect(merged.map((item) => item.data.id)).toStrictEqual([1, 2]);
});

test("limit", async () => {
  const list1: List = [
    "list1",
    [
      { id: 2, count: 10 },
      { id: 1, count: 50 },
    ],
  ];

  const list2: List = [
    "list2",
    [
      { id: 4, count: 30 },
      { id: 3, count: 40 },
    ],
  ];

  const merged = mergeLists(
    [list1, list2],
    2,
    SortDirection.Asc,
    (value) => value.count
  );

  expect(merged.map((item) => item.source)).toStrictEqual(["list1", "list2"]);
  expect(merged.map((item) => item.data.id)).toStrictEqual([2, 4]);
});
