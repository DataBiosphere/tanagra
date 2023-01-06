package bio.terra.tanagra.service.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdToken;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;

/** Utilities for working with Google application default credentials. */
public final class AppDefaultUtils {
  private AppDefaultUtils() {}

  public static UserId getUserIdFromAdc(String targetAudience) {
    IdToken idToken =
        getIdTokenFromAdc(
            getApplicationDefaultCredentials(),
            ImmutableList.of("openid", "email", "profile"),
            targetAudience);
    return IapJwtUtils.verifyJwt(
        idToken.getTokenValue(), targetAudience, "https://accounts.google.com");
  }

  /** Get an ID token from the application default credentials. */
  public static IdToken getIdTokenFromAdc(
      GoogleCredentials applicationDefaultCredentials, List<String> scopes, String targetAudience) {
    GoogleCredentials scopedCredentials = applicationDefaultCredentials.createScoped(scopes);
    if (!(scopedCredentials instanceof IdTokenProvider)) {
      throw new InvalidCredentialsException(
          "Passed credential is not an IdTokenProvider, please ensure only scoped ADC are passed.");
    } else if (scopedCredentials instanceof UserCredentials) {
      // Unlike for ServiceAccountCredentials, it doesn't seem possible to set the target audience
      // for UserCredentials below. An ID token is returned but with the gcloud client id as the
      // audience, not the target one passed in here.
      throw new InvalidCredentialsException(
          "Only service accounts are supported when using application default credentials. Please set the GOOGLE_APPLICATION_CREDENTIALS environment variable to a service account key file path.");
    }

    // Generate an ID token with and audience that matches the one expected by the service and/or
    // proxy in front of it.
    IdTokenCredentials idTokenCredentials =
        IdTokenCredentials.newBuilder()
            .setIdTokenProvider((IdTokenProvider) scopedCredentials)
            .setTargetAudience(targetAudience)
            .setOptions(List.of(IdTokenProvider.Option.FORMAT_FULL))
            .build();

    // Call refresh() to obtain the token.
    try {
      idTokenCredentials.refresh();
    } catch (IOException ioEx) {
      throw new InvalidCredentialsException("Error refreshing ID token", ioEx);
    }
    return idTokenCredentials.getIdToken();
  }

  /** Get the application default credentials. Throw an exception if they are not defined. */
  public static GoogleCredentials getApplicationDefaultCredentials() {
    try {
      return GoogleCredentials.getApplicationDefault();
    } catch (IOException ioEx) {
      throw new InvalidCredentialsException(
          "Application default credentials are not defined. Run `gcloud auth application-default login`",
          ioEx);
    }
  }
}
