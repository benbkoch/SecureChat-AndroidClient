package com.skynetchat.skynetchat;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ConversationsActivity extends ListActivity {

    SharedPreferences prefs;

    String accessToken;
    String clientToken;
    ArrayAdapter<String> adapter;
    ArrayList<String> listItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext() );

        final ListView userList = (ListView) findViewById(android.R.id.list);

        userList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String receiver_email = userList.getItemAtPosition(position).toString();

                Intent intent = new Intent(getApplicationContext(), MessageActivity.class);
                Bundle b = new Bundle();
                b.putString("receiver_email", receiver_email);
                intent.putExtras(b);
                startActivity(intent);
                //finish();

            }
        });

        listItems = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems );
        setListAdapter(adapter);

        accessToken = prefs.getString("Access-Token", "No Token");
        clientToken = prefs.getString("Client", "No Token");

        Log.d("Token", accessToken);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Webb webb = Webb.create();
                webb.setBaseUri("https://skynetchat.herokuapp.com");
                Response<JSONArray> response = webb.get("/conversations/index").header("Access-Token", accessToken).header("Client", clientToken).header("Token-Type", "Bearer").header("UID", prefs.getString("userEmail", "")).header("Content-Type", "application/json").asJsonArray();
                SharedPreferences.Editor edit;
                edit = prefs.edit();

                if(response.getHeaderField("Access-Token") != null) {
                    edit.putString("Access-Token", response.getHeaderField("Access-Token"));
                    edit.putString("Client", response.getHeaderField("Client"));
                    edit.apply();
                }
                //edit.commit();

                JSONArray array = response.getBody();
                try {
                    for(int i = 0; i < array.length(); i++) {
                        listItems.add(array.getString(i));

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        adapter.notifyDataSetChanged();


    }

}
