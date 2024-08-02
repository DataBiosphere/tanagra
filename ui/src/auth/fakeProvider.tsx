import {Outlet} from "react-router-dom";
import {AuthContext, Profile} from "auth/provider";

const FakeProfile = {
  sub: "fakesub",
  email: "fakeemail"
};

export interface FakeAuthContextType {
  loaded?: boolean;
  expired?: boolean;
  profile?: Profile;
  noProfile?: boolean;
  error?: Error;
  signIn?: () => void;
  signOut?: () => void;
  getAuthToken?: () => Promise<string>;
  children: JSX.Element;
}

export function FakeAuthContext({
                                  loaded = true,
                                  expired = false,
                                  noProfile,
                                  profile = noProfile ? undefined :  FakeProfile,
                                  error,
                                  signIn = () => {},
                                  signOut = () => {},
                                  getAuthToken = () => Promise.resolve("fakeauthtoken"),
                                  children,
                                }: FakeAuthContextType) {
  return (
      <AuthContext.Provider
          value={{
            loaded: loaded,
            expired: expired,
            profile: profile,
            error: error,
            getAuthToken: getAuthToken,
            signIn: signIn,
            signOut: signOut,
          }}
      >
        {children}
      </AuthContext.Provider>
  );
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
        <Outlet />     </AuthContext.Provider>
  );
}
