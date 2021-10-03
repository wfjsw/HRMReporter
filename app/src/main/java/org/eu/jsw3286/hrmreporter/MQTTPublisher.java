package org.eu.jsw3286.hrmreporter;

import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class MQTTPublisher {
    private final String topic        = "org/eu/jsw3286/mqtt/hrm";
    // String content      = "Message from MqttPublishSample";
    private final int qos             = 0;
    private String broker;
    // String broker       = "tcp://iot.eclipse.org:1883";
    private String clientId     = "WearHRMReporter";
    private MemoryPersistence persistence = new MemoryPersistence();
    private MqttAndroidClient client;

    public MQTTPublisher(android.content.Context ctx, String broker) throws MqttException {
        this.broker = broker;
        client = new MqttAndroidClient(ctx, broker, clientId, persistence);
    }

    public IMqttToken connect(IMqttActionListener cb) throws MqttException {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setConnectionTimeout(5000);
        Log.i("MQTT", "Connecting to broker: "+this.broker);
        return client.connect(connOpts, cb);
    }

    public IMqttToken disconnect() throws MqttException {
        if (!client.isConnected()) return null;
        Log.i("MQTT", "MQTT Disconnected");
        return client.disconnect();
    }

    public IMqttToken send(int bpm, int accuracy) throws MqttException {
        if (!client.isConnected()) return null;
        Log.d("MQTT", "Reporting BPM: " + bpm);
        String data = String.format("{\"a\":%d,\"b\":%d}", accuracy, bpm);
        MqttMessage message = new MqttMessage(data.getBytes(StandardCharsets.UTF_8));
        message.setQos(qos);
        return client.publish(topic, message);
    }
}
