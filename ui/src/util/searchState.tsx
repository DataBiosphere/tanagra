import produce from "immer";
import { forwardRef, Ref, useCallback, useMemo } from "react";
import {
  Link as BaseLink,
  useNavigate as useRouterNavigate,
  useSearchParams,
} from "react-router-dom";

export type GlobalSearchState = {
  // addCriteria
  addCriteriaTags?: string[];

  // featureSet
  showSelectedColumnsOnly?: boolean;
};

const STORAGE_ID = "t_globalSearchState";

type SearchData = {
  global?: GlobalSearchState;
  local?: unknown;
};

export function useGlobalSearchState(): [
  GlobalSearchState,
  (update: (state: GlobalSearchState) => void) => void
] {
  const [searchData, updateSearchData] = useSearchData();
  const storedData = useMemo(
    () => JSON.parse(window.localStorage.getItem(STORAGE_ID) ?? "{}"),
    []
  );

  return [
    searchData.global ?? storedData ?? {},
    (update: (state: GlobalSearchState) => void) => {
      updateSearchData((searchData: SearchData) => {
        const global = searchData.global ?? {};
        update(global);
        searchData.global = global;

        window.localStorage.setItem(STORAGE_ID, JSON.stringify(global ?? {}));
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

export const RouterLink = forwardRef(function RouterLink(
  props: RouterLinkProps,
  ref: Ref<HTMLAnchorElement>
) {
  const searchStr = useGlobalSearchDataString();
  return (
    <BaseLink
      {...props}
      ref={ref}
      to={{
        pathname: props.to,
        search: searchStr,
      }}
    />
  );
});

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
