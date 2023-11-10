export function safeRegExp(query: string): [RegExp, string | undefined] {
  let regExp: RegExp | undefined;
  let error: string | undefined;

  try {
    regExp = new RegExp(query, "i");
  } catch (e) {
    if (e instanceof Error) {
      error = e.message;
    } else {
      error = "Unexpected error type.";
    }
    regExp = new RegExp(query.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "i");
  }
  return [regExp, error];
}
