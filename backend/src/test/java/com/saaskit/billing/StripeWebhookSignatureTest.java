package com.saaskit.billing;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class StripeWebhookSignatureTest {

    private static final String SECRET = "whsec_test_secret";

    private String sign(String payload) throws Exception {
        long ts = System.currentTimeMillis() / 1000;
        String signed = ts + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sig = HexFormat.of().formatHex(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8)));
        return "t=" + ts + ",v1=" + sig;
    }

    @Test
    void acceptsValidSignature() throws Exception {
        String payload = "{\"id\":\"evt_1\",\"object\":\"event\"}";
        var event = Webhook.constructEvent(payload, sign(payload), SECRET);
        assertEquals("evt_1", event.getId());
    }

    @Test
    void rejectsTamperedPayload() throws Exception {
        String payload = "{\"id\":\"evt_1\",\"object\":\"event\"}";
        String header = sign(payload);
        assertThrows(SignatureVerificationException.class, () ->
                Webhook.constructEvent("{\"id\":\"evt_2\",\"object\":\"event\"}", header, SECRET));
    }

    @Test
    void rejectsWrongSecret() throws Exception {
        String payload = "{\"id\":\"evt_1\",\"object\":\"event\"}";
        assertThrows(SignatureVerificationException.class, () ->
                Webhook.constructEvent(payload, sign(payload), "whsec_wrong"));
    }
}
