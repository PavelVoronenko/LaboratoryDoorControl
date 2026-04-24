#include "BLEManager.h"
#include <Arduino.h>

// ------------------ Глобальные переменные BLE ------------------
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
BLECharacteristic* Terminal = NULL;
BLEClient *pClient = NULL;
BLERemoteCharacteristic* pRemoteCharacteristic = NULL;
BLEScan* pBLEScan;

// --- Логирование истории ---
const int MAX_LOG_HISTORY = 500;
String logHistory[MAX_LOG_HISTORY];
int logHead = 0;
int logCount = 0;

bool connected = false;
String rxValue = "";
bool jdeConnect = false;

DevicesDetected devicesDetected[10];

// ------------------ Обработчики BLE ------------------
class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    pServer->startAdvertising();
    //log("Клиент подключён", LOG_INFO);
  }
  void onDisconnect(BLEServer* pServer) {
    pServer->startAdvertising();
    //log("Клиент отключён", LOG_INFO);
  }
};

class CharacteristicCallBack : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pChr) override {
    rxValue = pChr->getValue();
  }
};

//-----------------BLE терминал -----------------------------
void addToLogHistory(String message) {
  logHistory[logHead] = message;
  logHead = (logHead + 1) % MAX_LOG_HISTORY;
  if (logCount < MAX_LOG_HISTORY) logCount++;
}

void log(String message, LogType type) {
  String prefix = "[I] ";
  if (type == LOG_DOOR) prefix = "[D] ";
  else if (type == LOG_USER) prefix = "[U] ";
  else if (type == LOG_WARN) prefix = "[W] ";

  String fullMessage = prefix + message;

  Terminal->setValue((uint8_t*)fullMessage.c_str(), fullMessage.length());
  Terminal->notify();
  Serial.println(fullMessage);
  addToLogHistory(fullMessage);
}

void sendLogHistoryChunked() {
  //Serial.println("Starting fast log history sync...");

  // Начинаем с самого последнего (нового) лога
  int newestIndex = (logHead - 1 + MAX_LOG_HISTORY) % MAX_LOG_HISTORY;

  String buffer = "";
  const int MAX_PACKET_SIZE = 450; // Оставляем запас для заголовка

  for (int i = 0; i < logCount; i++) {
    int currentIndex = (newestIndex - i + MAX_LOG_HISTORY) % MAX_LOG_HISTORY;
    String logEntry = logHistory[currentIndex];

    // Если добавление следующего лога превысит размер пакета
    if (buffer.length() + logEntry.length() + 15 > MAX_PACKET_SIZE) {
      // Отправляем текущий буфер
      String packet = "LOG_HIST:BATCH|" + buffer;
      Terminal->setValue((uint8_t*)packet.c_str(), packet.length());
      Terminal->notify();
      delay(15); // Задержка для надежной передачи пакета
      buffer = "";
    }

    if (buffer.length() > 0) buffer += "\n";
    buffer += logEntry;
  }

  // Отправляем остаток
  if (buffer.length() > 0) {
    String packet = "LOG_HIST:BATCH|" + buffer;
    Terminal->setValue((uint8_t*)packet.c_str(), packet.length());
    Terminal->notify();
    delay(1);
  }

  String endPacket = "LOG_HIST:END";
  Terminal->setValue((uint8_t*)endPacket.c_str(), endPacket.length());
  Terminal->notify();
  //Serial.println("Fast log history sync complete.");
}

// ------------------ Инициализация BLE сервера ------------------
void updateAdvertising() {
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();

  // Останавливаем рекламу перед обновлением данных
  pAdvertising->stop();

  // Основной пакет: только имя и флаги (чтобы точно влезло в 31 байт)
  BLEAdvertisementData advData;
  advData.setName("DEV Laboratory");
  advData.setFlags(0x06);
  pAdvertising->setAdvertisementData(advData);

  // Пакет Scan Response: здесь передаем данные о состоянии
  BLEAdvertisementData resData;
  resData.setCompleteServices(BLEUUID(SERVICE_UUID));

  uint8_t status = (lightStatus == "LIGHTSTATUS:1") ? 1 : 0;
  uint8_t jdeStatus = jdeConnect ? 1 : 0;
  uint8_t sData[2] = {status, jdeStatus};

  // Используем 16-битный UUID FFE0 для Service Data (экономит много места)
  resData.setServiceData(BLEUUID((uint16_t)0xFFE0), String((char*)sData, 2));

  pAdvertising->setScanResponseData(resData);

  // Устанавливаем агрессивный интервал рекламы (20мс - 40мс)
  // Это позволит телефонам мгновенно "слышать" изменения
  pAdvertising->setMinInterval(0x20); // 20ms * 0.625
  pAdvertising->setMaxInterval(0x40); // 40ms * 0.625

  pAdvertising->start();
  Serial.println("📢 Adv Updated: Light=" + String(status) + ", JDE=" + String(jdeStatus));
}

void initBLEServer() {
  BLEDevice::init("Laboratory");

  // Устанавливаем максимальную мощность передатчика (+9 dBm)
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_DEFAULT, ESP_PWR_LVL_P9);
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_ADV, ESP_PWR_LVL_P9);
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_SCAN, ESP_PWR_LVL_P9);

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

  BLE2902 *terminalBLE2902 = new BLE2902();
  terminalBLE2902->setNotifications(true);
  Terminal->addDescriptor(terminalBLE2902);
  Terminal->setCallbacks(new CharacteristicCallBack());
  pService->start();

  // Запускаем рекламу с актуальными данными
  updateAdvertising();

  // Инициализация сканера
  pBLEScan = BLEDevice::getScan();
  pBLEScan->setActiveScan(true);
  pBLEScan->setInterval(100); // Интервал сканирования (мс)
  pBLEScan->setWindow(99);    // Окно сканирования (мс) - почти непрерывное прослушивание
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
        log("Соединение с JDE-33 установлено", LOG_INFO);
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
  int rssi;

  for (int i = 0; i < 10; i++) {
    devicesDetected[i].name = "";
    devicesDetected[i].rssi = 0;
  }

  for (int i = 0; i < results->getCount(); i++) {
    BLEAdvertisedDevice device = results->getDevice(i);
    String mac = device.getAddress().toString().c_str();
    rssi = device.getRSSI();

    // Нормализуем MAC-адрес
    mac.replace(":", "");
    mac.toUpperCase();

    // Проверка по MAC для всех устройств
    for (int j = 0; j < trustedDevicesCount; j++) {
      if (mac.equalsIgnoreCase(trustedDevices[j].macAddress)) {
        if (rssi >= trustedDevices[j].rssiThreshold) {
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
  // Формат: LIGHTSTATUS:0|JDE:1|ID1-0|ID2-1|...
  String sendData = lightStatus + "|JDE:" + String(jdeConnect ? "1" : "0") + "|";

  for (int i = 0; i < trustedDevicesCount; i++) {
    sendData += ("ID" + String(trustedDevices[i].id) + "-" + String(trustedDevices[i].location == "inside" ? 1 : 0) + "|");
  }
  pCharacteristic->setValue(sendData.c_str());
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
