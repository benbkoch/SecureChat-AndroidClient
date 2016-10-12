package com.skynetchat.skynetchat;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MessageActivity extends AppCompatActivity {

    SharedPreferences prefs;
    SharedPreferences.Editor edit;
    String receiver_email;
    int conversation_id = 0;
    boolean ready = false;
    final String[] textArea = new String[1];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext() );

        final TextView conversationText = (TextView) findViewById(R.id.conversationText);
        final EditText messageText = (EditText) findViewById(R.id.messageText);

        Thread t;


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
                        Response<JSONObject> response = webb.post("/messages/create").header("Access-Token", prefs.getString("Access-Token", "No Token")).header("Client", prefs.getString("Client", "No Token")).header("Token-Type", "Bearer").header("UID", prefs.getString("userEmail", "No email")).param("conversation_id", conversation_id).param("body", messageText.getText()).asJsonObject();
                        edit = prefs.edit();
                        if(response.getHeaderField("Access-Token") != null) {
                            edit.putString("Access-Token", response.getHeaderField("Access-Token"));
                            edit.putString("Client", response.getHeaderField("Client"));
                            edit.apply();
                        }

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

        Bundle b = getIntent().getExtras();
        receiver_email = "error";
        if(b != null) {
            receiver_email = b.getString("receiver_email");
        }

        t = new Thread(new Runnable() {
            @Override
            public void run() {
                //while(!ready);
                Webb webb = Webb.create();
                webb.setBaseUri("https://skynetchat.herokuapp.com");
                //conversation_id = -1;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext() );
                Log.d("Test", prefs.getString("Access-Token", "No Token"));
                Response<JSONObject> response = webb.post("/conversations/create").header("Access-Token", prefs.getString("Access-Token", "No Token")).header("Client", prefs.getString("Client", "No Token")).header("Token-Type", "Bearer").header("UID", prefs.getString("userEmail", "No email")).param("recipient_email", receiver_email).asJsonObject();
                if(response.getHeaderField("Access-Token") != null) {
                    edit = prefs.edit();
                    edit.putString("Access-Token", response.getHeaderField("Access-Token"));
                    edit.putString("Client", response.getHeaderField("Client"));
                    edit.apply();
                }
                //edit.commit();
                try {
                    conversation_id = response.getBody().getInt("id");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                while (true) {
                    Response<JSONArray> mResponse = webb.get("/messages/index").header("Access-Token", prefs.getString("Access-Token", "No Token")).header("Client", prefs.getString("Client", "No Token")).header("Token-Type", "Bearer").header("UID", prefs.getString("userEmail", "No email")).param("conversation_id", conversation_id).asJsonArray();
                    JSONArray array = mResponse.getBody();
                    if(mResponse.getHeaderField("Access-Token") != null) {
                        edit = prefs.edit();
                        edit.putString("Access-Token", mResponse.getHeaderField("Access-Token"));
                        edit.putString("Client", mResponse.getHeaderField("Client"));
                        edit.apply();
                    }
                    //edit.commit();
                    if(array != null && array.length() >  0) {
                        try {
                            String message = conversationText.getText().toString();
                            for (int i = 0; i < array.length(); i++) {

                                JSONObject curr = array.getJSONObject(i);
                                message += receiver_email + ": " + curr.getString("body") + "\n";
                            }

                            textArea[0] = message;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    conversationText.setText(textArea[0]);

                                }
                            });


                        } catch (JSONException e) {
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

        t.start();
    }
}
