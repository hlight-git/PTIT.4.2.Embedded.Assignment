package com.example.iot_project.connector;

import com.example.iot_project.entity.StatusIot;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherForecastingModelConnector {
    private final String API_URL = "http://127.0.0.1:5000/iot/weather/forecast";
    private StatusIot cageState;

    public WeatherForecastingModelConnector(StatusIot cageState) {
        this.cageState = cageState;
    }

    public boolean predict1() throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        String jsonInputString = json();
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println(response.toString());
            if (response.toString().charAt(14) == '0') {
                return false;
            }
            return true;
        }
    }

    public boolean predict2() throws IOException {
        CloseableHttpResponse response = postJson();
        if (new String(response.getEntity().getContent().readAllBytes()).charAt(14) == '0') {
            return false;
        }
        return true;
    }

    public CloseableHttpResponse postJson() throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(API_URL);
        StringEntity entity = new StringEntity(json(), ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        CloseableHttpResponse response = client.execute(httpPost);
        client.close();
        return response;
    }

    public String json() {
        return String.format(
                "{\"temperature\": %f, \"humidity\": %f, \"wind_speed\": %f}",
                cageState.getTemperature(),
                cageState.getHumidity(),
                cageState.getWindSpeed()
        );
    }
}