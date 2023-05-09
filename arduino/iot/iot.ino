#include <DHT.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

//DHT Settings
#define DHTPIN 4
#define DHTTYPE DHT11
float temperature, humidity;

DHT dht(DHTPIN, DHTTYPE);

// Wi-Fi Settings
const char* ssid = "Chinh xinh dep tuyet tran";
const char* password = "0368674425.";

// Broker Settings
#define MQTT_SERVER "broker.mqttdashboard.com"
#define MQTT_PORT 1883
#define MQTT_USER "hlight"
#define MQTT_PASSWORD "hlight"
#define MQTT_CLIENTID "hlight"
#define TOPIC_PUBLISH "hlight/PTIT/IOT/state"
#define TOPIC_SUBSCRIBE "hlight/PTIT/IOT/command"

WiFiClient esp_client;
void callback(char* topic, byte* payload, unsigned int length);
PubSubClient mqtt_client(MQTT_SERVER, MQTT_PORT, callback, esp_client);

// Data Sending Time
unsigned long CurrentMillis, PreviousMillis, DataSendingTime = (unsigned long) 1000 * 30;

// Variable
int curFanLevel = 0;
float curHeaterTemp = 20;
int curPowerState = 0;

void connect_wifi() {
  Serial.print("Connecting");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(1000);
  }
  Serial.println("\r\nWiFi connected.");
}

void mqtt_publish(char * data) {
  if (mqtt_client.publish(TOPIC_PUBLISH, data))
    Serial.println("Publish \"" + String(data) + "\" ok");
  else
    Serial.println("Publish \"" + String(data) + "\" failed");
}
void mqtt_subscribe(const char * topic) {
  if (mqtt_client.subscribe(topic))
    Serial.println("Subscribe \"" + String(topic) + "\" ok");
  else
    Serial.println("Subscribe \"" + String(topic) + "\" failed");
}

void mqtt_connect() {
  while (!mqtt_client.connected()) {
    Serial.println("Attempting MQTT connection...");
    // Attempt to connect
    if (mqtt_client.connect(MQTT_CLIENTID, MQTT_USER, MQTT_PASSWORD)) {
    // if (mqtt_client.connect(MQTT_CLIENTID)) {
      Serial.println("MQTT Client Connected");
      mqtt_subscribe(TOPIC_SUBSCRIBE);
    } else {
      Serial.print("failed, rc=");
      Serial.print(mqtt_client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void unzip_command(String command){
  DynamicJsonDocument doc(1024);
  deserializeJson(doc, command);
  JsonObject obj = doc.as<JsonObject>();
  int newFanLevel = ((String) obj[String("fan_level")]).toInt();
  float newHeaterTemp = ((String) obj[String("heater_temperature")]).toFloat();
  int newPowerState = ((String) obj[String("power")]).toInt();
  Serial.printf("Setted fan level from %i to %i.\n", curFanLevel, newFanLevel);
  Serial.printf("Setted heater temperature from %f to %f.\n", curHeaterTemp, newHeaterTemp);
  Serial.printf("Setted power state from %i to %i.\n", curPowerState, newPowerState);
  curFanLevel = newFanLevel;
  curHeaterTemp = newHeaterTemp;
  curPowerState = newPowerState;
  if (curPowerState == 0){
    digitalWrite(LED_BUILTIN, HIGH);
  } else {
    digitalWrite(LED_BUILTIN, LOW);
  }
  sendDataToBroker();
}

void callback(char* topic, byte* payload, unsigned int length) {
  String command;
  Serial.printf("\n\n*****Received command from [%s]:\n", topic);
  // Bytes to string
  for (int i = 0; i < length; i++)
    command += (char)payload[i];
  if (command.length() > 0)
    unzip_command(command);
}

bool readDataFromDHT11() {
  temperature = dht.readTemperature();
  humidity = dht.readHumidity();
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println(F("Failed to read from DHT sensor!"));
    return false;
  }
  return true;
}

void sendDataToBroker(){
  if(readDataFromDHT11()){
    delay(1000);
    // Devices State Sync Request
    CurrentMillis = millis();
    if (CurrentMillis - PreviousMillis > DataSendingTime) {
      PreviousMillis = CurrentMillis;
      // Publish Temperature Data
      String data = String(temperature) + "|" + String(humidity) + "|" + String(curPowerState);
      mqtt_publish((char*) data.c_str());
    }
  }
}

void setup() {
  Serial.begin(115200);
  delay(5000);
  pinMode(LED_BUILTIN, OUTPUT);
  connect_wifi();
  mqtt_connect();
  dht.begin();
}

void loop() {
  sendDataToBroker();
  if (!mqtt_client.loop())
    mqtt_connect();
}
