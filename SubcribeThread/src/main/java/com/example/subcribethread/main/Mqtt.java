package com.example.subcribethread.main;


import com.example.subcribethread.model.ControlState;
import com.example.subcribethread.model.StatusIot;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.subcribethread.repository.ControlStateRepository;
import com.example.subcribethread.repository.StatusIotRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Random;

@Component
public class Mqtt {
    private final String SERVER_URI = "tcp://broker.mqttdashboard.com:1883";
    private final String USERNAME = "hlight";
    private final String PASSWORD = "hlight";
    private final String PUBLISH_TOPIC = "hlight/PTIT/IOT/command";
    private final String SUBCRIBE_TOPIC = "hlight/PTIT/IOT/state";
    private MqttClient mqttClient;
    private MqttConnectOptions options;
    private WeatherForecastingModelConnector predictor;

    @Autowired
    private StatusIotRepository statusIotRepository;
    @Autowired
    private ControlStateRepository controlStateRepository;

    public Mqtt() throws MqttException {
        predictor = new WeatherForecastingModelConnector();
        mqttClient = new MqttClient(
                SERVER_URI,
                MqttClient.generateClientId(),
                new MemoryPersistence()
        );
        options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        options.setConnectionTimeout(1);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) { //Called when the client lost the connection to the broker
                System.out.println("Connection lost...");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                byte[] bytes = message.getPayload();
                String data = new String(bytes, StandardCharsets.UTF_8);
                System.out.println("Receive data:" + data);

                String[] attributes = data.split("\\|");
                StatusIot statusIot = new StatusIot(
                        null,
                        Double.parseDouble(attributes[0]),
                        Double.parseDouble((attributes[1])),
                        new Random().nextDouble() * 20 + 10,
                        attributes[2] == "1"?1:0
                );
                statusIotRepository.save(statusIot);

                ControlState control = controlStateRepository.getAllOrderByIdDesc().get(0);
                control.setPower(predictor.predict1(statusIot)?1:0);
                post(control.getFan(), control.getHeater(), control.getPower());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {//Called when a outgoing publish is complete
                System.out.println("Delivery completed!");
            }
        });
        mqttClient.connect(options);
    }
    public void post(int fan_level, double heater_temp, int power) throws MqttException, InterruptedException {
        String payload = String.format(
                "{\"fan_level\":%d, \"heater_temperature\":%f, \"power\":%d}",
                fan_level, heater_temp, power
        );
        System.out.println("Control data:" + payload);

        Thread r = new Thread() {
            @Override
            public void run() {
                try {
                    Mqtt.this.mqttClient.publish(PUBLISH_TOPIC, payload.getBytes(StandardCharsets.UTF_8), 0, false);
                } catch (MqttException e) {

                }
            }
        };
        r.start();
        r.join(1000);
        r.interrupt();
    }

    public void get() throws MqttException {
        mqttClient.subscribe(SUBCRIBE_TOPIC, 0);
    }
}
