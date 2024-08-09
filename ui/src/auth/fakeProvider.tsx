import {
  AuthContext,
  AuthContextType,
  AuthProviderProps,
  Profile,
} from "auth/provider";
import { Outlet } from "react-router-dom";

export const FakeProfile = {
  sub: "fakesub",
  email: "fakeemail",
} as Profile;

export function makeFakeAuth({
  loaded,
  expired,
  profile,
  error,
  signIn,
  signOut,
  getAuthToken,
}: {
  loaded?: boolean;
  expired?: boolean;
  profile?: Profile;
  error?: Error;
  signIn?: () => void;
  signOut?: () => void;
  getAuthToken?: () => Promise<string>;
}) {
  return {
    loaded: loaded ?? true,
    expired: expired ?? false,
    profile: profile,
    error: error,
    signIn:
      signIn ??
      (() => {
        console.info("fake sign in");
      }),
    signOut:
      signOut ??
      (() => {
        console.info("fake sign in");
      }),
    getAuthToken: getAuthToken ?? (() => Promise.resolve("fake-auth-token")),
  } as AuthContextType;
}

export function FakeAuthProvider(authProps?: AuthProviderProps) {
  return (
    <AuthContext.Provider value={authProps?.authCtx ?? makeFakeAuth({})}>
      <Outlet />
    </AuthContext.Provider>
  );
}
