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
import android.widget.Button;
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

    private Button add_btn;
    private Button share_btn;

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

        add_btn = (Button)findViewById(R.id.add_btn);
        share_btn = (Button)findViewById(R.id.share_btn);

        //user clicks add keys
        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ConversationsActivity.this, ReaderActivity.class));
            }
        });
        //user clicks share key
        share_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ConversationsActivity.this, GeneratorActivity.class));
            }
        });

        listItems = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems );
        setListAdapter(adapter);

        accessToken = prefs.getString("authorization", "No Token");

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Webb webb = Webb.create();
                webb.setBaseUri("https://skynetchat.herokuapp.com");
                Response<JSONArray> response = webb.get("/conversations/index").header("authorization", "Bearer " + accessToken).header("Content-Type", "application/json").asJsonArray();
                SharedPreferences.Editor edit;
                if(response.getStatusCode() != 200)
                    finish();
//                edit = prefs.edit();
//
//                if(response.getHeaderField("Access-Token") != null) {
//                    edit.putString("Access-Token", response.getHeaderField("Access-Token"));
//                    edit.putString("Client", response.getHeaderField("Client"));
//                    edit.commit();
//                }
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

    @Override
    public void onBackPressed() {
        Log.d("Kill", "kill");
        super.onBackPressed();
        this.finish();
    }

}
