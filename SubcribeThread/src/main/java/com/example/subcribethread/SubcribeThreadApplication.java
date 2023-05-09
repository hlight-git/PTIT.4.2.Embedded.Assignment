package com.example.subcribethread;

import com.example.subcribethread.main.Mqtt;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SubcribeThreadApplication {

    public static void main(String[] args) throws MqttException {
        ConfigurableApplicationContext context = SpringApplication.run(SubcribeThreadApplication.class, args);
        Mqtt mqtt = context.getBeanFactory().getBean(Mqtt.class);
        while (true){
            mqtt.get();
        }
    }

}
