import { Auth0Provider, useAuth0 } from "@auth0/auth0-react";
import { AuthContext } from "auth/provider";
import { getEnvironment } from "environment";
import React, { useEffect, useMemo } from "react";
import { Outlet } from "react-router-dom";
import { useNavigate } from "util/searchState";

export function isAuth0Enabled(): boolean {
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
        scope: "profile email",
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
    getIdTokenClaims,
    loginWithRedirect,
    logout,
  } = useAuth0();

  // Needed for user state to get updated: calls auth0 if token is expired
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
        await getAccessTokenSilently();
        const claims = await getIdTokenClaims();
        return claims?.__raw || "";
      },
    }),
    [
      isLoading,
      isAuthenticated,
      user,
      error,
      getAccessTokenSilently,
      getIdTokenClaims,
      loginWithRedirect,
      logout,
    ]
  );

  return (
    <AuthContext.Provider value={auth}>
      <Outlet />
    </AuthContext.Provider>
  );
}
