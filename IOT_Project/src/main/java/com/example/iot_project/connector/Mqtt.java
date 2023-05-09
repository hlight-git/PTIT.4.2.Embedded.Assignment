package com.example.iot_project.connector;


import com.example.iot_project.entity.StatusIot;
import com.example.iot_project.repository.StatusIotRepository;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Mqtt {
    private final String SERVER_URI = "tcp://broker.mqttdashboard.com:1883";
    private final String USERNAME = "hlight";
    private final String PASSWORD = "hlight";
    private final String PUBLISH_TOPIC = "hlight/PTIT/IOT/command";
    private final String SUBCRIBE_TOPIC = "hlight/PTIT/IOT/state";
    private MqttClient mqttClient;
    private MqttConnectOptions options;
    private final StatusIotRepository statusIotRepository;

    public Mqtt(StatusIotRepository statusIotRepository) throws MqttException {
        this.statusIotRepository = statusIotRepository;
        mqttClient = new MqttClient(
                SERVER_URI,
                MqttClient.generateClientId(),
                new MemoryPersistence()
        );
        options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) { //Called when the client lost the connection to the broker
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                byte[] bytes = message.getPayload();
                String data = new String(bytes, StandardCharsets.UTF_8);
                System.out.println(data);
                String[] attributes = data.split("|");
                StatusIot statusIot = new StatusIot(
                        null,
                        Double.parseDouble(attributes[0]),
                        Double.parseDouble((attributes[1])),
                        new Random().nextDouble() * (200 - 20) + 20,
                        attributes[2] == "1"?1:0
                );
                Mqtt.this.statusIotRepository.save(statusIot);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {//Called when a outgoing publish is complete
            }
        });
        mqttClient.connect(options);
    }
    public void post(int fan_level, double heater_temp, int power) throws MqttException {
        String payload = String.format(
                "{\"fan_level\":%d, \"heater_temperature\":%f, \"power\":%d}",
                fan_level, heater_temp, power
        );
        System.out.println(payload);
        mqttClient.publish(PUBLISH_TOPIC, payload.getBytes(StandardCharsets.UTF_8), 2, false);
    }
    // tự start 1 thread riêng, sẽ đợi từ khi gọi cho tới khi nhận được 1 message mới từ topic
    public void get() throws MqttException {
        mqttClient.subscribe(SUBCRIBE_TOPIC, 2);
    }
}
