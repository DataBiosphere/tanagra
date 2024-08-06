// All environment variables used in UI must be declared here
type Environment = {
  REACT_APP_ADDITIONAL_ROUTES?: string;
  REACT_APP_BACKEND_HOST?: string;
  REACT_APP_CLIENT_ID?: string;
  REACT_APP_POST_MESSAGE_ORIGIN?: string;

  REACT_APP_USE_EXIT_URL?: boolean;
  REACT_APP_EXIT_URL?: string;

  REACT_APP_USE_FAKE_API?: string;
  REACT_APP_GET_LOCAL_AUTH_TOKEN?: string;

  REACT_APP_AUTH0_DOMAIN?: string;
  REACT_APP_AUTH0_CLIENT_ID?: string;
};

declare global {
  interface Window {
    environment: Environment;
  }
}

export function getEnvironment(): Environment {
  if (typeof window !== "undefined" && window?.environment) {
    return window.environment;
  }
  return process.env as unknown as Environment;
}

export function isTestEnvironment(): boolean {
  return getEnvironment().REACT_APP_USE_FAKE_API === "y";
}
