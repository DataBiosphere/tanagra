import { Outlet } from "react-router-dom";
import { makeFakeAuth } from "./fakeAuth";
import { AuthProviderProps, AuthContext } from "auth/provider";

export function FakeAuthProvider(authProps?: AuthProviderProps) {
  return (
    <AuthContext.Provider value={authProps?.authCtx ?? makeFakeAuth({})}>
      <Outlet />
    </AuthContext.Provider>
  );
}
