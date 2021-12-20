/*
 * muhammad suhendri
 */
//library servo
#include <Servo.h>

// Library wifi
#include <EasyNTPClient.h>
//#include <FS.h> 
#include <Ticker.h>
#include <ESP8266WiFi.h>
#include <DNSServer.h>
#include <ESP8266WebServer.h>
#include <WiFiManager.h>
//Libray Jsonfile
#include <ArduinoJson.h>
#include <PubSubClient.h>
#include "config.h"
char payloadTemp[200];
float TempReading;
//int soil_sensor= A0;
//int powerPin = 15;
int Status;

Servo servo;
#define led 4

WiFiUDP udp;
EasyNTPClient ntpClient(udp, "pool.ntp.org", 25200); 
StaticJsonBuffer<200> jsonBuffer;
//var untuk userID
String commandMessage1 = "1"; //message pesan subscribe
String commandMessage2 = "2"; //message pesan publish
String typeDevice = "Soil Sensor";
char routingkey1[40] = "deteksimasker";

void setup_wifi() {
  WiFi.macAddress(MAC_array);
  for (int i = 0; i < sizeof(MAC_array) - 1; ++i) {
    sprintf(MAC_char, "%s%02x:", MAC_char, MAC_array[i]);
  }
  sprintf(MAC_char, "%s%02x", MAC_char, MAC_array[sizeof(MAC_array) - 1]);
  WiFiManagerParameter custom_mqtt_server("server", "mqtt server", mqtt_server, 40);
  String(mqtt_port).toCharArray(smqtt_port, 5);
  WiFiManagerParameter custom_mqtt_port("port", "mqtt port", smqtt_port, 5);
  WiFiManagerParameter custom_mqtt_user("user", "mqtt user", mqtt_user, 40);
  WiFiManagerParameter custom_mqtt_password("password", "mqtt password", mqtt_password, 40);
  WiFiManagerParameter custom_mqtt_keywords1("keyword1", "mqtt keyword1", MAC_char, 40);
  
  WiFiManager wifiManager;
  wifiManager.setSaveConfigCallback(saveConfigCallback);
  wifiManager.addParameter( & custom_mqtt_server);
  wifiManager.addParameter( & custom_mqtt_port);
  wifiManager.addParameter( & custom_mqtt_user);
  wifiManager.addParameter( & custom_mqtt_password);
  wifiManager.addParameter(&custom_mqtt_keywords1);

  //fetches ssid and pass and tries to connect
  //if it does not connect it starts an access point with the specified name
  //here  "AutoCon nectAP"
  //and goes into a blocking loop awaiting configuration
  
  if (!wifiManager.autoConnect(MAC_char, "password")) {
    Serial.println("failed to connect and hit timeout");
    delay(2000);
    //reset and try again, or maybe put it to deep sleep

    delay(2000);
  }
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

//calback
void callback(char * topic, byte * payload, unsigned int length) {
  char message [7] ;
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.println("] ");
  for (int i = 0; i < length; i++) {
    message[i] = (char)payload[i];
  }
  String convertMsg = String(message) ;
  String data = convertMsg.substring(5);
  int timer = data.toInt();

  //palang pintu
  if (message[0] == '0') {
      servo.write(0);
     digitalWrite(led, HIGH);
     delay(1000);
     digitalWrite(led, LOW);
     delay(1000);
     digitalWrite(led, HIGH);
     delay(1000);
     digitalWrite(led, LOW);
    
  } else if (message[0] == '1') {
    palang();  
  }
 }
 
void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...%s");
    Serial.println(mqtt_server);
    // Attempt to connect
    if (client.connect(MAC_char, mqtt_user, mqtt_password)) {
      Serial.println("connected");
      Serial.println(MAC_char);
      client.subscribe(MAC_char);

    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      if (client.state() == 4) ESP.restart();
      else {
        Serial.println(" try again in 5 seconds");
        // Wait 5 seconds before retrying
        
      }
    }
  }
}

void setup() {
  //setup pin mode
  servo.attach(2); //D1
  servo.write(0);
  pinMode(led,OUTPUT);
  Serial.begin(9600);
  Serial.println(F("Booting...."));
  
  //read config wifi,mqtt dan yang lain
  ReadConfigFile();
  setup_wifi();
  SaveConfigFile();
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
  delay(1000);
  
}

void palang(){
  
  servo.write(180);
  digitalWrite(led,HIGH);
  delay(4000);
  
  servo.write(0);
  digitalWrite(led,LOW);
}
