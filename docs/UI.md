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

#### Integration tests
To run the UI integration tests, first start the service and UI, then open Cypress.
```
[terminal 1] ./service/local-dev/run_server -a
[terminal 2] npm start
[terminal 3] npx cypress open
```
In the Cypress window, click on "Electron Testing" and then you can run the tests one at a time.
