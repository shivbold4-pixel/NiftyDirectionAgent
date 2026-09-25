package com.niftydirection.agent;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecretStore {
    private static final String PREFS="secure_prefs", KEY_ALIAS="nifty_agent_api_key", DATA_KEY="api_key_blob";
    private SecretStore(){}
    public static void putApiKey(Context c,String apiKey)throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);SecretKey key;
        if(!ks.containsAlias(KEY_ALIAS)){
            KeyGenerator kg=KeyGenerator.getInstance("AES","AndroidKeyStore");
            kg.init(new android.security.keystore.KeyGenParameterSpec.Builder(KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT|android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE).build());
            key=kg.generateKey();
        }else key=((KeyStore.SecretKeyEntry)ks.getEntry(KEY_ALIAS,null)).getSecretKey();
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);
        byte[] iv=cipher.getIV(), enc=cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8)), blob=new byte[iv.length+enc.length];
        System.arraycopy(iv,0,blob,0,iv.length);System.arraycopy(enc,0,blob,iv.length,enc.length);
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(DATA_KEY,Base64.encodeToString(blob,Base64.NO_WRAP)).apply();
    }
    public static String getApiKey(Context c)throws Exception{
        String e=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(DATA_KEY,null);if(e==null||e.isEmpty())return null;
        byte[] blob=Base64.decode(e,Base64.NO_WRAP);int ivLen=12;if(blob.length<=ivLen)return null;
        byte[] iv=new byte[ivLen], enc=new byte[blob.length-ivLen];System.arraycopy(blob,0,iv,0,ivLen);System.arraycopy(blob,ivLen,enc,0,enc.length);
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);SecretKey key=((KeyStore.SecretKeyEntry)ks.getEntry(KEY_ALIAS,null)).getSecretKey();
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));
        return new String(cipher.doFinal(enc),StandardCharsets.UTF_8);
    }
    public static void clearApiKey(Context c){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(DATA_KEY).apply();}
}
