package bio.terra.tanagra.app.authentication;

import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.tanagra.app.configuration.AuthenticationConfiguration;
import bio.terra.tanagra.service.authentication.BearerTokenUtils;
import bio.terra.tanagra.service.authentication.IapJwtUtils;
import bio.terra.tanagra.service.authentication.InvalidCredentialsException;
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
      LOGGER.info("Authorization not required for OPTIONS methods requests");
      return true;
    }

    if (!(handler instanceof HandlerMethod)) {
      LOGGER.error(
          "Unexpected handler class: {}, {}", request.getRequestURL(), request.getMethod());
      return false;
    }

    HandlerMethod method = (HandlerMethod) handler;
    Operation apiOp = AnnotationUtils.findAnnotation(method.getMethod(), Operation.class);
    if (apiOp != null) {
      for (String tag : apiOp.tags()) {
        if (OPENAPI_TAG_AUTH_NOT_REQUIRED.equals(tag)) {
          LOGGER.info(
              "Authorization not required by endpoint: {}", request.getRequestURL().toString());
          return true;
        }
      }
    }
    LOGGER.info("Authorization required by endpoint: {}", request.getRequestURL().toString());

    UserId userId;
    try {
      if (authenticationConfiguration.isIapGkeJwt()) {
        String jwt = IapJwtUtils.getJwtFromHeader(request);
        userId =
            IapJwtUtils.verifyJwtForComputeEngineOrGKE(
                jwt,
                authenticationConfiguration.getGcpProjectNumber(),
                authenticationConfiguration.getGkeBackendServiceId());
      } else if (authenticationConfiguration.isIapAppEngineJwt()) {
        String jwt = IapJwtUtils.getJwtFromHeader(request);
        userId =
            IapJwtUtils.verifyJwtForAppEngine(
                jwt,
                authenticationConfiguration.getGcpProjectNumber(),
                authenticationConfiguration.getGcpProjectId());
      } else if (authenticationConfiguration.isBearerToken()) {
        BearerToken bearerToken = new BearerTokenFactory().from(request);
        userId = BearerTokenUtils.getUserIdFromToken(bearerToken);
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
    LOGGER.info("User authenticated: subject={}, email={}", userId.getSubject(), userId.getEmail());

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
