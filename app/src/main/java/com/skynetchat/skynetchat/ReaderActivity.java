package com.skynetchat.skynetchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Activity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import static com.skynetchat.skynetchat.R.id.view;

public class ReaderActivity extends AppCompatActivity {
    private Button scan_btn;
    private Button input_btn;
    SharedPreferences prefs;
    SharedPreferences.Editor edit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        scan_btn = (Button) findViewById(R.id.scan_btn);
        final Activity activity = this;
        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(activity);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setPrompt("Scan");
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();
            }
        });
        Button input_btn = (Button) findViewById(R.id.input_btn);
        final EditText email = (EditText) findViewById(R.id.emailText);
        final EditText key = (EditText) findViewById(R.id.keyText);

        input_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                prefs = getSharedPreferences("Keys", Context.MODE_PRIVATE);
                edit = prefs.edit();
                edit.putString(email.getText().toString(), key.getText().toString());
                edit.apply();
                Toast.makeText(getApplicationContext(), "Successfully added", Toast.LENGTH_SHORT).show();
                //finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "You cancelled the scanning.", Toast.LENGTH_SHORT).show();
            }
            else {
                //split result into mail and key
                String[] parts = result.getContents().split("-");
                String mail = parts[0];
                Log.d("MAIL", mail);
                String key = parts[1];
                Log.d("KEY", parts.length + "");

                prefs = getSharedPreferences("Keys", Context.MODE_PRIVATE);
                try {

                    X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(key, Base64.DEFAULT));
                    KeyFactory fact = KeyFactory.getInstance("RSA");
                    PublicKey publicKey = fact.generatePublic(spec);
                    edit = prefs.edit();
                    edit.putString(mail, Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT));
                    edit.apply();
                    //Toast.makeText(getApplicationContext(), , Toast.LENGTH_SHORT).show();
                } catch(Exception e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), "Successfully scanned QR code for key.", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
