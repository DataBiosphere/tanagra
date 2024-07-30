package bio.terra.tanagra.service.authentication;

import bio.terra.tanagra.utils.FileUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.util.Assert;

public class JwtAccessTokenUtils {
  private final JWTVerifier jwtVerifier;
  private final ObjectMapper mapper = new ObjectMapper();
  private final Base64.Decoder decoder = Base64.getUrlDecoder();

  private static byte[] parsePEMFile(String pemFileName) throws IOException {
    File pemFile = FileUtils.getResourceFile(Path.of("keys/" + pemFileName));
    PemReader reader = new PemReader(new FileReader(pemFile));
    PemObject pemObject = reader.readPemObject();
    byte[] content = pemObject.getContent();
    reader.close();
    return content;
  }

  private static PublicKey getPublicKey(byte[] keyBytes, String algorithm) {
    try {
      KeyFactory kf = KeyFactory.getInstance(algorithm);
      EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
      return kf.generatePublic(keySpec);
    } catch (NoSuchAlgorithmException e) {
      throw new InvalidCredentialsException(
          "Public key generation failed, algorithm not be found: " + algorithm, e);
    } catch (InvalidKeySpecException e) {
      throw new InvalidCredentialsException("Public key generation failed, invalid key", e);
    }
  }

  private static PublicKey readPublicKeyFromFile(String publicKeyPemFileName, String algorithm)
      throws IOException {
    byte[] bytes = parsePEMFile(publicKeyPemFileName);
    return getPublicKey(bytes, algorithm);
  }

  public JwtAccessTokenUtils(String issuer, String publicKeyPemFileName, String algorithmName)
      throws IOException {
    Assert.isTrue(StringUtils.isNotBlank(issuer), "JWT issuer empty");
    Algorithm algorithm =
        Algorithm.RSA256(
            (RSAPublicKey) readPublicKeyFromFile(publicKeyPemFileName, algorithmName),
            /* RSAPrivateKey= */ null);
    jwtVerifier = JWT.require(algorithm).withIssuer(issuer).build();
  }

  public UserId getUserIdFromToken(String accessToken) {
    Assert.isTrue(StringUtils.isNotBlank(accessToken), "user accessToken empty");

    try {
      DecodedJWT verifiedJWT = jwtVerifier.verify(accessToken);
      String payloadJSON = new String(decoder.decode(verifiedJWT.getPayload()));
      Map<String, String> payloadMap = mapper.readValue(payloadJSON, new TypeReference<>() {});

      String email = payloadMap.getOrDefault("email", "");
      String sub = payloadMap.getOrDefault("sub", "");

      if (email.isEmpty() || sub.isEmpty()) {
        throw new InvalidCredentialsException(
            String.format(
                "Error decoding user email: '%s', sub: '%s' in access token", email, sub));
      }

      return UserId.fromToken(sub, email, accessToken);
    } catch (JWTVerificationException | IllegalArgumentException | JsonProcessingException e) {
      throw new InvalidCredentialsException("Error decoding user info from JWT access token", e);
    }
  }
}
