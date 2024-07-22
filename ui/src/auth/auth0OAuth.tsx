import { Auth0Provider, useAuth0 } from "@auth0/auth0-react";
import Button from "@mui/material/Button";
import List from "@mui/material/List";
import { useTitlePrefix } from "components/title";
import { getEnvironment } from "environment";
import { ErrorList } from "errorhandler";
import GridLayout from "layout/gridLayout";
import { useCallback, useEffect } from "react";
import { useNavigate } from "react-router";
import { Outlet, Path, useLocation } from "react-router-dom";
import { Header } from "sampleApp/header";

// TODO(dexamundsen): Wrap in auth interface

export function isAuth0Enabled(): boolean {
  return !!getEnvironment().REACT_APP_AUTH0_DOMAIN;
}

export function isAuthenticated(): boolean {
  const { isAuthenticated } = useAuth0();
  return isAuthenticated;
}

export function Auth0ProviderWithOutlet() {
  const env = getEnvironment();
  const navigate = useNavigate();

  if (!isAuth0Enabled) {
    return null;
  }

  return (
    <Auth0Provider
      domain={env.REACT_APP_AUTH0_DOMAIN}
      clientId={env.REACT_APP_AUTH0_CLIENT_ID}
      cacheLocation="localstorage"
      onRedirectCallback={(appState) =>
        navigate(appState?.returnTo || "/", { replace: true })
      }
      authorizationParams={{
        scope: "profile email",
        redirect_uri: window.location.origin,
      }}
    >
      <Outlet />
    </Auth0Provider>
  );
}

export function LoginPage() {
  useTitlePrefix("Sign In");
  const { error, isAuthenticated, isLoading, loginWithRedirect } = useAuth0();
  const location = useLocation();
  const navigate = useNavigate();

  // Already authenticated, return
  useEffect(() => {
    if (isAuthenticated) {
      const from: Path | undefined = location.state?.from;
      navigate(from || "/", { replace: true });
    }
  });

  const signIn = useCallback(
    async (from?: string) => {
      await loginWithRedirect({ appState: { returnTo: from || "/" } });
    },
    [loginWithRedirect]
  );

  return (
    <GridLayout rows>
      <Header />
      <List>
        <ErrorList errors={error} />
        <Button
          variant="contained"
          disabled={isLoading}
          onClick={() => signIn(location.state?.from)}
        >
          Sign in to Data Explorer
        </Button>
      </List>
    </GridLayout>
  );
}

export const LogoutPage = () => {
  const { error, logout } = useAuth0();

  const signOut = useCallback(() => {
    logout({ logoutParams: { returnTo: window.location.origin } });
  }, [logout]);

  return (
    <GridLayout rows>
      <Header />
      <List>
        <ErrorList errors={error} />
        <Button variant="contained" onClick={signOut}>
          Sign out from Data Explorer
        </Button>
      </List>
    </GridLayout>
  );
};

export function getAuth0Token(): () => Promise<string> {
  const { isAuthenticated, getAccessTokenSilently } = useAuth0();
  const navigate = useNavigate();

  if (!isAuthenticated) {
    console.info("Not logged in");
  }
  // Not authenticated, redirect to login
  useEffect(() => {
    if (!isAuthenticated) {
      navigate("login" || "/", { replace: true });
    }
  });

  return useCallback(async () => {
    try {
      return await getAccessTokenSilently();
    } catch (e: unknown) {
      console.info("Error getting access token", e);
      throw e;
    }
  }, [getAccessTokenSilently]);
}
