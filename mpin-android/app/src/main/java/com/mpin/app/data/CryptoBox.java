package com.mpin.app.data;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypts small files with an AES-256-GCM key that lives in the Android Keystore
 * (hardware-backed on most phones), so MPINs and the Gmail app password are never
 * stored in plain text.
 */
final class CryptoBox {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "mpin_store_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private CryptoBox() {
    }

    /** Returns the decrypted text, or null if the file does not exist or cannot be read. */
    static synchronized String read(File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            byte[] data = new AtomicFile(file).readFully();
            ByteBuffer buf = ByteBuffer.wrap(data);
            byte[] iv = new byte[buf.get()];
            buf.get(iv);
            byte[] cipherText = new byte[buf.remaining()];
            buf.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (IOException | GeneralSecurityException | RuntimeException e) {
            return null;
        }
    }

    static synchronized void write(File file, String text) throws IOException {
        byte[] out;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] iv = cipher.getIV();
            byte[] cipherText = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            out = ByteBuffer.allocate(1 + iv.length + cipherText.length)
                    .put((byte) iv.length).put(iv).put(cipherText).array();
        } catch (GeneralSecurityException e) {
            throw new IOException("Encryption failed", e);
        }

        AtomicFile atomic = new AtomicFile(file);
        FileOutputStream stream = atomic.startWrite();
        try {
            stream.write(out);
            atomic.finishWrite(stream);
        } catch (IOException e) {
            atomic.failWrite(stream);
            throw e;
        }
    }

    private static SecretKey key() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(ALIAS)) {
            return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
