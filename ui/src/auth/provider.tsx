import { getEnvironment } from "environment";
import { createContext, useContext } from "react";

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
  getAuthToken?: () => Promise<string>;
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
export function isAuth0Enabled(): boolean {
  return !!getEnvironment().REACT_APP_AUTH0_DOMAIN;
}

export function hasExpired(expAt: number | undefined): boolean {
  // Consider the token expired within 60 seconds of expiry.
  return expAt ? expAt - 60 * 1000 - Date.now() <= 0 : false;
}
