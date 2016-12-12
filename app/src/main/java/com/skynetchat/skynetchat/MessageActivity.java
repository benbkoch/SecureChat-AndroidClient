package com.skynetchat.skynetchat;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class MessageActivity extends AppCompatActivity {

    SharedPreferences prefs;
    SharedPreferences.Editor edit;
    String receiver_email;
    int conversation_id = 0;
    boolean ready = false;
    final String[] textArea = new String[1];

    private static String salt;
    private static int iterations = 65536;
    private static int keySize = 256;
    private static byte[] ivBytes;
    private static SecretKey secretKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);





    }

    public EncryptedData encrypt(String message) throws Exception {
        SecureRandom r = SecureRandom.getInstance("SHA1PRNG");
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // for example
        SecretKey secretKey = keyGen.generateKey();
        byte[] encodedKey = secretKey.getEncoded();
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
        byte[] encrypted = c.doFinal(message.getBytes());


        SecretKey hmacKey = keyGen.generateKey();
        byte[] hmacKeyEncoded = hmacKey.getEncoded();
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(hmacKey);
        byte[] hmac = m.doFinal(encrypted);

        return new EncryptedData(Base64.encodeToString(hmac, Base64.DEFAULT), Base64.encodeToString(encrypted, Base64.DEFAULT), Base64.encodeToString(hmacKeyEncoded, Base64.DEFAULT), Base64.encodeToString(encodedKey, Base64.DEFAULT));
    }


    public String decrypt(String cipherText, byte[] hmacKeyEncoded, byte[] encryptKeyEncoded) throws Exception {

        String[] inputSplit = cipherText.split("-");
        byte[] hmac = Base64.decode(inputSplit[0], Base64.DEFAULT);
        byte[] encrypted = Base64.decode(inputSplit[1], Base64.DEFAULT);

        Mac m = Mac.getInstance("HmacSHA256");
        SecretKey hmacKey = new SecretKeySpec(hmacKeyEncoded, 0, hmacKeyEncoded.length, "HmacSHA256");
        m.init(hmacKey);
        byte[] hmacTest = m.doFinal(encrypted);


        //check for valid hmac
        if(!MessageDigest.isEqual(hmac, hmacTest))
            throw new Exception("invalid");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKey encryptKey = new SecretKeySpec(encryptKeyEncoded, 0, encryptKeyEncoded.length, "AES");
        c.init(Cipher.DECRYPT_MODE, encryptKey, new IvParameterSpec(new byte[16]));
        byte[] decrypted = c.doFinal(encrypted);
        Log.d("NO 64", new String(decrypted, "UTF-8"));
        return new String(decrypted, "UTF-8");


    }

     protected void onResume() {

        super.onResume();

        final TextView conversationText = (TextView) findViewById(R.id.conversationText);

        FileInputStream fis = null;

        Bundle b = getIntent().getExtras();
        receiver_email = "error";
        if(b != null) {
            receiver_email = b.getString("receiver_email");
        }

        try {

            fis = getApplicationContext().openFileInput("messages_" + receiver_email);
            InputStreamReader isr = new InputStreamReader(fis);
            // READ STRING OF UNKNOWN LENGTH
            StringBuilder sb = new StringBuilder();
            char[] inputBuffer = new char[2048];
            int l;
            // FILL BUFFER WITH DATA
            while ((l = isr.read(inputBuffer)) != -1) {
                sb.append(inputBuffer, 0, l);
            }
            // CONVERT BYTES TO STRING
            String readString = sb.toString();
            conversationText.setText(readString);
            fis.close();
        }
        catch (Exception e) {

        } finally {
            if (fis != null) {
                fis = null;
            }
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext() );


        final EditText messageText = (EditText) findViewById(R.id.messageText);

        Thread t;

        //check if receiver public key is available
        //if not, ask user to exchange keys/QR
        SharedPreferences forKeys = getSharedPreferences("Keys", Context.MODE_PRIVATE);
        if(forKeys.getString(receiver_email, "") == "") {
            Toast.makeText(getApplicationContext(), "Recipient public key not found. Click 'ADD' to manually input or scan recipient key.", Toast.LENGTH_LONG).show();
        }

        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread t;
                t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Webb webb = Webb.create();
                        webb.setBaseUri("https://skynetchat.herokuapp.com");

                        //ENCRYPT here
                        SharedPreferences forKeys = getSharedPreferences("Keys", Context.MODE_PRIVATE);
                        String receiverKey = forKeys.getString(receiver_email, "");
                        byte[] decodedKey = Base64.decode(receiverKey, Base64.DEFAULT);



                        //get salt
//                        try{
//                            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
//                            byte[] salt = new byte[20];
//                            sr.nextBytes(salt);
//                        }
//                        catch (NoSuchAlgorithmException e) {
//                            e.printStackTrace();
//                        }
                        //message text to char[]
                        String getMessage = messageText.getText().toString();
//                        byte[] message = getMessage.getBytes();

                        //byte[] iv = null;
                        String cipherString = null;
                        EncryptedData data = null;
                        try {

                            data = encrypt(getMessage);
                            String encryptedMessage = data.hmacKey + "-" + data.encryptKey;

                            //RSA encrypt
                            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedKey));
                            Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA256AndMGF1Padding");
                            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                            byte[] cipherBytes = cipher.doFinal(encryptedMessage.getBytes());
                            cipherString = Base64.encodeToString(cipherBytes, Base64.DEFAULT);
                            //cipherString = cipher.doFinal(encryptedMessage.getBytes())
