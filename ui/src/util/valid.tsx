export function isValid<Type>(arg: Type): arg is NonNullable<Type> {
  return arg !== null && arg !== undefined;
}
