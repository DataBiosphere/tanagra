package bio.terra.tanagra.service;

import bio.terra.common.logging.RequestIdFilter;
import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.app.configuration.VumcAdminConfiguration;
import javax.ws.rs.client.Client;

import bio.terra.tanagra.service.auth.AppDefaultUtils;
import bio.terra.tanagra.service.auth.UserId;
import bio.terra.tanagra.vumc.admin.api.AuthorizationApi;
import bio.terra.tanagra.vumc.admin.api.TestApi;
import bio.terra.tanagra.vumc.admin.api.UnauthenticatedApi;
import bio.terra.tanagra.vumc.admin.client.ApiClient;
import bio.terra.tanagra.vumc.admin.client.ApiException;
import bio.terra.tanagra.vumc.admin.model.CoreServiceTest;
import bio.terra.tanagra.vumc.admin.model.ResourceIdList;
import bio.terra.tanagra.vumc.admin.model.SystemVersion;
import org.apache.http.HttpStatus;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VumcAdminService {
    private final VumcAdminConfiguration vumcAdminConfiguration;
    private final Client commonHttpClient;

    @Autowired
    public VumcAdminService(VumcAdminConfiguration vumcAdminConfiguration) {
        this.vumcAdminConfiguration = vumcAdminConfiguration;
        this.commonHttpClient = new ApiClient().getHttpClient();
    }

    private ApiClient getApiClient(String accessToken) {
        ApiClient client =
                new ApiClient()
                        .setBasePath(vumcAdminConfiguration.getBasePath())
                        .setHttpClient(commonHttpClient)
                        .addDefaultHeader(
                                RequestIdFilter.REQUEST_ID_HEADER, MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
        client.setAccessToken(accessToken);
        return client;
    }

    /** Return an ApiClient with no token set. */
    private ApiClient getApiClientUnauthenticated() {
        return getApiClient("");
    }

    /**
     * Return an ApiClient with a token from the currently authenticated user or the application
     * default credentials, depending on the configuration flag.
     */
    private ApiClient getApiClientAuthenticated() {
        UserId userId =
                vumcAdminConfiguration.isUseAdc()
                        ? AppDefaultUtils.getUserIdFromAdc(vumcAdminConfiguration.getOauthClientId())
                        : SpringAuthentication.getCurrentUser();
        return getApiClient(userId.getToken());
    }

    public SystemVersion version() throws ApiException {
        return new UnauthenticatedApi(getApiClientUnauthenticated()).serviceVersion();
    }

    public CoreServiceTest roundtripTest() throws ApiException {
        TestApi testApi = new TestApi(getApiClientAuthenticated());
        return testApi.coreServiceTest();
    }

    public boolean isAuthorized(String action, String resourceType, String resourceId, String userEmail) throws ApiException {
        AuthorizationApi authorizationApi = new AuthorizationApi(getApiClientAuthenticated());
        try {
            authorizationApi.isAuthorized(action, resourceType, resourceId, userEmail);
            return true;
        } catch (ApiException apiEx) {
            if (apiEx.getCode() == HttpStatus.SC_UNAUTHORIZED) {
                return false;
            }
            throw apiEx;
        }
    }

    public ResourceIdList listAuthorizedResources(String resourceType, String userEmail) throws ApiException {
        AuthorizationApi authorizationApi = new AuthorizationApi(getApiClientAuthenticated());
        return authorizationApi.listAuthorizedResources(resourceType, userEmail);
    }
}
