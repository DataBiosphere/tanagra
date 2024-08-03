package bio.terra.tanagra.service.authentication;

import jakarta.servlet.http.HttpServletRequest;

/** Verify IAP authorization JWT in incoming request. */
public final class GcpIapUtils {
  private static final String GCP_IAP_JWT_ISSUER_URL = "https://cloud.google.com/iap";

  private GcpIapUtils() {}

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
    return JwtUtils.verifyJwtAndGetUserid(
        jwt,
        GCP_IAP_JWT_ISSUER_URL,
        String.format(
            "/projects/%s/global/backendServices/%s",
            Long.toUnsignedString(projectNumber), Long.toUnsignedString(backendServiceId)));
  }

  public static UserId verifyJwtForAppEngine(String jwt, long projectNumber, String projectId) {
    return JwtUtils.verifyJwtAndGetUserid(
        jwt,
        GCP_IAP_JWT_ISSUER_URL,
        String.format("/projects/%s/apps/%s", Long.toUnsignedString(projectNumber), projectId));
  }
}
