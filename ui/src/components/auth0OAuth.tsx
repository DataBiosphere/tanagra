import { Auth0Provider, useAuth0 } from "@auth0/auth0-react";
import { useFlagsmith } from "flagsmith/react";
import {
  ReactElement,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useNavigate } from "react-router";
import { getEnvironment } from "../../environment";
import { AuthContext, AuthProviderProps } from "./provider";

const domain = getEnvironment().REACT_APP_AUTH0_DOMAIN;
const clientId = getEnvironment().REACT_APP_AUTH0_CLIENT_ID;

export function AuthProvider(props: AuthProviderProps): ReactElement {
  const navigate = useNavigate();
  return (
    <Auth0Provider
      domain={domain}
      clientId={clientId}
      cacheLocation="localstorage"
      onRedirectCallback={(appState) =>
        navigate(appState?.returnTo || "/", { replace: true })
      }
      authorizationParams={{
        scope: "profile email",
        redirect_uri: window.location.origin,
      }}
    >
      <ProviderWithClient {...props} />
    </Auth0Provider>
  );
}

function ProviderWithClient(props: AuthProviderProps) {
  const {
    // Auth state:
    error,
    user,
    isLoading,
    isAuthenticated,
    // Auth methods:
    getAccessTokenSilently,
    loginWithRedirect,
    logout,
  } = useAuth0();

  const [neverLoggedIn, setNeverLoggedIn] = useState(true);
  const flagsmith = useFlagsmith();
  const signOut = useCallback(() => {
    logout({ logoutParams: { returnTo: window.location.origin } });
    setNeverLoggedIn(true);
    flagsmith.logout();
  }, [logout, setNeverLoggedIn, flagsmith]);

  useEffect(() => {
    // This is needed for user state to get updated.
    getAccessTokenSilently();
  }, [getAccessTokenSilently]);

  // Provide flagsmith with the logged in user's 'email_address' trait when it becomes available
  useEffect(() => {
    if (user && user.sub && user.email) {
      flagsmith.identify(user.sub, { email_address: user.email });
    }
  }, [flagsmith, user]);

  const signIn = useCallback(
    async (from?: string) => {
      await loginWithRedirect({ appState: { returnTo: from } });
      setNeverLoggedIn(false);
    },
    [loginWithRedirect]
  );

  const isAuthenticatedRef = useRef(isAuthenticated);
  useEffect(() => {
    isAuthenticatedRef.current = isAuthenticated;
  }, [isAuthenticated]);
  const getAuthToken = useCallback(async () => {
    try {
      return await getAccessTokenSilently();
    } catch (e: unknown) {
      console.info("Error getting access token", e, isAuthenticatedRef);
      throw e;
    }
  }, [getAccessTokenSilently]);

  if (user && !user.email) throw new Error("user profile has no email address");
  const auth = useMemo(
    () => ({
      loaded: !isLoading,
      expired: !neverLoggedIn && !isAuthenticated,
      profile: user && {
        sub: user.sub || "",
        email: user.email || "",
        name: user.name || "",
        givenName: user.given_name || "",
        familyName: user.family_name || "",
        imageUrl: user.picture || "",
      },
      error: error,
      getAuthToken,
      signIn: signIn,
      signOut: signOut,
    }),
    [
      isLoading,
      neverLoggedIn,
      isAuthenticated,
      user,
      error,
      getAuthToken,
      signIn,
      signOut,
    ]
  );
  return (
    <AuthContext.Provider value={auth}>{props.children}</AuthContext.Provider>
  );
}
