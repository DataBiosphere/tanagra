import { AuthContext, AuthContextType } from "auth/provider";
import { Outlet } from "react-router-dom";

const FakeProfile = {
  sub: "fakesub",
  email: "fakeemail",
};

export function useFakeAuth(
  loaded = true,
  expired = true,
  profile = undefined,
  error = undefined,
  signIn = () => {
    return;
  },
  signOut = () => {
    return;
  },
  getAuthToken = () => Promise.resolve("fakeauthtoken")
) {
  return {
    loaded: loaded,
    expired: expired,
    profile: profile,
    error: error,
    signIn: signIn,
    signOut: signOut,
    getAuthToken: getAuthToken,
  } as AuthContextType;
}

export function FakeAuthProvider() {
  const auth = {
    loaded: true,
    expired: false,
    FakeProfile,
    signIn: () => console.info("fake sign in"),
    signOut: () => console.info("fake sign out"),
    getAuthToken: () => Promise.resolve("fakeauthtoken"),
  };
  return (
    <AuthContext.Provider value={auth}>
      <Outlet />
    </AuthContext.Provider>
  );
}
