import { Alert, AlertProps, Button } from "@mui/material";
import {
  isRouteErrorResponse,
  useNavigate,
  useRouteError,
} from "react-router-dom";

export interface ErrorListProps {
  errors?: Error | Error[];
  alertProps?: AlertProps;
}

export function ErrorList({ errors = [], alertProps }: ErrorListProps) {
  const errorList = (Array.isArray(errors) ? errors : [errors]).filter(
    (error): error is Error => !!error
  );

  return errorList.length ? (
    <div>
      {errorList.map((error, index) => (
        <Alert
          key={index}
          severity="error"
          {...alertProps}
          sx={{ mb: 2, ...alertProps?.sx }}
        >
          {"We ran into an unknown issue with your request. Contact support."}
        </Alert>
      ))}
    </div>
  ) : (
    <></>
  );
}

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
