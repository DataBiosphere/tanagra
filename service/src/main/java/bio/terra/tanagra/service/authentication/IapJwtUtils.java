package bio.terra.tanagra.service.authentication;

import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Verify IAP authorization JWT token in incoming request. */
public final class IapJwtUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(IapJwtUtils.class);
  private static final String IAP_ISSUER_URL = "https://cloud.google.com/iap";

  private IapJwtUtils() {}

  public static String getJwtFromHeader(HttpServletRequest request) {
    // Check for iap jwt header in incoming request
    String jwt = request.getHeader("x-goog-iap-jwt-assertion");
    if (jwt == null) {
      throw new InvalidCredentialsException("JWT is null");
    }
    return jwt;
  }

  public static UserId verifyJwtForComputeEngineOrGKE(
      String jwt, long projectNumber, long backendServiceId) {
    return verifyJwt(
        jwt,
        String.format(
            "/projects/%s/global/backendServices/%s",
            Long.toUnsignedString(projectNumber), Long.toUnsignedString(backendServiceId)),
        IAP_ISSUER_URL);
  }

  public static UserId verifyJwtForAppEngine(String jwt, long projectNumber, String projectId) {
    return verifyJwt(
        jwt,
        String.format("/projects/%s/apps/%s", Long.toUnsignedString(projectNumber), projectId),
        IAP_ISSUER_URL);
  }

  public static UserId verifyJwt(String jwt, String expectedAudience, String issuer) {
    TokenVerifier tokenVerifier =
        TokenVerifier.newBuilder().setAudience(expectedAudience).setIssuer(issuer).build();
    try {
      JsonWebToken jsonWebToken = tokenVerifier.verify(jwt);
      JsonWebToken.Payload payload = jsonWebToken.getPayload();

      // Verify that the token contain subject and email claims
      if (payload.getSubject() == null || payload.get("email") == null) {
        throw new InvalidCredentialsException(
            "Subject or email not included in JWT payload: "
                + payload.getSubject()
                + ", "
                + payload.get("email"));
      }
      return UserId.fromToken(payload.getSubject(), (String) payload.get("email"), jwt);
    } catch (TokenVerifier.VerificationException tve) {
      LOGGER.info("JWT expected audience: {}", expectedAudience);
      throw new InvalidCredentialsException("JWT verification failed", tve);
    }
  }
}
