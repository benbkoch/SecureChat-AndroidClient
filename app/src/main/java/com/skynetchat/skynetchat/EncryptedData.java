package com.skynetchat.skynetchat;

/**
 * Created by TheDude on 12/6/16.
 */

public class EncryptedData {

    public String hmac;
    public String encryptedText;
    public String hmacKey;
    public String encryptKey;

    public EncryptedData(String hmac, String encryptedText, String hmacKey, String encryptKey) {
        this.hmac = hmac;
        this.encryptedText = encryptedText;
        this.hmacKey = hmacKey;
        this.encryptKey = encryptKey;
    }
}
