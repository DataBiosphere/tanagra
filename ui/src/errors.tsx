import { getReasonPhrase } from "http-status-codes";
import { useCallback } from "react";
import { useAsync } from "react-async";

export function useAsyncWithApi<T>(promiseFn: () => Promise<T>) {
  const wrapped = useCallback(() => {
    return promiseFn().catch(async (e) => {
      throw await canonicalizeError(e);
    });
  }, [promiseFn]);
  return useAsync<T>({ promiseFn: wrapped, watch: wrapped });
}

async function canonicalizeError(response: unknown): Promise<Error> {
  if (!(response instanceof Response)) {
    return response as Error;
  }
  const text = await response.text();
  try {
    return new Error(JSON.parse(text).message);
  } catch (e) {
    return new Error(getReasonPhrase(response.status) + ": " + text);
  }
}
