package org.eu.jsw3286.hrmreporter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;


import androidx.core.app.ActivityCompat;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eu.jsw3286.hrmreporter.databinding.ActivityMainBinding;

public class MainActivity extends Activity implements SensorEventListener, IMqttActionListener {

    private TextView mHR;
    private TextView tAccu;
    private TextView txtConn;
    private ActivityMainBinding binding;
    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
    private MQTTPublisher mqtt;
    private SharedPreferences sharedPref;
    private String currentAddr = "";
    private boolean connected = true;

    private boolean state = false;

    private ConnectivityManager connectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectivityManager  = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        sharedPref = this.getSharedPreferences("org.eu.jsw3286.hrmreporter", Context.MODE_PRIVATE);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, 100);
        }

        mHR = binding.HR;
        tAccu = binding.tAccu;
        txtConn = binding.txtConnected;

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        mHR.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onDoubleClick() {
                Log.d("BPMLabel", "Dblclicked");
                Intent switchActivityIntent = new Intent(getApplicationContext(), InputMQTTAddr.class);
                startActivity(switchActivityIntent);
            }
        });

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 0.3F;
        getWindow().setAttributes(lp);

        bindProcessToWifi();
        startMeasure();
    }



    private void startMeasure() {
        if (state) return;
        boolean sensorRegistered = mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_UI);
        Log.d("Sensor Status", "Sensor registered: " + (sensorRegistered ? "y" : "n"));
        state = sensorRegistered;

    }

    private void connectMQTT() {
        if (this.mqtt != null) {
            try {
                this.mqtt.disconnect();
                this.connected = false;
                this.txtConn.setText("No");
            } catch (MqttException e) {
                Log.e("MQTTProvider", Log.getStackTraceString(e));
            }
            this.mqtt = null;
        }
        String mqttServer = sharedPref.getString("org.eu.jsw3286.hrmreporter.mqtt", "");
        this.currentAddr = mqttServer;
        if (mqttServer.length() > 4) {
            try {
                this.mqtt = new MQTTPublisher(this.getApplicationContext(), mqttServer);
                this.mqtt.connect(this);
            } catch (MqttException e) {
                Log.e("MQTTProvider", Log.getStackTraceString(e));
            }
        }
    }

    private void stopMeasure() {
        if (!state) return;
        mSensorManager.unregisterListener(this);
        Log.d("Sensor Status", "Sensor unregistered");
        mHR.setText("---");
        state = false;

        connectivityManager.releaseNetworkRequest(null);

        if (this.mqtt != null) {
            try {
                this.mqtt.disconnect();
            } catch (MqttException e) {
                Log.e("MQTTProvider", Log.getStackTraceString(e));
            }
            this.connected = false;
            this.txtConn.setText("No");
            this.mqtt = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float mHeartRateFloat = sensorEvent.values[0];
        int mHeartRate = Math.round(mHeartRateFloat);
        mHR.setText(Integer.toString((mHeartRate)));
        tAccu.setText(Integer.toString(sensorEvent.accuracy));

        String mqttServer = sharedPref.getString("org.eu.jsw3286.hrmreporter.mqtt", "");
        if (this.currentAddr != mqttServer) {
            this.connectMQTT(); // reconnect
        }

        if (this.mqtt != null && this.connected) {
            try {
                this.mqtt.send(mHeartRate, sensorEvent.accuracy);
            } catch (MqttException e) {
                Log.e("MQTTProvider", Log.getStackTraceString(e));
                this.connectMQTT();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        tAccu.setText(Integer.toString(i));
    }

    @Override
    protected void onPause() {
        this.stopMeasure();
        this.unbindProcessFromWifi();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.bindProcessToWifi();
        this.startMeasure();
    }

    private void bindProcessToWifi() {
        NetworkCallback networkCallback = new NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                // The Wi-Fi network has been acquired, bind it to use this network by default
                connectivityManager.bindProcessToNetwork(network);

                connectMQTT();
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                // The Wi-Fi network has been disconnected
            }
        };
        connectivityManager.requestNetwork(
                new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                networkCallback
        );
    }

    private void unbindProcessFromWifi() {
        connectivityManager.bindProcessToNetwork(null);
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        this.connected = true;
        this.txtConn.setText("Yes");
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        Log.e("MQTTProvider", Log.getStackTraceString(exception));
        return;
    }
}