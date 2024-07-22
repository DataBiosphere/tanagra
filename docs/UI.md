### Developing

1. Works with Node 16 (the current LTS). First, generate API code:

   ```sh
   npm run codegen
   ```

2. Install deps:

   ```sh
   npm install
   ```

3. Start development server:

   ```sh
   npm start
   ```

This runs the app in the development mode.\
Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

The page will reload if you make edits.\
You will also see any lint errors in the console.

By default, the app assumes the Tanagra service is running at
[http://localhost:8080](http://localhost:8080).

### Auth0 setup

Set the below environment variables in the .env files to enable Auth0 OAuth2 integration
```sh
  REACT_APP_AUTH0_DOMAIN: string; # required
  REACT_APP_AUTH0_CLIENT_ID: string; # required
```
The auth0 flow will be enabled only if the domain is set, and overrides any other authentication mechanism that might be otherwise enabled.
User's auth token will be sent in the request header for all calls to the backend server.

#### Integration tests
To run the UI integration tests, first start the service and UI, then open Cypress.
```
[terminal 1] ./service/local-dev/run_server -a
[terminal 2] npm start
[terminal 3] npx cypress open
```
In the Cypress window, click on "Electron Testing" and then you can run the tests one at a time.
