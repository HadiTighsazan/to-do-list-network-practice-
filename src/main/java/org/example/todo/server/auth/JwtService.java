package org.example.todo.server.auth;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;


public class JwtService {
    private static final Gson GSON = new Gson();
    private static final Base64.Encoder B64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DECODER = Base64.getUrlDecoder();

    private final byte[] secret;

    public JwtService(byte[] secret) {
        this.secret = Objects.requireNonNull(secret);
    }

    public String createToken(String userId, String username, long iatMillis, long expMillis) {
        Header hdr = new Header("HS256", "JWT");
        Payload p = new Payload(userId, username, millisToSeconds(iatMillis), millisToSeconds(expMillis), UUID.randomUUID().toString());
        String header64 = B64_URL_ENCODER.encodeToString(GSON.toJson(hdr).getBytes(StandardCharsets.UTF_8));
        String payload64 = B64_URL_ENCODER.encodeToString(GSON.toJson(p).getBytes(StandardCharsets.UTF_8));
        String signingInput = header64 + "." + payload64;
        String sig64 = B64_URL_ENCODER.encodeToString(hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + sig64;
    }

    public Claims parseAndValidate(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT format");
        String header64 = parts[0], payload64 = parts[1], sig64 = parts[2];
        byte[] expectedSig = hmacSha256((header64 + "." + payload64).getBytes(StandardCharsets.UTF_8));
        byte[] providedSig = B64_URL_DECODER.decode(sig64);
        if (!PasswordHasher.constantTimeEquals(expectedSig, providedSig))
            throw new SecurityException("Invalid JWT signature");

        Payload p = GSON.fromJson(new String(B64_URL_DECODER.decode(payload64), StandardCharsets.UTF_8), Payload.class);
        long nowSec = millisToSeconds(Instant.now().toEpochMilli());
        if (p.exp < nowSec) throw new SecurityException("Token expired");
        return new Claims(p.sub, p.username, secondsToMillis(p.iat), secondsToMillis(p.exp), p.jti);
    }

    public String getJti(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT");
        Payload p = GSON.fromJson(new String(B64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8), Payload.class);
        return p.jti;
    }

    private byte[] hmacSha256(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }

    private static long millisToSeconds(long ms) { return ms / 1000L; }
    private static long secondsToMillis(long s) { return s * 1000L; }

    private record Header(@SerializedName("alg") String alg, @SerializedName("typ") String typ) {}

    private static class Payload {
        String sub;         // user id
        String username;
        long iat;           // issued at (sec)
        long exp;           // expiry (sec)
        String jti;         // token id
        Payload(String sub, String username, long iat, long exp, String jti) {
            this.sub = sub; this.username = username; this.iat = iat; this.exp = exp; this.jti = jti;
        }
    }

    public static class Claims {
        public final String userId;
        public final String username;
        public final long issuedAtMillis;
        public final long expiresAtMillis;
        public final String jti;
        public Claims(String userId, String username, long issuedAtMillis, long expiresAtMillis, String jti) {
            this.userId = userId; this.username = username; this.issuedAtMillis = issuedAtMillis; this.expiresAtMillis = expiresAtMillis; this.jti = jti;
        }
    }
}
