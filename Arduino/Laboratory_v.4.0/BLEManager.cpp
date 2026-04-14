#include "BLEManager.h"
#include <Arduino.h>

// ------------------ Глобальные переменные BLE ------------------
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
BLECharacteristic* Terminal = NULL;
BLEClient *pClient = NULL;
BLERemoteCharacteristic* pRemoteCharacteristic = NULL;
BLEScan* pBLEScan;

bool connected = false;
String rxValue = "";
bool jdeConnect = false;

DevicesDetected devicesDetected[10];

// ------------------ Обработчики BLE ------------------
class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    pServer->startAdvertising();
    log("Клиент подключен");
  }
  void onDisconnect(BLEServer* pServer) {
    pServer->startAdvertising();
    log("Клиент отключен");
  }
};

class CharacteristicCallBack : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pChr) override {
    rxValue = pChr->getValue();
  }
};

//-----------------BLE терминал -----------------------------
void log(String message) {
  Terminal->setValue("" + message);
  Terminal->notify();
  Serial.println(message);
}

// ------------------ Инициализация BLE сервера ------------------
void initBLEServer() {
  BLEDevice::init("DEV Laboratory");
  BLEDevice::setMTU(512);
  
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  
  BLESecurity *pSecurity = new BLESecurity();
  pSecurity->setAuthenticationMode(ESP_LE_AUTH_REQ_SC_MITM_BOND);

  // Создаем сервис
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(CHAR1_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_NOTIFY);
  Terminal = pService->createCharacteristic(CHAR2_UUID_TERMINAL, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_NOTIFY);

  BLEDescriptor *pDescr = new BLEDescriptor((uint16_t)0x2901);
  pDescr->setValue("A very interesting variable");
  pCharacteristic->addDescriptor(pDescr);
  
  BLE2902 *pBLE2902 = new BLE2902();
  pBLE2902->setNotifications(true);
  pCharacteristic->addDescriptor(pBLE2902);
  
  Terminal->addDescriptor(new BLE2902());
  Terminal->setCallbacks(new CharacteristicCallBack());
  pService->start();

  // Запускаем рекламу
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);
  BLEDevice::startAdvertising();

  // Инициализация сканера
  pBLEScan = BLEDevice::getScan();
  pBLEScan->setActiveScan(true);
}

// -------------------------Функция сканирования и подключения к JDY-33 ----------
bool scanAndConnect() {
  BLEScan* pBLEScan = BLEDevice::getScan();
  pBLEScan->setActiveScan(true);
  Serial.println("Начинается сканирование...");
  BLEScanResults* results = pBLEScan->start(1);

  for (int i = 0; i < results->getCount(); i++) {
    BLEAdvertisedDevice device = results->getDevice(i);

    if (device.getName() == TARGET_NAME || device.getName() == "JDY-33") {
      Serial.println("Найден JDY-33, подключение...");
      pClient = BLEDevice::createClient();
      if (pClient->connect(&device)) {
        Serial.println("Подключено");
        BLERemoteService* pRemoteService = pClient->getService(SERVICE_UUID);
        if (pRemoteService == nullptr) {
          Serial.println("Не найден сервис");
          pClient->disconnect();
          jdeConnect = false;
          return false;
        }
        pRemoteCharacteristic = pRemoteService->getCharacteristic(JDYCHAR_UUID);
        if (pRemoteCharacteristic == nullptr) {
          Serial.println("Не найдена характеристика");
          pClient->disconnect();
          jdeConnect = false;
          return false;
        }
        jdeConnect = true;
        return true;
      } else {
        Serial.println("Не удалось подключиться к устройству");
        jdeConnect = false;
      }
    }
  }
  Serial.println("Устройство не найдено");
  jdeConnect = false;
  return false;
}

