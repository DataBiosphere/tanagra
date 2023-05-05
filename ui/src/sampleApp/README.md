# Tanagra Sample App

This is a small sample app that demonstrates how Tanagra can be embedded within
a larger app using an iframe. It uses `postMessage` to communicate with the
containing app, such as to indicate when the user has navigated out of Tanagra
and the iframe should be closed.

To disable the sample app, set `REACT_APP_ADDITIONAL_ROUTES=none` in a `.env.
file.
