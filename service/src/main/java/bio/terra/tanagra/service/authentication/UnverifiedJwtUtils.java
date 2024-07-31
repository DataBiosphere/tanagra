package bio.terra.tanagra.service.authentication;

import bio.terra.tanagra.utils.FileUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

public class UnverifiedJwtUtils {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Base64.Decoder decoder = Base64.getUrlDecoder();
  private static JWTVerifier jwtVerifier;

  /**
   * Get a UserId object from an unverified JWT. Verifies the token if the tokenVerifier is provided
   *
   * @param idToken Unverified JWT
   * @param issuer Token issuer, used to verily the token
   * @param publicKeyPemFileName Public key file name (under `resources/keys` folder), used to
   *     verily the token
   * @param algorithmName Algorithm, used to verily the token
   * @return UserId
   */
  public static UserId getUserIdFromToken(
      String issuer, String publicKeyPemFileName, String algorithmName, String idToken)
      throws IOException {
    Assert.isTrue(StringUtils.isNotBlank(idToken), "user idToken empty");

    // only verify the token if issuer, publicKeyPemFileName and algorithm are present {
    if ((issuer != null) && (publicKeyPemFileName != null) && (algorithmName != null)) {
      JWTVerifier tokenVerifier = getJwtVerifier(issuer, publicKeyPemFileName, algorithmName);
      tokenVerifier.verify(idToken);
    }

    try {
      String payloadJSON = new String(decoder.decode(idToken.split("\\.")[1]));
      Map<String, String> payloadMap = mapper.readValue(payloadJSON, new TypeReference<>() {});

      String email = payloadMap.getOrDefault("email", "");
      String sub = payloadMap.getOrDefault("sub", "");

      if (email.isEmpty() || sub.isEmpty()) {
        throw new InvalidCredentialsException(
            String.format(
                "Error decoding user email: '%s', sub: '%s' in access token", email, sub));
      }

      return UserId.fromToken(sub, email, idToken);
    } catch (JWTVerificationException
        | IllegalArgumentException
        | JsonProcessingException
        | IndexOutOfBoundsException e) {
      throw new InvalidCredentialsException("Error decoding user info from JWT access token", e);
    }
  }

  private static JWTVerifier getJwtVerifier(
      String issuer, String publicKeyPemFileName, String algorithmName) throws IOException {
    if (jwtVerifier == null) {

      if (!"RSA".equals(algorithmName)) {
        throw new UnsupportedEncodingException("Unsupported algorithm: " + algorithmName);
      }

      byte[] bytes = parsePEMFile(publicKeyPemFileName);
      RSAPublicKey rsaPublicKey = (RSAPublicKey) getPublicKey(bytes, algorithmName);
      Algorithm algorithm = Algorithm.RSA256(rsaPublicKey, /* RSAPrivateKey= */ null);
      jwtVerifier = JWT.require(algorithm).withIssuer(issuer).build();
    }
    return jwtVerifier;
  }

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
}