// ------------------ Проверка доверенных устройств ------------------
void scanForTrustedDevices() {
  BLEScanResults* results = pBLEScan->start(SCAN_TIME, false);
  int devicesIndex = 0;

  for (int i = 0; i < 10; i++) {
    devicesDetected[i].name = "";
    devicesDetected[i].rssi = 0;
  }

  for (int i = 0; i < results->getCount(); i++) {
    BLEAdvertisedDevice device = results->getDevice(i);
    String mac = device.getAddress().toString().c_str();
    int rssi = device.getRSSI();

    // Нормализуем MAC-адрес
    mac.replace(":", "");
    mac.toUpperCase();

    // Проверка по MAC для всех устройств
    for (int j = 0; j < trustedDevicesCount; j++) {
      if (mac.equalsIgnoreCase(trustedDevices[j].macAddress)) {
        if (rssi >= trustedDevices[j].rssiThreshold) {
          log("Обнаружен: " + trustedDevices[j].name + ", RSSI: " + String(rssi));
          devicesDetected[devicesIndex].name = trustedDevices[j].name;
          devicesDetected[devicesIndex].rssi = rssi;
          devicesIndex++;
        }
      }
    }

    // Проверка по UUID и Service Data
    BLEUUID serviceUUID = device.getServiceUUID();
    if (!serviceUUID.equals(BLEUUID())) {
      String uuidStr = serviceUUID.toString().c_str();
      for (int j = 0; j < trustedDevicesCount; j++) {
        if (uuidStr.equalsIgnoreCase(trustedDevices[j].uuid)) {
          if (device.getServiceData() == trustedDevices[j].serviceDataHex && rssi >= trustedDevices[j].rssiThreshold) {
            log("Обнаружен: " + trustedDevices[j].name + ", RSSI: " + String(rssi));
            devicesDetected[devicesIndex].name = trustedDevices[j].name;
            devicesDetected[devicesIndex].rssi = rssi;
            devicesIndex++;
          }
        }
      }
    }
  }

  if (devicesDetected[0].name != "") {
    processExitAndEntry(devicesDetected);
  }
  pBLEScan->stop();
}

// ----------------------- Отправка служебных данных ------------------------------
void sendCommand() {
  extern String lightStatus;
  String sendCommand = ("" + lightStatus + "|");

  for (int i = 0; i < trustedDevicesCount; i++) {
    sendCommand += ("ID" + String(trustedDevices[i].id) + "-" + String(trustedDevices[i].location == "inside" ? 1 : 0) + "|");
  }
  pCharacteristic->setValue("" + sendCommand);
  pCharacteristic->notify();
}

// ----------------------- Отправка списка пользователей в приложение -----------------------
void sendUserListChunked() {
  const int MAX_CHUNK_PAYLOAD = 120;

  // Формируем полный список
  String fullList = "";
  for (int i = 0; i < trustedDevicesCount; i++) {
    fullList += String(trustedDevices[i].id) + "," +
                trustedDevices[i].name + "," +
                trustedDevices[i].macAddress + "," +
                trustedDevices[i].location + "," +
                trustedDevices[i].uuid + "," +
                trustedDevices[i].serviceDataHex + "|";
  }

  Serial.println("📊 Users in fullList: " + String(trustedDevicesCount));
  Serial.println("📊 FullList length: " + String(fullList.length()) + " bytes");

  if (fullList.isEmpty()) {
    Serial.println("⚠️ User list is empty — sending empty packet");

    String packet = "USERLIST_PKT:0/1|END";
    pCharacteristic->setValue((uint8_t*)packet.c_str(), packet.length());
    pCharacteristic->notify();
    delay(50);
    Serial.println("✅ Empty list sent");
    return;
  }

  Serial.println("=== USERLIST Debug ===");
  Serial.println("Total users: " + String(trustedDevicesCount));
  Serial.println("Full list: " + String(fullList.length()) + " bytes");

  // Отправляем чанками
  size_t position = 0;
  int chunkIdx = 0;

  while (position < fullList.length()) {
    size_t searchStart = position + 30;
    size_t searchEnd = min(position + MAX_CHUNK_PAYLOAD, fullList.length());
    size_t cutPos = searchEnd;

    for (size_t pos = searchStart; pos <= searchEnd && pos < fullList.length(); pos++) {
      if (fullList.charAt(pos) == '|') {
        cutPos = pos + 1;
        break;
      }
    }

    if (cutPos < position) cutPos = fullList.length();

    String chunkData = fullList.substring(position, cutPos);
    bool isLastChunk = (cutPos >= fullList.length());

    String packet = "USERLIST_PKT:" + String(chunkIdx) + "/?" + "|" + chunkData;
    if (isLastChunk) {
      packet += "|END";
      Serial.println("📤 Chunk " + String(chunkIdx) + " [LAST] (" + String(packet.length()) + " bytes)");
    } else {
      Serial.println("📤 Chunk " + String(chunkIdx) + " (" + String(packet.length()) + " bytes)");
    }

    pCharacteristic->setValue((uint8_t*)packet.c_str(), packet.length());
    pCharacteristic->notify();

    position = cutPos;
    chunkIdx++;
  }

  Serial.println("✅ All " + String(chunkIdx) + " chunks sent");
  Serial.println("📊 Sent " + String(position) + " / " + String(fullList.length()) + " bytes");
}
