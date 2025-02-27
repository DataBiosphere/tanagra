import { isTestEnvironment } from "environment";
import React from "react";
import { Auth0AuthProvider } from "./auth0OAuth";
import { FakeAuthProvider } from "./fakeAuthProvider";
import { AuthProviderProps, isAuthEnabled } from "./provider";

export function AuthProvider(authProps: AuthProviderProps) {
  return isAuthEnabled() ? (
    isTestEnvironment() ? (
      <FakeAuthProvider {...authProps} />
    ) : (
      <Auth0AuthProvider />
    )
  ) : null;
}
