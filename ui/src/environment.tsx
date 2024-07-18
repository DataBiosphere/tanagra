// Items here should be added to environment.js.template as well.
type Environment = {
  REACT_APP_ADDITIONAL_ROUTES: string;
  REACT_APP_BACKEND_HOST: string;
  REACT_APP_BACKEND_FILTERS: string;
  REACT_APP_POST_MESSAGE_ORIGIN: string;

  REACT_APP_USE_EXIT_URL: boolean;
  REACT_APP_EXIT_URL: string;

  REACT_APP_USE_FAKE_API: string;

  REACT_APP_GET_LOCAL_AUTH_TOKEN: string;
};

export function getEnvironment(): Environment {
  return process.env as unknown as Environment;
}
