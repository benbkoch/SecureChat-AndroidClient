package com.skynetchat.skynetchat;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Button register = (Button) findViewById(R.id.register);
        final EditText email = (EditText) findViewById(R.id.emailRegisterText);
        final EditText password = (EditText) findViewById(R.id.passwordRegisterText);
        final EditText passConfirm = (EditText) findViewById(R.id.passwordConfirmText);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(email.getText().toString().equals("") || password.getText().toString().equals("") || passConfirm.getText().toString().equals(""))
                    return;
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Webb webb = Webb.create();
                        webb.setBaseUri("https://skynetchat.herokuapp.com");
                        String success = "";
                        Response<JSONObject> response = webb.post("/register_user").param("email", email.getText()).param("password", password.getText()).param("password_confirmation", passConfirm.getText()).asJsonObject();
                        int status = response.getStatusCode();


                        if (status != 200) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Error registering, please try again", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //SharedPreferences prefs = getSharedPreferences("Keys", Context.MODE_PRIVATE);
                                    //SharedPreferences.Editor edit = prefs.edit();
                                    //edit.putString("password", password.getText().toString());

                                    Toast.makeText(getApplicationContext(), "Successfully registered, please check email to confirm your account", Toast.LENGTH_LONG).show();
                                    finish();

                                }
                            });
                        }
                    }
                });

                t.start();
            }

        });
    }
}

