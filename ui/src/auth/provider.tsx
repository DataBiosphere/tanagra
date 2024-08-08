import Button from "@mui/material/Button";
import { Auth0AuthProvider, isAuth0Enabled } from "auth/auth0OAuth";
import { FakeAuthProvider, useFakeAuth } from "auth/fakeProvider";
import { ErrorList } from "components/errorPage";
import Loading from "components/loading";
import { isTestEnvironment } from "environment";
import verilyImage from "images/verily.png";
import GridLayout from "layout/gridLayout";
import React, { createContext, useContext, useEffect } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { useNavigate } from "util/searchState";

const imageTitle = "Verily WorkBench Data Explorer";

export const signInText = "Sign in";
export const signOutText = "Sign out";

export type Profile = {
  readonly sub: string;
  readonly email: string;
};

export type AuthContextType = {
  /** Set to true when the 'SignIn with Google' library is initialized. */
  loaded: boolean;
  /** Set to true when the user's access token is expired. */
  expired: boolean;
  /** When logged in, contains the user's Google profile information. */
  profile?: Profile;
  /** When set, contains an error that occurred during login. */
  error?: Error;
  /** Invokes the sign in process. */
  signIn: () => void;
  /** Invokes the sign out process. */
  signOut: () => void;
  /** Provides an id token for use in API calls. */
  getAuthToken: () => Promise<string>;
};

export const AuthContext = createContext<AuthContextType>(
  {} as AuthContextType
);

export function useAuth(): AuthContextType {
  return isTestEnvironment() ? useFakeAuth() : useContext(AuthContext);
}
export function isAuthEnabled(): boolean {
  return isAuth0Enabled();
}

export function AuthProvider() {
  return isAuthEnabled() ? (
    isTestEnvironment() ? (
      <FakeAuthProvider />
    ) : (
      <Auth0AuthProvider />
    )
  ) : null;
}

export function CheckAuthorization() {
  const { loaded, expired, profile } = useAuth();

  if (!isAuthEnabled()) {
    return <Outlet />;
  }

  if (!loaded) {
    return <Loading status={{ isLoading: true }} />;
  }
  if (expired) {
    console.info("Login expired");
    return <LoginPage />;
  }

  if (!profile) {
    console.info("Login not found");
    return <LoginPage />;
  }

  return <Outlet />;
}

export const LoginPage = () => {
  const { loaded, profile, error, signIn } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    if (profile) {
      navigate("/", { replace: true });
    }
  }, [location, navigate]);

  return (
    <GridLayout
      cols
      rowAlign="middle"
      colAlign="center"
      sx={{ px: 2, py: 1, minHeight: "200px" }}
    >
      <GridLayout
        colAlign="center"
        spacing={1}
        height="auto"
        sx={{ textAlign: "center" }}
      >
        <img
          src={verilyImage}
          style={{ width: "250px", height: "auto" }}
          alt={imageTitle}
        />
        {imageTitle}
        <ErrorList errors={error} />
        <Button variant="contained" onClick={() => signIn()} disabled={!loaded}>
          {signInText}
        </Button>
      </GridLayout>
    </GridLayout>
  );
};

export const LogoutPage = () => {
  const { loaded, error, signOut } = useAuth();
  return (
    <GridLayout
      cols
      rowAlign="middle"
      colAlign="center"
      sx={{ px: 2, py: 1, minHeight: "200px" }}
    >
      <GridLayout
        colAlign="center"
        spacing={1}
        height="auto"
        sx={{ textAlign: "center" }}
      >
        <img
          src={verilyImage}
          style={{ width: "250px", height: "auto" }}
          alt={imageTitle}
        />
        {imageTitle}
        <ErrorList errors={error} />
        <Button
          variant="contained"
          onClick={() => signOut()}
          disabled={!loaded}
        >
          {signOutText}
        </Button>
      </GridLayout>
    </GridLayout>
  );
};
