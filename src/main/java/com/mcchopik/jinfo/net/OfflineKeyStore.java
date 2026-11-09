package com.mcchopik.jinfo.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class OfflineKeyStore {

    private final Path file;
    private PrivateKey privateKey;
    private String publicKeyBase64;

    public OfflineKeyStore(Path file) {
        this.file = file;
    }

    public boolean loadOrCreate() {
        if (load()) {
            return false;
        }
        create();
        save();
        return true;
    }

    private boolean load() {
        try {
            if (!Files.exists(file)) {
                return false;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("privateKey") || !obj.has("publicKey")) {
                return false;
            }
            byte[] priv = Base64.getDecoder().decode(obj.get("privateKey").getAsString());
            byte[] pub = Base64.getDecoder().decode(obj.get("publicKey").getAsString());
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(priv));
            PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(pub));
            this.publicKeyBase64 = Base64.getEncoder().encodeToString(pubKey.getEncoded());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void create() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            KeyPair kp = kpg.generateKeyPair();
            this.privateKey = kp.getPrivate();
            this.publicKeyBase64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("Ed25519 is not available", e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("privateKey", Base64.getEncoder().encodeToString(privateKey.getEncoded()));
            obj.addProperty("publicKey", publicKeyBase64);
            Files.writeString(file, obj.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public String publicKeyBase64() {
        return publicKeyBase64;
    }

    public String signBase64(String message) {
        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(privateKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign offline auth message", e);
        }
    }
}
