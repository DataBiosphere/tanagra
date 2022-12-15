package bio.terra.tanagra.app;

import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.tanagra.app.configuration.AuthConfiguration;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.auth.BearerTokenUtils;
import bio.terra.tanagra.service.auth.IapJwtUtils;
import bio.terra.tanagra.service.auth.InvalidTokenException;
import bio.terra.tanagra.service.auth.UserAuthentication;
import bio.terra.tanagra.service.auth.UserId;
import com.google.api.client.http.HttpMethods;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Service
public class AuthInterceptor implements HandlerInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthInterceptor.class);

  private final AuthConfiguration authConfiguration;

  @Autowired
  public AuthInterceptor(AuthConfiguration authConfiguration) {
    this.authConfiguration = authConfiguration;
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
    SecurityContextHolder.clearContext();

    if (request.getMethod().equals(HttpMethods.OPTIONS)) {
      LOGGER.info("Authorization not required for OPTIONS methods requests");
      return true;
    }

    if (!(handler instanceof HandlerMethod)) {
      LOGGER.error(
          "Unexpected handler class: {}, {}", request.getRequestURL(), request.getMethod());
      return false;
    }

    HandlerMethod method = (HandlerMethod) handler;
    boolean isAuthRequired = false;
    ApiOperation apiOp = AnnotationUtils.findAnnotation(method.getMethod(), ApiOperation.class);
    if (apiOp != null) {
      Authorization[] authorizations = apiOp.authorizations();
      for (Authorization auth : apiOp.authorizations()) {
        if (!auth.value().isEmpty()) {
          LOGGER.info("Authorization required by endpoint: {}", request.getRequestURL().toString());
          isAuthRequired = true;
          break;
        }
      }
    }
    if (!isAuthRequired) {
      LOGGER.info("Authorization not required by endpoint: {}", request.getRequestURL().toString());
      return true;
    }

    UserAuthentication userAuth;
    try {
      if (authConfiguration.isIapGkeJwt()) {
        String jwt = IapJwtUtils.getJwtFromHeader(request);
        UserId userId =
            IapJwtUtils.verifyJwtForComputeEngineOrGKE(
                jwt,
                authConfiguration.getGcpProjectNumber(),
                authConfiguration.getGkeBackendServiceId());
        userAuth = new UserAuthentication(userId, jwt, UserAuthentication.TokenType.JWT);
      } else if (authConfiguration.isIapAppEngineJwt()) {
        String jwt = IapJwtUtils.getJwtFromHeader(request);
        UserId userId =
            IapJwtUtils.verifyJwtForAppEngine(
                jwt, authConfiguration.getGcpProjectNumber(), authConfiguration.getGcpProjectId());
        userAuth = new UserAuthentication(userId, jwt, UserAuthentication.TokenType.JWT);
      } else if (authConfiguration.isBearerToken()) {
        BearerToken bearerToken = new BearerTokenFactory().from(request);
        UserId userId = BearerTokenUtils.getUserIdFromToken(bearerToken);
        userAuth =
            new UserAuthentication(
                userId, bearerToken.getToken(), UserAuthentication.TokenType.BEARER_TOKEN);
      } else if (authConfiguration.isDisableChecks()) {
        LOGGER.warn("Authentication checks are disabled. This should only happen for local development.");
        userAuth = new UserAuthentication(UserId.forDisabledAuthentication(), null, null);
      } else {
        throw new SystemException("Invalid auth configuration");
      }
    } catch (InvalidTokenException ite) {
      LOGGER.error("Authentication failed", ite);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }

    SecurityContextHolder.getContext().setAuthentication(userAuth);
    LOGGER.info(
        "User authenticated: subject={}, email={}",
        userAuth.getPrincipal().getSubject(),
        userAuth.getPrincipal().getEmail());

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
    // Clear the security context, just to make sure nothing subsequently uses the credentials
    // set up in here.
    SecurityContextHolder.clearContext();
  }
}