//                            AlgorithmParameters params = cipher.getParameters();
//                            //iv = params.getParameterSpec(IvParameterSpec.class).getIV();
//                            byte[] cypherText = cipher.doFinal(encryptedMessage.getBytes());
//                            cipherString = new String(cypherText, "UTF-8");
                            Log.d("Cipher", cipherString);
                        }
                        catch(NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                        catch(InvalidKeySpecException e) {
                            e.printStackTrace();
                        }
                        catch(NoSuchPaddingException e) {
                            e.printStackTrace();
                        }
                        catch(InvalidKeyException e) {
                            e.printStackTrace();
                        }
                        catch(IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                        catch(BadPaddingException e) {
                            e.printStackTrace();
                        }
                        catch(UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }

                        //Send RSA handshake
                        Response<JSONObject> response = webb.post("/messages/create").header("authorization", "Bearer " + prefs.getString("authorization", "No Token")).param("conversation_id", conversation_id).param("body", cipherString).param("handshake", true).asJsonObject();

                        //Send encrypted data
                        String encryptedMessage = data.hmac + "-" + data.encryptedText;
                        Response<JSONObject> response2 = webb.post("/messages/create").header("authorization", "Bearer " + prefs.getString("authorization", "No Token")).param("conversation_id", conversation_id).param("body", encryptedMessage).param("handshake", false).asJsonObject();

                        Log.d("RESPONSE", response.getStatusCode() + "");
                        Log.d("RESPONSE2", response2.getStatusCode() + "");
                        //edit = prefs.edit();
//                        if(response.getHeaderField("Access-Token") != null) {
//                            edit.putString("Access-Token", response.getHeaderField("Access-Token"));
//                            edit.putString("Client", response.getHeaderField("Client"));
//                            edit.commit();
//                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext() );
                                conversationText.setText(conversationText.getText().toString() + prefs.getString("userEmail", "Unknown") + ": " + messageText.getText() + "\n");
                                messageText.setText("");

                            }
                        });
                        //edit.commit();
                    }
                });
                t.start();

            }
        });

        //final TextView conversationText = (TextView) findViewById(R.id.conversationText);

        Thread loop = new Thread(new Runnable() {
            @Override
            public void run() {
                //while(!ready);
                Webb webb = Webb.create();
                webb.setBaseUri("https://skynetchat.herokuapp.com");
                //conversation_id = -1;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext() );
                //Log.d("Test", prefs.getString("Access-Token", "No Token"));
                Response<JSONObject> response = webb.post("/conversations/create").header("authorization", "Bearer " + prefs.getString("authorization", "No Token")).param("recipient_email", receiver_email).asJsonObject();
//                if(response.getHeaderField("Access-Token") != null) {
//                    edit = prefs.edit();
//                    edit.putString("Access-Token", response.getHeaderField("Access-Token"));
//                    edit.putString("Client", response.getHeaderField("Client"));
//                    edit.commit();
//                }
                //edit.commit();
                try {
                    conversation_id = response.getBody().getInt("id");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                String lastHmacKey = "";
                String lastEncryptKey = "";

                while (true) {
                    Response<JSONArray> mResponse = webb.get("/messages/index").header("authorization", "Bearer " + prefs.getString("authorization", "No Token")).param("conversation_id", conversation_id).asJsonArray();
                    JSONArray array = mResponse.getBody();
                    HashMap<Integer, String> hmacs = new HashMap<Integer, String>();
                    HashMap<Integer, String> aesKeys = new HashMap<Integer, String>();

                    if(array != null && array.length() >  0) {
                        try {
                            //String message = conversationText.getText().toString();

                            //convert ciphermessage string to byte
                            for(int i = 0; i < array.length(); i++) {
                                if(array.getJSONObject(i).getBoolean("handshake")) {
                                    //Do RSA decrypt
                                    try {

                                        //RSA decrypt
                                        String encrypted = array.getJSONObject(i).getString("body");
                                        SharedPreferences forKeys = getSharedPreferences("Keys", Context.MODE_PRIVATE);
                                        String myKey = forKeys.getString("myPrivateKey", "");

                                        byte[] cipherByte = encrypted.getBytes();

                                        byte[] decodedKey = Base64.decode(myKey, Base64.DEFAULT);
                                        Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA256AndMGF1Padding");
                                        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
                                        //cipher.init(Cipher.DECRYPT_MODE, secKey, new IvParameterSpec(iv));
                                        cipher.init(Cipher.DECRYPT_MODE, privateKey);
                                        byte[] messageByte = cipher.doFinal(Base64.decode(cipherByte, Base64.DEFAULT));
                                        //convert back to string
                                        String result = new String(messageByte, "UTF-8");
                                        Log.d("After RSA decrypt", result);
                                        Log.d("After RSA decrypt no 64", new String(messageByte, "UTF-8"));
                                        String[] resultSplit = result.split("-");
                                        Log.d("LENGTH", resultSplit.length + "");
                                        hmacs.put(array.getJSONObject(i).getInt("id"), resultSplit[0]);
                                        aesKeys.put(array.getJSONObject(i).getInt("id"), resultSplit[1]);

                                    }
                                    catch(InvalidKeySpecException e) {
                                        e.printStackTrace();
                                    }
                                    catch(NoSuchAlgorithmException e) {
                                        e.printStackTrace();
                                    }
                                    catch(NoSuchPaddingException e) {
                                        e.printStackTrace();
                                    }
                                    catch(InvalidKeyException e) {
                                        e.printStackTrace();
                                    }
                                    catch(IllegalBlockSizeException e) {
                                        e.printStackTrace();
                                    }
                                    catch(BadPaddingException e) {
                                        e.printStackTrace();
                                    }

                                    catch(Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                else {
                                    //Decrypt with AES
                                    int id = array.getJSONObject(i).getInt("id");
                                    //Log.d
                                    String plainText = decrypt(array.getJSONObject(i).getString("body"), Base64.decode(hmacs.get(id - 1), Base64.DEFAULT), Base64.decode(aesKeys.get(id - 1), Base64.DEFAULT));
                                    String message = conversationText.getText().toString();
                                    message += receiver_email + ": " + plainText + "\n";
                                    textArea[0] = message;

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            conversationText.setText(textArea[0]);

                                        }
                                    });

                                }



                            }






                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        loop.start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        final TextView conversationText = (TextView) findViewById(R.id.conversationText);

        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput("messages_" + receiver_email, getApplicationContext().MODE_PRIVATE);
            outputStream.write(conversationText.getText().toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("Kill", "kill");
        super.onBackPressed();
        this.finish();
    }
}
