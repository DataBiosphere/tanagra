export function isValid<Type>(arg: Type) {
  return arg !== null && typeof arg !== "undefined";
}
