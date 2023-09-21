import produce from "immer";
import { useCallback, useMemo } from "react";
import {
  Link as BaseLink,
  useNavigate as useRouterNavigate,
  useSearchParams,
} from "react-router-dom";

export type GlobalSearchState = {
  // addCriteria
  addCriteriaTags?: string[];
};

type SearchData = {
  global?: GlobalSearchState;
  local?: unknown;
};

export function useGlobalSearchState(): [
  GlobalSearchState,
  (update: (state: GlobalSearchState) => void) => void
] {
  const [searchData, updateSearchData] = useSearchData();

  return [
    searchData.global ?? {},
    (update: (state: GlobalSearchState) => void) => {
      updateSearchData((searchData: SearchData) => {
        const global = searchData.global ?? {};
        update(global);
        searchData.global = global;
      });
    },
  ];
}

export function useLocalSearchState<T>(): [
  T,
  (update: (data: T) => void) => void
] {
  const [searchData, updateSearchData] = useSearchData();

  return [
    (searchData.local ?? {}) as T,
    (update: (state: T) => void) => {
      updateSearchData((searchData: SearchData) => {
        const local = searchData.local ?? {};
        update(local as T);
        searchData.local = local;
      });
    },
  ];
}

type NavigateOptions = {
  replace?: boolean;
};

export function useNavigate() {
  const searchStr = useGlobalSearchDataString();
  const navigate = useRouterNavigate();

  return (path: string, options?: NavigateOptions) => {
    navigate({ pathname: path, search: searchStr }, options);
  };
}

type RouterLinkProps = { to: string } & { [key: string]: unknown };

export function RouterLink(props: RouterLinkProps) {
  const searchStr = useGlobalSearchDataString();
  return (
    <BaseLink
      {...props}
      to={{
        pathname: props.to,
        search: searchStr,
      }}
    />
  );
}

function useSearchData(): [
  SearchData,
  (update: (data: SearchData) => void) => void
] {
  const [searchParams, setSearchParams] = useSearchParams();

  const searchData = useMemo(() => {
    const param = searchParams.get("q");
    return JSON.parse(!!param ? atob(param) : "{}") as SearchData;
  }, [searchParams]);

  const updateSearchData = useCallback(
    (update: (data: SearchData) => void) => {
      setSearchParams(searchParamsFromData(produce(searchData, update)));
    },
    [searchData, setSearchParams]
  );

  return [searchData, updateSearchData];
}

function useGlobalSearchDataString() {
  const [searchData] = useSearchData();
  searchData.local = undefined;
  return searchParamsFromData(searchData).toString();
}

function searchParamsFromData(data: SearchData) {
  return new URLSearchParams({ q: btoa(JSON.stringify(data ?? {})) });
}
