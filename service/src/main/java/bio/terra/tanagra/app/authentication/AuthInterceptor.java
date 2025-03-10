package bio.terra.tanagra.app.authentication;

import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.tanagra.app.configuration.AuthenticationConfiguration;
import bio.terra.tanagra.service.authentication.GcpAccessTokenUtils;
import bio.terra.tanagra.service.authentication.GcpIapUtils;
import bio.terra.tanagra.service.authentication.InvalidCredentialsException;
import bio.terra.tanagra.service.authentication.JwtUtils;
import bio.terra.tanagra.service.authentication.UserId;
import com.google.api.client.http.HttpMethods;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Service
public class AuthInterceptor implements HandlerInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthInterceptor.class);

  // For OpenAPI endpoints with this tag, we don't check for an authorization token in the request
  // header (e.g. status, version). Depending on how the service is deployed and routes are
  // configured, an authorization token may still be required for the request to make it past the
  // proxy.
  private static final String OPENAPI_TAG_AUTH_NOT_REQUIRED = "Unauthenticated";

  private final AuthenticationConfiguration authenticationConfiguration;

  @Autowired
  public AuthInterceptor(AuthenticationConfiguration authenticationConfiguration) {
    this.authenticationConfiguration = authenticationConfiguration;
  }

  /**
   * Returns true if the request is authenticated and can proceed. Publishes authenticated user info
   * using Spring's SecurityContext.
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // Clear the security context before we start, to make sure we're not using authentication
    // from a previous request.
    SpringAuthentication.clearCurrentUser();

    if (HttpMethods.OPTIONS.equals(request.getMethod())) {
      // Authorization not required for OPTIONS methods requests
      return true;
    }

    if (!(handler instanceof HandlerMethod method)) {
      LOGGER.error(
          "Unexpected handler class: {}, {}", request.getRequestURL(), request.getMethod());
      return false;
    }

    Operation apiOp = AnnotationUtils.findAnnotation(method.getMethod(), Operation.class);
    if (apiOp != null) {
      for (String tag : apiOp.tags()) {
        if (OPENAPI_TAG_AUTH_NOT_REQUIRED.equals(tag)) {
          // Authorization not required by endpoint
          return true;
        }
      }
    }

    UserId userId;
    try {
      if (authenticationConfiguration.isIapGkeJwt()) {
        String jwt = GcpIapUtils.getJwtFromHeader(request);
        userId =
            GcpIapUtils.verifyJwtForComputeEngineOrGKE(
                jwt,
                authenticationConfiguration.getGcpProjectNumber(),
                authenticationConfiguration.getGkeBackendServiceId());

      } else if (authenticationConfiguration.isIapAppEngineJwt()) {
        String jwt = GcpIapUtils.getJwtFromHeader(request);
        userId =
            GcpIapUtils.verifyJwtForAppEngine(
                jwt,
                authenticationConfiguration.getGcpProjectNumber(),
                authenticationConfiguration.getGcpProjectId());

      } else if (authenticationConfiguration.isGcpAccessToken()) {
        BearerToken bearerToken = new BearerTokenFactory().from(request);
        userId = GcpAccessTokenUtils.getUserIdFromToken(bearerToken.getToken());

      } else if (authenticationConfiguration.isJwt()) {
        String idToken = new BearerTokenFactory().from(request).getToken();
        userId =
            (authenticationConfiguration.getJwtIssuer() == null)
                ? JwtUtils.getUserIdFromJwt(idToken)
                : JwtUtils.verifyJwtAndGetUserid(
                    idToken,
                    authenticationConfiguration.getJwtIssuer(),
                    authenticationConfiguration.getJwtAudience(),
                    JwtUtils.getPublicKey(
                        authenticationConfiguration.getJwtPublicKeyFile(),
                        authenticationConfiguration.getJwtAlgorithm()));

      } else if (authenticationConfiguration.isDisableChecks()) {
        LOGGER.warn(
            "Authentication checks are disabled. This should only happen for local development.");
        userId = UserId.forDisabledAuthentication();

      } else {
        throw new InternalServerErrorException("Invalid auth configuration");
      }
    } catch (InvalidCredentialsException ite) {
      LOGGER.error("Authentication failed", ite);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }

    SpringAuthentication.setCurrentUser(userId);

    // Any further checks on the user (e.g. check email domain name) should go here.
    // Return SC_FORBIDDEN, not SC_UNAUTHORIZED, if they fail.

    return true;
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView) {
    SpringAuthentication.clearCurrentUser();
  }
}
