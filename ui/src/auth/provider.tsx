import Button from "@mui/material/Button";
import { Auth0AuthProvider, isAuth0Enabled } from "auth/auth0OAuth";
import { FakeAuthProvider } from "auth/fakeProvider";
import { ErrorList } from "components/errorPage";
import Loading from "components/loading";
import { isTestEnvironment } from "environment";
import verilyImage from "images/verily.png";
import GridLayout from "layout/gridLayout";
import React, { createContext, useContext, useEffect } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { useNavigate } from "util/searchState";

// TODO(BENCH-4320): Remove imageTitle after Image -> 'Verily Data Explorer'
const imageAlt = "Data Explorer";

export const logInText = "Continue to login";
export const logOutText = "Continue to logout";

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

export interface AuthProviderProps {
  authCtx: AuthContextType;
}

export const AuthContext = createContext<AuthContextType>(
  {} as AuthContextType
);

export function useAuth(): AuthContextType {
  return useContext(AuthContext);
}
export function isAuthEnabled(): boolean {
  return isAuth0Enabled();
}

export function AuthProvider(authProps: AuthProviderProps) {
  return isAuthEnabled() ? (
    isTestEnvironment() ? (
      <FakeAuthProvider {...authProps} />
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
  const { loaded, profile, signIn } = useAuth();
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
          style={{ width: "350px", height: "auto" }}
          alt={imageAlt}
        />
        <h2>{imageAlt}</h2>
        <Button
          size="medium"
          variant="contained"
          onClick={() => signIn()}
          disabled={!loaded}
        >
          {logInText}
        </Button>
        <p>
          Need a Data Explorer Account?{" "}
          <a href="https://support.workbench.verily.com/docs/contact/">
            Contact us
          </a>
        </p>
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
          style={{ width: "350px", height: "auto" }}
          alt={imageAlt}
        />
        <h2>{imageAlt}</h2>
        <ErrorList errors={error} />
        <Button
          size="medium"
          variant="contained"
          onClick={() => signOut()}
          disabled={!loaded}
        >
          {logOutText}
        </Button>
      </GridLayout>
    </GridLayout>
  );
};
