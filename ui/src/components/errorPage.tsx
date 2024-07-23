import { Button } from "@mui/material";
import {
  isRouteErrorResponse,
  useNavigate,
  useRouteError,
} from "react-router-dom";

// TODO(tjennison): Make a prettier error page.
export function ErrorPage() {
  const error = useRouteError();
  const navigate = useNavigate();

  let message = String(error);
  if (isRouteErrorResponse(error)) {
    message = `${error.status}: ${error.statusText}`;
  } else if (typeof error === "object" && !!error && "message" in error) {
    message = (error as Error).message;
  }

  return (
    <>
      <p>{message}</p>
      <Button
        variant="contained"
        onClick={() => {
          navigate("/");
        }}
      >
        Return Home
      </Button>
    </>
  );
}
