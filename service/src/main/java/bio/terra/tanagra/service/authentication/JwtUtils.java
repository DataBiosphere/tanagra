package bio.terra.tanagra.service.authentication;

import bio.terra.tanagra.utils.FileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/** Verify JWT (id token) in incoming request. */
public final class JwtUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class);
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static PublicKey publicKey;

  private JwtUtils() {}

  public static UserId verifyJwtAndGetUserid(String jwt, String issuer, String audience) {
    return verifyJwtAndGetUserid(jwt, issuer, audience, /* publicKey= */ null);
  }

  /**
   * Verify a JWT and get UserId from it
   *
   * @param jwt JWT
   * @param audience JWT audience
   * @param issuer JWT issuer
   * @param publicKey JWT Public key file name (under `resources/keys` folder)
   * @return UserId
   */
  public static UserId verifyJwtAndGetUserid(
      String jwt, String issuer, String audience, PublicKey publicKey) {
    Assert.isTrue(StringUtils.isNotBlank(jwt), "jwt empty");
    Assert.isTrue(StringUtils.isNotBlank(issuer), "issuer empty");

    TokenVerifier tokenVerifier =
        TokenVerifier.newBuilder()
            .setAudience(audience)
            .setIssuer(issuer)
            .setPublicKey(publicKey)
            .build();

    try {
      JsonWebToken jsonWebToken = tokenVerifier.verify(jwt);
      JsonWebToken.Payload payload = jsonWebToken.getPayload();
      return makeUserId(payload.getSubject(), (String) payload.get("email"), jwt);

    } catch (TokenVerifier.VerificationException tve) {
      LOGGER.info("JWT expected audience: {}", audience);
      throw new InvalidCredentialsException("JWT verification failed", tve);
    }
  }

  /**
   * Get UserId from JWT (no verification)
   *
   * @param jwt JWT
   * @return UserId
   */
  public static UserId getUserIdFromJwt(String jwt) {
    try {
      String payloadJSON = new String(DECODER.decode(jwt.split("\\.")[1]));
      Map<String, Object> payloadMap = MAPPER.readValue(payloadJSON, new TypeReference<>() {});
      return makeUserId(
          (String) payloadMap.getOrDefault("sub", null), (String) payloadMap.getOrDefault("email", null), jwt);

    } catch (IllegalArgumentException | JsonProcessingException | IndexOutOfBoundsException e) {
      throw new InvalidCredentialsException("Error decoding user info from JWT access token", e);
    }
  }

  private static UserId makeUserId(String sub, String email, String token) {
    if (StringUtils.isEmpty(sub) || StringUtils.isEmpty(email)) {
      throw new InvalidCredentialsException(
          String.format("Error decoding user sub: '%s', email: '%s' in token", sub, email));
    }
    return UserId.fromToken(sub, email, token);
  }

  private static byte[] parsePEMFile(String pemFileName) throws IOException {
    File pemFile = FileUtils.getResourceFile(Path.of("keys/" + pemFileName));
    PemReader reader = new PemReader(new FileReader(pemFile));
    PemObject pemObject = reader.readPemObject();
    byte[] content = pemObject.getContent();
    reader.close();
    return content;
  }

  public static PublicKey getPublicKey(String pemFileName, String algorithm) throws IOException {
    if (JwtUtils.publicKey == null) {
      if (!"RSA".equals(algorithm)) {
        throw new UnsupportedEncodingException("Unsupported algorithm: " + algorithm);
      }

      try {
        EncodedKeySpec keySpec = new X509EncodedKeySpec(parsePEMFile(pemFileName));
        publicKey = KeyFactory.getInstance(algorithm).generatePublic(keySpec);
      } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
        throw new InvalidCredentialsException("Public key read failed: " + algorithm, e);
      }
    }

    return publicKey;
  }
}
