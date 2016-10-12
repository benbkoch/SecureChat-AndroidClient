package com.skynetchat.skynetchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;

public class LoginActivity extends AppCompatActivity {


    HttpsURLConnection connection;
    JSONObject data;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext() );

        data = new JSONObject();

        final Button button = (Button) findViewById(R.id.loginButton);
        final Button registerButton = (Button) findViewById(R.id.registerButton);
        final EditText emailText = (EditText) findViewById(R.id.emailText);
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

                            Response<JSONObject> response = webb.post("/auth/sign_in").param("email", emailText.getText().toString().toLowerCase()).param("password", passwordText.getText()).asJsonObject();
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

                            String accessToken = response.getHeaderField("Access-Token");
                            String clientToken = response.getHeaderField("Client");
                            Log.d("Token", accessToken);
                            Log.d("client", clientToken);
                            SharedPreferences prefs = prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext() );
                            SharedPreferences.Editor edit = prefs.edit();
                            edit.putString("userEmail", emailText.getText().toString().toLowerCase());

                            if(response.getHeaderField("Access-Token") != null) {
                                edit.putString("Access-Token", response.getHeaderField("Access-Token"));
                                edit.putString("Client", response.getHeaderField("Client"));
                            }
                            edit.apply();
                            //edit.commit();

                            Log.d("Result", apiResult.toString());

                            Intent intent = new Intent(getApplicationContext(), ConversationsActivity.class);
                            startActivity(intent);

                        } catch(Exception e) {
                            Log.d("Error!:", e.toString());
                        }

//                        try {
//                            Log.d("Got here", "1");
//                            connection = (HttpsURLConnection) new URL("https://skynetchat.herokuapp.com/auth/sign_in").openConnection();
//                            //connection.setDoOutput(true);
//                            connection.setRequestMethod("POST");
//                            connection.setRequestProperty("Contect-Type", "application/json");
//                            //connection.setRequestProperty("Accept", "application/json");
//                            connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
//                            connection.setRequestProperty("Accept","*/*");
//                            data.put("email", emailText.getText());
//                            data.put("password", passwordText.getText());
//                            Log.d("Got here", "Got here");
//                            Writer writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
//                            Log.d("Got here", "Got here 2");
//                            String jsonResponse = "";
//                            Log.d("Json data", data.toString());
//                            writer.write(URLEncoder.encode(data.toString()));
//                            Log.d("Got here", "Got here 3");
//                            writer.flush();
//                            writer.close();
//                            Log.d("Got here", "Got here 4");
//                            int status = connection.getResponseCode();
//                            Log.d("status", "" + status);
//                            InputStream input = connection.getInputStream();
//                            Log.d("Got here", "Got here 5");
//                            StringBuffer buffer = new StringBuffer();
//                            if(input == null)
//                                return;
//                            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
//                            String inputLine;
//                            while((inputLine = reader.readLine()) != null) {
//                                buffer.append(inputLine + "\n");
//                            }
//
//                            jsonResponse = buffer.toString();
//                            Log.d("JSon respone", jsonResponse);
//
//                        } catch(Exception e) {
//                            Log.d("Error", e.toString());
//                        } finally {
//                            connection.disconnect();
//                        }
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
