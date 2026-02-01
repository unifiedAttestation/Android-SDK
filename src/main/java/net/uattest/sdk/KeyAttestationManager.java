package net.uattest.sdk;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

public class KeyAttestationManager {
    private static final String PROVIDER = "AndroidKeyStore";
    public static List<String> getAttestationChain(
            Context context,
            String alias,
            byte[] requestHash
    ) throws Exception {
        KeyStore ks = KeyStore.getInstance(PROVIDER);
        ks.load(null);

        if (ks.containsAlias(alias)) {
            ks.deleteEntry(alias);
        }
        KeyGenParameterSpec.Builder specBuilder = new KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY
        )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(requestHash);

        try {
            KeyGenParameterSpec spec = specBuilder.build();
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC,
                    PROVIDER
            );
            kpg.initialize(spec);
            kpg.generateKeyPair();
        } catch (Exception ecFailure) {
            KeyGenParameterSpec spec = specBuilder
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .build();
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    PROVIDER
            );
            kpg.initialize(spec);
            kpg.generateKeyPair();
        }

        Certificate[] chain = ks.getCertificateChain(alias);
        if (chain == null || chain.length == 0) {
            throw new IllegalStateException("No attestation certificate chain");
        }
        List<String> output = new ArrayList<>();
        for (Certificate cert : chain) {
            output.add(Base64.encodeToString(cert.getEncoded(), Base64.NO_WRAP));
        }
        return output;
    }

}
