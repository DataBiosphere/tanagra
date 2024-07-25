import { loginAccessType } from "apiContext";
import {
  Auth0AuthProvider,
  Auth0LoginPage,
  Auth0LogoutPage,
  isAuth0Enabled,
  useAuth0Token,
} from "auth/auth0OAuth";

export function isAuthEnabled(): boolean {
  return isAuth0Enabled();
}

export function AuthProvider() {
  if (isAuth0Enabled()) {
    // TODO:dexamundsen add mocks for testing
    return <Auth0AuthProvider />;
  }

  return null;
}

export function LoginPage() {
  return isAuth0Enabled() ? Auth0LoginPage() : null;
}

export function LogoutPage() {
  return isAuth0Enabled() ? Auth0LogoutPage() : null;
}

export function useAuthToken(
  loginType: loginAccessType
): string | (() => Promise<string>) {
  return isAuth0Enabled() ? useAuth0Token(loginType) : "";
}
