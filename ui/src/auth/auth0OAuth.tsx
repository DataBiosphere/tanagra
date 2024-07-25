import { Auth0Provider, useAuth0 } from "@auth0/auth0-react";
import Button from "@mui/material/Button";
import { LoginAccessType } from "apiContext";
import { ErrorList } from "components/errorPage";
import { getEnvironment } from "environment";
import verilyImage from "images/verily.png";
import GridLayout from "layout/gridLayout";
import React, { useEffect } from "react";
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

export const Auth0LoginPage = () => {
  const { error, isAuthenticated, isLoading, loginWithRedirect } = useAuth0();
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) {
      console.info("Already authenticated, return to previous page");
      navigate(location.state?.from.toString() || "/", { replace: true });
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
          style={{ width: "100px", height: "auto" }}
          alt={imageAltText}
        />
        <ErrorList errors={error} />
        <Button
          variant="contained"
          onClick={() =>
            loginWithRedirect({ appState: { returnTo: location.state?.from } })
          }
          disabled={isLoading}
        >
          Sign in to Data Explorer
        </Button>
      </GridLayout>
    </GridLayout>
  );
};

export const Auth0LogoutPage = () => {
  const { error, isLoading, logout } = useAuth0();

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
          style={{ width: "100px", height: "auto" }}
          alt={imageAltText}
        />
        <ErrorList errors={error} />
        <Button
          variant="contained"
          onClick={() =>
            logout({ logoutParams: { returnTo: window.location.origin } })
          }
          disabled={isLoading}
        >
          Sign out of Data Explorer
        </Button>
      </GridLayout>
    </GridLayout>
  );
};

export function useAuth0Token(
  loginType: LoginAccessType
): (() => Promise<string>) | null {
  const { isAuthenticated, getAccessTokenSilently } = useAuth0();
  const navigate = useNavigate();

  if (!isAuth0Enabled()) {
    return null;
  }

  if (!isAuthenticated) {
    if (loginType == LoginAccessType.REDIRECT_URL) {
      redirect("/login");
    } else if (loginType == LoginAccessType.NAVIGATE_PATH) {
      // TODO:dexamundsen needs to be within useEffect
      navigate("login", { replace: true });
    }
  }
  return getAccessTokenSilently;
}
