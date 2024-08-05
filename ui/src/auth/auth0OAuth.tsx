import { Auth0Provider, useAuth0 } from "@auth0/auth0-react";
import { AuthContext } from "auth/provider";
import { getEnvironment } from "environment";
import React, { useEffect, useMemo } from "react";
import { Outlet } from "react-router-dom";
import { useNavigate } from "util/searchState";

export function isAuth0Enabled(): boolean {
  console.log("Auth Domain is " + getEnvironment().REACT_APP_AUTH0_DOMAIN);
  return !!getEnvironment().REACT_APP_AUTH0_DOMAIN;
}

export function Auth0AuthProvider() {
  const env = getEnvironment();
  const navigate = useNavigate();

  return (
    <Auth0Provider
      domain={env.REACT_APP_AUTH0_DOMAIN ?? ""}
      clientId={env.REACT_APP_AUTH0_CLIENT_ID ?? ""}
      cacheLocation="localstorage"
      onRedirectCallback={(appState) =>
        navigate(appState?.returnTo || "/", { replace: true })
      }
      authorizationParams={{
        scope: "openid profile email",
        redirect_uri: window.location.origin,
      }}
    >
      <Auth0ProviderWithClient />
    </Auth0Provider>
  );
}

function Auth0ProviderWithClient() {
  const {
    error,
    user,
    isLoading,
    isAuthenticated,
    getAccessTokenSilently,
    loginWithRedirect,
    logout,
  } = useAuth0();

  // This is needed for user state to get updated.
  useEffect(() => {
    if (isAuthenticated) {
      getAccessTokenSilently();
    }
  }, [isAuthenticated, getAccessTokenSilently]);

  if (user && !user.email) {
    throw new Error("User profile has no email address");
  }

  const auth = useMemo(
    () => ({
      loaded: !isLoading,
      expired: !isAuthenticated,
      profile: user && {
        sub: user.sub || "",
        email: user.email || "",
      },
      error: error,
      signIn: async () => {
        await loginWithRedirect();
      },
      signOut: () => {
        logout({ logoutParams: { returnTo: window.location.origin } });
      },
      getAuthToken: async () => {
        return await getAccessTokenSilently();
      },
    }),
    [isLoading, isAuthenticated, user, error, loginWithRedirect, logout, getAccessTokenSilently]
  );

  return (
    <AuthContext.Provider value={auth}>
      <Outlet />
    </AuthContext.Provider>
  );
}
