package bio.terra.tanagra.service.auth;

import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;
import javax.servlet.http.HttpServletRequest;

/** Verify IAP authorization JWT token in incoming request. */
public final class IapJwtUtils {
  private static final String IAP_ISSUER_URL = "https://cloud.google.com/iap";

  private IapJwtUtils() {}

  public static String getJwtFromHeader(HttpServletRequest request) {
    // Check for iap jwt header in incoming request
    String jwt = request.getHeader("x-goog-iap-jwt-assertion");
    if (jwt == null) {
      throw new InvalidTokenException("JWT is null");
    }
    return jwt;
  }

  public static UserId verifyJwtForComputeEngineOrGKE(
      String jwt, long projectNumber, long backendServiceId) {
    return verifyJwt(
        jwt,
        String.format(
            "/projects/%s/global/backendServices/%s",
            Long.toUnsignedString(projectNumber), Long.toUnsignedString(backendServiceId)));
  }

  public static UserId verifyJwtForAppEngine(String jwt, long projectNumber, String projectId) {
    return verifyJwt(
        jwt,
        String.format("/projects/%s/apps/%s", Long.toUnsignedString(projectNumber), projectId));
  }

  private static UserId verifyJwt(String jwt, String expectedAudience) {
    TokenVerifier tokenVerifier =
        TokenVerifier.newBuilder().setAudience(expectedAudience).setIssuer(IAP_ISSUER_URL).build();
    try {
      JsonWebToken jsonWebToken = tokenVerifier.verify(jwt);
      JsonWebToken.Payload payload = jsonWebToken.getPayload();

      // Verify that the token contain subject and email claims
      if (payload.getSubject() == null || payload.get("email") == null) {
        throw new InvalidTokenException(
            "Subject or email not included in JWT payload: "
                + payload.getSubject()
                + ", "
                + payload.get("email"));
      }
      return new UserId(payload.getSubject(), (String) payload.get("email"));
    } catch (TokenVerifier.VerificationException tve) {
      throw new InvalidTokenException("JWT verification failed", tve);
    }
  }
}
