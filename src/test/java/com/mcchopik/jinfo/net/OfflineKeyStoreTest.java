package com.mcchopik.jinfo.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OfflineKeyStoreTest {

    @Test
    void createsSignsAndPersists(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("keys.json");
        OfflineKeyStore store = new OfflineKeyStore(file);
        assertTrue(store.loadOrCreate());
        assertTrue(file.toFile().exists());

        String message = "Steve:" + System.currentTimeMillis();
        String signature = store.signBase64(message);

        byte[] pubBytes = Base64.getDecoder().decode(store.publicKeyBase64());
        PublicKey pub = KeyFactory.getInstance("Ed25519")
                .generatePublic(new java.security.spec.X509EncodedKeySpec(pubBytes));
        Signature sig = Signature.getInstance("Ed25519");
        sig.initVerify(pub);
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        assertTrue(sig.verify(Base64.getDecoder().decode(signature)));
    }

    @Test
    void reloadKeepsSameKey(@TempDir Path dir) {
        Path file = dir.resolve("keys.json");
        OfflineKeyStore first = new OfflineKeyStore(file);
        first.loadOrCreate();
        String pub = first.publicKeyBase64();

        OfflineKeyStore second = new OfflineKeyStore(file);
        assertFalse(second.loadOrCreate());
        assertEquals(pub, second.publicKeyBase64());
    }
}