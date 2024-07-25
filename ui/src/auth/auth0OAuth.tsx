import { Auth0Provider, useAuth0 } from "@auth0/auth0-react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import { loginAccessType } from "apiContext";
import { ErrorList } from "components/errorPage";
import { getEnvironment } from "environment";
import verilyImage from "images/verily.png";
import GridLayout from "layout/gridLayout";
import React, { useCallback, useEffect } from "react";
import { Outlet, redirect, useLocation } from "react-router-dom";
import { useNavigate } from "util/searchState";

const imageAltText = "Verily Data Explorer";

export function isAuth0Enabled(): boolean {
  return !!getEnvironment().REACT_APP_AUTH0_DOMAIN;
}

export function Auth0AuthProvider() {
  const env = getEnvironment();
  const navigate = useNavigate();

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

export function Auth0LoginPage() {
  const { error, isAuthenticated, isLoading, loginWithRedirect } = useAuth0();
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) {
      console.info("Already authenticated, return to previous page");
      navigate(location.state?.from.toString() || "/", { replace: true });
    }
  }, [location, navigate]);

  const signIn = useCallback(
    async (from?: string) => {
      await loginWithRedirect({ appState: { returnTo: from } });
    },
    [loginWithRedirect]
  );

  return (
    <Box>
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
            style={{ width: "100px", height: "auto" }}
            alt={imageAltText}
          />
          <ErrorList errors={error} />
          <Button
            variant="contained"
            onClick={() => signIn(location.state?.from)}
            disabled={isLoading}
          >
            Sign in to Data Explorer
          </Button>
        </GridLayout>
      </GridLayout>
    </Box>
  );
}

export function Auth0LogoutPage() {
  const { error, isLoading, logout } = useAuth0();

  const signOut = useCallback(() => {
    logout({ logoutParams: { returnTo: window.location.origin } });
  }, [logout]);

  return (
    <Box>
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
            style={{ width: "100px", height: "auto" }}
            alt={imageAltText}
          />
          <ErrorList errors={error} />
          <Button
            variant="contained"
            onClick={() => signOut()}
            disabled={isLoading}
          >
            Sign out of Data Explorer
          </Button>
        </GridLayout>
      </GridLayout>
    </Box>
  );
}

export function useAuth0Token(
  loginType: loginAccessType
): () => Promise<string> {
  const { isAuthenticated, getAccessTokenSilently } = useAuth0();
  const navigate = useNavigate();

  if (!isAuthenticated) {
    if (loginType == loginAccessType.RedirectUrl) {
      redirect("/login");
    } else if (loginType == loginAccessType.NavigatePath) {
      navigate("login", { replace: true });
    }
  }

  return getAccessTokenSilently;
}
