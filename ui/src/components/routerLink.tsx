import { forwardRef } from "react";
import { LinkProps as BaseLinkProps, Link as BaseLink } from "react-router-dom";
import { useGlobalSearchDataString } from "util/searchState";

export const RouterLink = forwardRef<
  HTMLAnchorElement,
  Omit<BaseLinkProps, "to"> & { to?: string }
>(function RouterLink(props, ref) {
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
