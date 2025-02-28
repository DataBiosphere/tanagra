import Loading from "components/loading";
import React from "react";
import { Outlet } from "react-router-dom";
import { useAuth, isAuthEnabled } from "auth/provider";
import { LoginPage } from "./loginPage";

export function CheckAuthorization() {
  const { loaded, expired, profile } = useAuth();

  if (!isAuthEnabled()) {
    return <Outlet />;
  }

  if (!loaded) {
    return <Loading status={{ isLoading: true }} />;
  }
  if (expired) {
    console.info("Login expired");
    return <LoginPage />;
  }

  if (!profile) {
    console.info("Login not found");
    return <LoginPage />;
  }

  return <Outlet />;
}
