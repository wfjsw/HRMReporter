package org.eu.jsw3286.hrmreporter;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.eu.jsw3286.hrmreporter.databinding.ActivityInputMqttaddrBinding;

public class InputMQTTAddr extends Activity {

    private ActivityInputMqttaddrBinding binding;
    private EditText txtServer;
    private Button btnSave;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_mqttaddr);

        binding = ActivityInputMqttaddrBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        txtServer = binding.txtMQTTServer;
        btnSave = binding.btnSave;

        sharedPref = this.getSharedPreferences("org.eu.jsw3286.hrmreporter", Context.MODE_PRIVATE);
        String mqttServer = sharedPref.getString("org.eu.jsw3286.hrmreporter.mqtt", "");
        txtServer.setText(mqttServer);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("org.eu.jsw3286.hrmreporter.mqtt", String.valueOf(txtServer.getText()));
                editor.commit();
                finish();
            }
        });
    }
}