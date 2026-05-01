package dev.ceven.petapp.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import dev.ceven.petapp.config.SecurityConfig;
import dev.ceven.petapp.dto.PetDto;
import dev.ceven.petapp.service.PetService;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetController.class)
@Import({ SecurityConfig.class, JwtSignatureVerificationTest.JwtTestConfig.class })
class JwtSignatureVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetService petService;

    private static KeyPair validKeyPair;
    private static KeyPair invalidKeyPair;

    @BeforeAll
    static void setupKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        validKeyPair = keyPairGenerator.generateKeyPair();
        invalidKeyPair = keyPairGenerator.generateKeyPair();
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        public JwtDecoder jwtDecoder() {
            // The decoder only knows the valid public key
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) validKeyPair.getPublic()).build();
        }

        @Bean
        public JwtEncoder jwtEncoder() {
            JWK jwk = new RSAKey.Builder((RSAPublicKey) validKeyPair.getPublic())
                    .privateKey((RSAPrivateKey) validKeyPair.getPrivate())
                    .build();
            JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
            return new NimbusJwtEncoder(jwks);
        }
    }

    private String generateToken(KeyPair keyPair) {
        JWK jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        JwtEncoder encoder = new NimbusJwtEncoder(jwks);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:8080/realms/petapp-realm")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("testuser")
                .claim("realm_access", Map.of("roles", List.of("user")))
                .build();

        JwsHeader header = JwsHeader.with(() -> "RS256").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    void whenValidSignature_thenReturns200() throws Exception {
        Mockito.when(petService.getAllPets()).thenReturn(List.of(new PetDto("1", "Buddy", "Dog")));

        String validToken = generateToken(validKeyPair);

        mockMvc.perform(get("/api/pets")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void whenInvalidSignature_thenReturns401() throws Exception {
        // Generate a token signed with a key that the decoder doesn't know about
        String invalidToken = generateToken(invalidKeyPair);

        mockMvc.perform(get("/api/pets")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized()); // 401
    }

    @Test
    void whenNoneSignatureAlgorithm_thenReturns401() throws Exception {
        // Base64UrlEncoded header: {"alg":"none","typ":"JWT"} -> eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0
        // Base64UrlEncoded payload: {"sub":"testuser","iss":"http://localhost:8080/realms/petapp-realm","realm_access":{"roles":["user"]}} -> eyJzdWIiOiJ0ZXN0dXNlciIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9yZWFsbXMvcGV0YXBwLXJlYWxtIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbInVzZXIiXX19
        // A 'none' algorithm token has no signature (ends with a dot)
        String noneToken = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJ0ZXN0dXNlciIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9yZWFsbXMvcGV0YXBwLXJlYWxtIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbInVzZXIiXX19.";

        mockMvc.perform(get("/api/pets")
                .header("Authorization", "Bearer " + noneToken))
                .andExpect(status().isUnauthorized()); // 401
    }
}
