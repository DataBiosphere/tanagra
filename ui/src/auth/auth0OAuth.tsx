import { Auth0Provider, useAuth0 } from "@auth0/auth0-react";
import { AuthContext, CheckAuthorization } from "auth/provider";
import { getEnvironment } from "environment";
import React, { useCallback, useEffect, useMemo, useRef } from "react";
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

  const signIn = useCallback(
    async (from?: string) => {
      await loginWithRedirect({ appState: { returnTo: from } });
    },
    [loginWithRedirect]
  );

  const signOut = useCallback(() => {
    logout({ logoutParams: { returnTo: window.location.origin } });
  }, [logout]);

  // This is needed for user state to get updated.
  useEffect(() => {
    console.trace();
    getAccessTokenSilently();
  }, [getAccessTokenSilently]);

  const isAuthenticatedRef = useRef(isAuthenticated);
  useEffect(() => {
    isAuthenticatedRef.current = isAuthenticated;
  }, [isAuthenticated]);
  const getAuth0Token = useCallback(async () => {
    try {
      return await getAccessTokenSilently();
    } catch (e: unknown) {
      console.info("Error getting access token", e, isAuthenticatedRef);
      throw e;
    }
  }, [getAccessTokenSilently]);

  if (user && !user.email) {
    throw new Error("user profile has no email address");
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
      signIn: signIn,
      signOut: signOut,
      getAuthToken: getAuth0Token,
    }),
    [isLoading, isAuthenticated, user, error, getAuth0Token, signIn, signOut]
  );

  return (
    <AuthContext.Provider value={auth}>
      <Outlet />
    </AuthContext.Provider>
  );
}
