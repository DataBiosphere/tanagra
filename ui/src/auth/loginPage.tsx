import Button from "@mui/material/Button";
import { ErrorList } from "components/errorPage";
import verilyImage from "images/verily.png";
import GridLayout from "layout/gridLayout";
import React, { useEffect } from "react";
import { useNavigate } from "util/searchState";
import { useAuth } from "auth/provider";

// TODO(BENCH-4320): Remove imageTitle after Image -> 'Verily Data Explorer'
const imageAlt = "Data Explorer";

export const logInText = "Continue to login";
export const logOutText = "Continue to logout";

export const LoginPage = () => {
  const { loaded, profile, signIn } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (profile) {
      navigate("/", { replace: true });
    }
  }, [navigate, profile]);

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
          style={{ width: "350px", height: "auto" }}
          alt={imageAlt}
        />
        <h2>{imageAlt}</h2>
        <Button
          size="medium"
          variant="contained"
          onClick={() => signIn()}
          disabled={!loaded}
        >
          {logInText}
        </Button>
        <p>
          Need a Data Explorer Account?{" "}
          <a href="https://support.workbench.verily.com/docs/contact/">
            Contact us
          </a>
        </p>
      </GridLayout>
    </GridLayout>
  );
};

export const LogoutPage = () => {
  const { loaded, error, signOut } = useAuth();
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
          style={{ width: "350px", height: "auto" }}
          alt={imageAlt}
        />
        <h2>{imageAlt}</h2>
        <ErrorList errors={error} />
        <Button
          size="medium"
          variant="contained"
          onClick={() => signOut()}
          disabled={!loaded}
        >
          {logOutText}
        </Button>
      </GridLayout>
    </GridLayout>
  );
};
