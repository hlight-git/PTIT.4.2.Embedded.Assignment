package com.example.iot_project.controller;

import com.example.iot_project.connector.Mqtt;
import com.example.iot_project.entity.ControlState;
import com.example.iot_project.entity.StatusIot;
import com.example.iot_project.repository.ControlStateRepository;
import com.example.iot_project.repository.StatusIotRepository;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class ServerController {
    private final ControlStateRepository controlStateRepository;
    private final StatusIotRepository statusIotRepository;
    private final Mqtt mqtt;

    @Autowired
    public ServerController(ControlStateRepository controlStateRepository, StatusIotRepository statusIotRepository) throws MqttException {
        this.controlStateRepository = controlStateRepository;
        this.statusIotRepository = statusIotRepository;
        mqtt = new Mqtt(statusIotRepository);
    }

    @GetMapping("/home")
    public String showHomeForm(Model model) {
        List<StatusIot> statusIots = statusIotRepository.getAllOrderByIdDesc();
        model.addAttribute("statusIotList", statusIots);
        return "/home";
    }
    @GetMapping("/chicken_cage_control")
    public String showControlView(Model model){
        model.addAttribute("state", controlStateRepository.getAllOrderByIdDesc().get(0));
        return "/chicken_cage_control";
    }
    @PostMapping("/state")
    public String publishControl(@ModelAttribute("state") ControlState state){
        try {
            controlStateRepository.save(state);
            mqtt.post(
                    state.getFan(),
                    state.getHeater(),
                    state.getPower()
            );
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
        return "redirect:/chicken_cage_control";
    }
}
