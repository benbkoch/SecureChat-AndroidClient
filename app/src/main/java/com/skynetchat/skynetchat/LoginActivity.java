package com.skynetchat.skynetchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import javax.crypto.SecretKey;
import javax.net.ssl.HttpsURLConnection;

import java.security.NoSuchAlgorithmException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class LoginActivity extends AppCompatActivity {


    HttpsURLConnection connection;
    JSONObject data;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        final ProgressDialog progress = new ProgressDialog(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext() );

        //Check if current token is valid, then we can skip login
//        if(prefs.contains("Access-Token")) {
//            Log.d("Login", "Has access token");
//
//
//            progress.setTitle("Login");
//            progress.setMessage("Attempting to log back in...");
//            progress.show();
//
//            Thread checkToken = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    Webb webb = Webb.create();
//                    webb.setBaseUri("https://skynetchat.herokuapp.com");
//                    Response<JSONObject> response = webb.get("/auth/validate_token").param("access-token", prefs.getString("Access-Token", "a")).param("client", prefs.getString("Client", "c")).param("uid", prefs.getString("userEmail", "e")).asJsonObject();
//                    Log.d("Validate Token response", response.getStatusCode() + "");
//                    if(response.getStatusCode() == 200) {
//                        //token was valid
//                        SharedPreferences.Editor edit = prefs.edit();
//                        edit.putString("Access-Token", response.getHeaderField("Access-Token"));
//                        edit.putString("Client", response.getHeaderField("Client"));
//                        edit.commit();
//                        Intent intent = new Intent(getApplicationContext(), ConversationsActivity.class);
//                        startActivity(intent);
//                        progress.dismiss();
//                    }
//                    progress.dismiss();
//                }
//            });
//            checkToken.start();






        final Button button = (Button) findViewById(R.id.loginButton);
        final Button registerButton = (Button) findViewById(R.id.registerButton);
        final EditText emailText = (EditText) findViewById(R.id.keyText);
        final EditText passwordText = (EditText) findViewById(R.id.passwordText);

        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                Toast.makeText(getApplicationContext(), message.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(emailText.getText().toString().trim().equals("") || passwordText.getText().toString().trim().equals("")) {
                    //blank input
//                    Toast t = Toast.makeText(getApplicationContext(), "Please input valid email and password", Toast.LENGTH_SHORT);
//                    t.show();
                    return;
                }

                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {



                        try {

                            Webb webb = Webb.create();
                            webb.setBaseUri("https://skynetchat.herokuapp.com");

                            Response<JSONObject> response = webb.post("/auth_user").param("email", emailText.getText().toString().toLowerCase()).param("password", passwordText.getText()).asJsonObject();
                            JSONObject apiResult = response.getBody();

                            int responseCode = response.getStatusCode();

                            if(responseCode != 200) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Error logging in, please try again. Make sure you have confirmed your email.", Toast.LENGTH_SHORT).show();

                                    }
                                });

                            }

                            //check if user key exists
                            SharedPreferences forKeys = getSharedPreferences("Keys", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editKeys = forKeys.edit();
                            if(forKeys.getString("myPrivateKey", "") == "" || !emailText.getText().toString().toLowerCase().equals(forKeys.getString("myMail", "not found"))) {
                                try {
                                    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                                    generator.initialize(2048);
                                    KeyPair keyPair = generator.generateKeyPair();
                                    KeyFactory fact = KeyFactory.getInstance("RSA");
                                    X509EncodedKeySpec pubspec = fact.getKeySpec(keyPair.getPublic(), X509EncodedKeySpec.class);
                                    PKCS8EncodedKeySpec privspec = fact.getKeySpec(keyPair.getPrivate(), PKCS8EncodedKeySpec.class);
                                    //convert keys to string
                                    String publicKeyEncode = Base64.encodeToString(pubspec.getEncoded(), Base64.DEFAULT);

                                    String privateKeyEncode = Base64.encodeToString(privspec.getEncoded(), Base64.DEFAULT);
                                    Log.d("private key", privateKeyEncode);
                                    editKeys.putString("myPublicKey", publicKeyEncode);
                                    editKeys.putString("myPrivateKey", privateKeyEncode);
                                    //Toast.makeText(getApplicationContext(), publicKeyEncode, Toast.LENGTH_LONG).show();
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                            }
                            //save usermail to file
                            editKeys.putString("myMail", emailText.getText().toString().toLowerCase());

                            editKeys.apply();

                            SharedPreferences prefs = prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext() );
                            SharedPreferences.Editor edit = prefs.edit();
                            edit.putString("userEmail", emailText.getText().toString().toLowerCase());
                            JSONObject j = response.getBody();
                            edit.putString("authorization", j.getString("auth_token"));
                            edit.apply();
                            //edit.commit();

                            Log.d("Result", apiResult.toString());

                            Intent intent = new Intent(getApplicationContext(), ConversationsActivity.class);
                            startActivity(intent);

                        } catch(Exception e) {
                            Log.d("Error!:", e.toString());
                        }

                    }
                });

                t.start();


            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivityForResult(intent, 1234);
            }
        });
    }
}
