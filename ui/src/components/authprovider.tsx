import { createContext, ReactElement, ReactNode, useContext } from "react";
import { AuthProvider as Auth0AuthProvider } from "./auth0OAuth";

/** Profile is like BasicProfile except with properties instead of getter methods. */
export type Profile = {
  readonly sub: string;
  readonly email: string;
  readonly name: string;
  readonly givenName: string;
  readonly familyName: string;
  readonly imageUrl: string;
};

export type AuthContextType = {
  /** Set to true when the 'SignIn with Google' library is initialized. */
  loaded: boolean;
  /** Set to true when the user's access token is expired. */
  expired: boolean;
  /** When logged in, contains the user's Google profile information. */
  profile?: Profile;
  /** When set, contains an error that occured during login. */
  error?: Error;
  /** Invokes the sign in process. */
  signIn: (from?: string) => void;
  /** Invokes the sign out process. */
  signOut: () => void;
  /** Provides an id token for use in API calls. */
  getAuthToken: () => Promise<string>;
};

export const AuthContext = createContext<AuthContextType>(
  {} as AuthContextType
);

export function useAuth(): AuthContextType {
  return useContext(AuthContext);
}

export interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider(props: AuthProviderProps): ReactElement {
  return <Auth0AuthProvider {...props} />;
}
