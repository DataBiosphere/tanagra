Before starting the app, generate the API code by running `npm run codegen`.

Run the app in the development mode using `npm start`.\
Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

The page will reload if you make edits.\
You will also see any lint errors in the console.

By default, the app assumes the Tanagra service is running at
[http://localhost:8080](http://localhost:8080).

`npm run fake` runs the app using hardcoded test data which doesn't require the
backend service to be running. The UI integration tests assume they're running
against this test data.
