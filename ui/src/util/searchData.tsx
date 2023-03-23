import produce from "immer";
import { useCallback, useMemo } from "react";
import { useSearchParams } from "react-router-dom";

export function useSearchData<SearchData>(): [
  SearchData,
  (update: (data: SearchData) => void) => void
] {
  const [searchParams, setSearchParams] = useSearchParams();

  const searchData = useMemo(() => {
    const param = searchParams.get("search");
    return JSON.parse(!!param ? atob(param) : "{}");
  }, [searchParams]);

  const updateSearchData = useCallback(
    (update: (data: SearchData) => void) => {
      setSearchParams(searchParamsFromData(produce(searchData, update)));
    },
    [searchData, setSearchParams]
  );

  return [searchData, updateSearchData];
}

export function searchParamsFromData<SearchData>(data: SearchData) {
  return new URLSearchParams({ search: btoa(JSON.stringify(data ?? {})) });
}
