package bio.terra.tanagra.service.authentication;

import bio.terra.tanagra.utils.RetryUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import java.io.IOException;

public final class GcpAccessTokenUtils {
  private GcpAccessTokenUtils() {}

  @SuppressWarnings("deprecation")
  public static UserId getUserIdFromToken(String accessToken)
      throws InterruptedException, IOException {
    GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
    Oauth2 oauth2 =
        new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
            .setApplicationName("tanagra")
            .build();
    Userinfo userInfo =
        RetryUtils.callWithRetries(
            () -> oauth2.userinfo().get().execute(), ex -> ex instanceof IOException);
    return UserId.fromToken(userInfo.getId(), userInfo.getEmail(), accessToken);
  }
}
