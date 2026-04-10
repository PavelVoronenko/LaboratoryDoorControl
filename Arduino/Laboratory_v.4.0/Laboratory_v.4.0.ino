#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <BLEScan.h>
#include <BLEClient.h>
#include <Preferences.h>

// ------------------------------- Пины ---------------------------------
#define OPENING_PIN 32
#define SPEAKER_PIN 22
#define SENSOR_PIN 23

// ------------------------- BLE параметры ------------------------------
#define SCAN_TIME 1 // 1 сек
#define SERVICE_UUID "0000ffe0-0000-1000-8000-00805f9b34fb"  //4fafc201-1fb5-459e-8fcc-c5c9c331914b
#define CHAR1_UUID   "beb5483e-36e1-4688-b7f5-ea07361b26a8"  // beb5483e-36e1-4688-b7f5-ea07361b26a8
#define CHAR2_UUID_TERMINAL   "e3223119-9445-4e96-a4a1-85358c4046a2"  //e3223119-9445-4e96-a4a1-85358c4046a2
#define JDYCHAR_UUID "0000ffe1-0000-1000-8000-00805f9b34fb"
#define TARGET_NAME "JDY-33-BLE" // Название устройства, по которому ищем

// ------------- Хранилище пользователей (до 10 устройств) --------------
Preferences usersPrefs;
const char* NVS_NAMESPACE = "trusted_users";
const int MAX_USERS = 10;

// -------------- Структура для хранения в NVS (минимальная) ------------
struct TrustedDeviceNVS {
  int id;                    // Уникальный ID (ключ)
  char name[64];             // Фиксированный размер для NVS
  char uuid[37];             // UUID формата: 00000000-0000-0000-0000-000000000000
  char serviceDataHex[32];   // Service data
  char macAddress[18];       // MAC: XX:XX:XX:XX:XX:XX
};

// ------------------ Структура доверенного устройства ------------------
struct TrustedDevice {
  int id;
  String name;
  String uuid;
  String serviceDataHex;
  String macAddress;
  String location; // "inside" или "outside"
  int rssiThreshold;  // порог RSSI для открытия
  uint64_t userTime; 
  bool entryInProgress;
  bool exitInProgress;   
};

// ------------------ Структура обнаруженных устройств ------------------
struct DevicesDetected { 
  String name; 
  int rssi;
};

// ------------------ Массив доверенных устройств -----------------------
TrustedDevice trustedDevices[MAX_USERS];
int trustedDevicesCount = 0;

// -------------------- Массив обнаруженных устройств -------------------
DevicesDetected devicesDetected[10];

// ------------------ Объявление глобальных переменных ------------------
bool connected = false;
String rxValue = "";
bool jdeConnect = false;
String lightStatus = "LIGHTSTATUS:0";
bool autoLightOutside = true;

// ----------------- Переменные таймеров --------------------
unsigned long doorOpenTimestamp = 0;
unsigned long SendCommandTime = 0;

// --------------------- Многозадачность --------------------
TaskHandle_t Task1;
TaskHandle_t Task2;
// ------------------------- BLE ----------------------------
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
BLECharacteristic* Terminal = NULL;
BLEDescriptor *pDescr;
BLE2902 *pBLE2902;
BLEScan* pBLEScan;
BLEClient *pClient = NULL;
BLERemoteCharacteristic* pRemoteCharacteristic = NULL;  //BLERemoteCharacteristic*

//-----------------BLE терминал -----------------------------
void log (String message){
  Terminal->setValue("" + message);
  Terminal->notify();
  Serial.println(message);
}

// ------------------ Обработчики ------------------
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

// ------------------  Загрузка всех пользователей из NVS в runtime-массив ------------------ 
int loadTrustedDevices(TrustedDevice* outArray, int maxSize) {
  int count = 0;
  
  Serial.println("=== NVS Load Debug ===");
  Serial.printf("Namespace: %s, MaxUsers: %d\n", NVS_NAMESPACE, maxSize);
  
  // 🔥 Проверяем диапазон ID (0..99), не прерываемся при пропусках
  for (int potentialId = 0; potentialId < 100 && count < maxSize; potentialId++) {
    String key = "user_" + String(potentialId);
    
    if (!usersPrefs.isKey(key.c_str())) {
      // Ключа нет, но это не конец списка. Идём дальше.
      continue; 
    }
    
    Serial.printf("Found key: %s ... ", key.c_str());
    
    TrustedDeviceNVS temp;
    size_t expectedSize = sizeof(TrustedDeviceNVS);
    size_t len = usersPrefs.getBytes(key.c_str(), &temp, expectedSize);
    
    if (len == expectedSize) {
      outArray[count].id = temp.id;
      outArray[count].name = String(temp.name);
      outArray[count].uuid = String(temp.uuid);
      outArray[count].serviceDataHex = String(temp.serviceDataHex);
      outArray[count].macAddress = String(temp.macAddress);
      
      // Runtime-поля
      outArray[count].location = "outside";
      outArray[count].rssiThreshold = -70;
      outArray[count].userTime = 0;
      outArray[count].entryInProgress = false;
      outArray[count].exitInProgress = false;
      
      Serial.printf("OK. Loaded: %s\n", temp.name);
      count++;
    } else {
      Serial.println("SIZE MISMATCH! Removing corrupt key.");
      usersPrefs.remove(key.c_str());
    }
  }
  
  Serial.printf("=== Load Complete: %d devices ===\n", count);
  return count;
}

// ------------------  Сохранение одного пользователя в NVS ------------------ 
bool saveTrustedDevice(const TrustedDeviceNVS* device) {
  String key = "user_" + String(device->id);
  size_t size = sizeof(TrustedDeviceNVS);
  
  Serial.printf("=== NVS Save Debug ===\n");
  Serial.printf("Key: %s, Size: %d bytes\n", key.c_str(), size);
  Serial.printf("Data: id=%d, name=%s, uuid=%s\n", 
                device->id, device->name, device->uuid);
  
  bool result = usersPrefs.putBytes(key.c_str(), device, size);
  Serial.printf("putBytes result: %s\n", result ? "SUCCESS" : "FAILED");
  
  if (result) {
    // Принудительно коммитим изменения
    usersPrefs.end();
    usersPrefs.begin(NVS_NAMESPACE, false);
    Serial.println("NVS committed");
  }
  
  return result;
}

// ------------------  Удаление пользователя по ID ------------------ 
bool deleteTrustedDevice(int id) {
  String key = "user_" + String(id);
  return usersPrefs.remove(key.c_str());
}

// ------------------  Поиск устройства по MAC (для быстрой проверки при сканировании) ------------------ 
int findDeviceByMAC(const TrustedDevice* array, int count, const String& mac) {
  for (int i = 0; i < count; i++) {
    if (array[i].macAddress.equalsIgnoreCase(mac)) {
      return i;
    }
  }
  return -1;
}

// ------------------  Поиск устройства по UUID + ServiceData ------------------ 
int findDeviceByUUID(const TrustedDevice* array, int count, const String& uuid, const String& serviceData) {
  for (int i = 0; i < count; i++) {
    if (array[i].uuid.equalsIgnoreCase(uuid) && 
        array[i].serviceDataHex.equalsIgnoreCase(serviceData)) {
      return i;
    }
  }
  return -1;
}

// ------------------ Setup ------------------
void setup() {
  Serial.begin(115200);
  
  // Устанавливаем частоту CPU (например, 80 МГц)
  setCpuFrequencyMhz(80);

  // 🔥 Инициализация NVS
  usersPrefs.begin(NVS_NAMESPACE, false); // false = read/write
  
  // 🔥 Загрузка пользователей из памяти
  trustedDevicesCount = loadTrustedDevices(trustedDevices, MAX_USERS);
  Serial.printf("Загружено %d доверенных устройств из NVS\n", trustedDevicesCount);

  pinMode(OPENING_PIN, OUTPUT);
  pinMode(SPEAKER_PIN, OUTPUT);
  pinMode(SENSOR_PIN, INPUT_PULLUP);
  
  // Изначально дверь закрыта
  digitalWrite(OPENING_PIN, LOW);
  ledcAttach(SPEAKER_PIN, 4, 8);

  // Инициализация BLE
  BLEDevice::init("DEV Laboratory");
  BLEDevice::setMTU(512);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  BLESecurity *pSecurity = new BLESecurity();
  pSecurity->setAuthenticationMode(ESP_LE_AUTH_REQ_SC_MITM_BOND);
  //pSecurity->setStaticPIN(987001); // ваш PIN-код

  // Создаем сервис
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(CHAR1_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_NOTIFY);
  Terminal = pService->createCharacteristic(CHAR2_UUID_TERMINAL, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_NOTIFY);  
  
  pDescr = new BLEDescriptor((uint16_t)0x2901);
  pDescr->setValue("A very interesting variable");
  pCharacteristic->addDescriptor(pDescr);
  pBLE2902 = new BLE2902();
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

  // Многозадачность
  xTaskCreatePinnedToCore(Task1code, "Task1", 26000, NULL, 0, &Task1, 0);               
    delay(500); 
  xTaskCreatePinnedToCore(Task2code, "Task2", 26000, NULL, 1, &Task2, 1);
    delay(500); 

  // Попытка подключиться к JDY-33
  if (scanAndConnect()) {
    Serial.println("Подключение к JDY-33 успешно");
    jdeConnect = true;
    connected = true;
  } else {
    Serial.println("Подключение к JDY-33 не удалось");
    jdeConnect = false;
  }
}

//-----------------------------------First Core------------------------------------------------//
void Task1code( void * pvParameters ){

  for(;;){
    // Проверка BLE устройств (для доверенных устройств)
    scanForTrustedDevices();

    // Отправка служебных данных
    if (millis() -  SendCommandTime > 1000){
      SendCommandTime = millis();
      //sendCommand();
    }

    // Попытка восстановить соединение с JDY-33
    if (connected && !pClient->isConnected()) {
      Serial.println("Соединение потеряно, повторное сканирование...");
      if (scanAndConnect()) {
        Serial.println("Повторное подключение успешно");
        jdeConnect = true;
      } else {
        Serial.println("Не удалось повторно подключиться");
        jdeConnect = false;
      }
    }
  }
}  

//--------------------------------------Second core--------------------------------------------//
void Task2code( void * pvParameters ){
 
  for(;;){

    // Обработчик команд приложения
    commandHandler(); 

    // Закрытие двери
    closedDoor();

    // Проверка статуса выхода
    checkEntryExitStatus();

    // Автоматическое отключение освещения
    checkPeopleInside();
  
    // Обработка датчика движения
    if (millis() -  0 > 30000){
      if (digitalRead(SENSOR_PIN) == LOW) {
        openDoor(10, "|SENSOR|");
      }
    }

    // Обнуление процесса входа/выхода
    for (int i = 0; i < trustedDevicesCount; i++) {
      if (millis() - trustedDevices[i].userTime > 10000) {
        if(trustedDevices[i].entryInProgress || trustedDevices[i].exitInProgress) {
          trustedDevices[i].entryInProgress = false;
          trustedDevices[i].exitInProgress = false;
          log(trustedDevices[i].name + " не закончил процесс входа/выхода");
        }
      }
    }
  }
}

// ------------------------- Основной цикл ---------------------------
void loop() { 
    vTaskDelay(10000);
}

//---------------Обновление статуса доверенных устройств--------------
void updateDeviceStatus(String name, String newStatus) {
  for (int i = 0; i < trustedDevicesCount; i++) {
    if (trustedDevices[i].name == name) {
      trustedDevices[i].location = newStatus;
      trustedDevices[i].userTime = millis();   //
      log(trustedDevices[i].name + " теперь " + newStatus);
    }
  }
}

// ----------------- Проверка на вход и выход -----------------------
void processExitAndEntry(DevicesDetected *devDetected) {
  for (int i = 0; i < 10; i++) {
    for (int j = 0; j < trustedDevicesCount; j++){
      if(devDetected[i].name == trustedDevices[j].name) {
        if (millis() - trustedDevices[j].userTime > 11000) {
          trustedDevices[j].userTime = millis();
          
          if(trustedDevices[j].location == "outside") {
            openDoor(8, "|BLE| ");
            trustedDevices[j].entryInProgress = true;
            log(devicesDetected[i].name + " начинает вход");
          }

          if(trustedDevices[j].location == "inside") {
            trustedDevices[j].exitInProgress = true;
            log(devicesDetected[i].name + " начинает выход");
          }
        }
      }
    }
  }
}

void checkEntryExitStatus() {
  if (digitalRead(SENSOR_PIN) == LOW) {
      for (int j = 0; j < trustedDevicesCount; j++){       
        if(trustedDevices[j].entryInProgress && trustedDevices[j].location == "outside") {
          trustedDevices[j].entryInProgress = false; 
          updateDeviceStatus(trustedDevices[j].name, "inside");
        } else if(trustedDevices[j].exitInProgress && trustedDevices[j].location == "inside") {
          trustedDevices[j].exitInProgress = false;
          updateDeviceStatus(trustedDevices[j].name, "outside");
        }
      }
  }
}

//------------------ Проверка доверенных устройств ------------------
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

// ---------------------------- Открытие и закрытие -----------------------------
void openDoor(int pause, String source) {
  if (millis() -  doorOpenTimestamp > pause*1000){
    doorOpenTimestamp = millis();
    ledcWrite(SPEAKER_PIN, 150);
    digitalWrite(OPENING_PIN, HIGH);
    log(source + " Дверь открыта");
  }
}

void closedDoor() {
  if (digitalRead(OPENING_PIN) == HIGH){
    if (millis() -  doorOpenTimestamp > 3000){
      ledcWrite(SPEAKER_PIN, 0);
      digitalWrite(OPENING_PIN, LOW);
      log("|SYSTEM| Дверь закрыта");
    }
  }
}

// -------------------------Функция сканирования и подключения к JDY-33 ----------
bool scanAndConnect() {
  BLEScan* pBLEScan = BLEDevice::getScan();
  pBLEScan->setActiveScan(true);
  Serial.println("Начинается сканирование...");
  BLEScanResults* results = pBLEScan->start(1); // сканировать 1 секунд

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
        pRemoteCharacteristic = pRemoteService->getCharacteristic(JDYCHAR_UUID);  //pRemoteService->getCharacteristic(JDYCHAR_UUID)
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

// ----------------------- Парсинг команды ADDUSER и сохранение в NVS -----------------------
void addNewUserFromCommand(String params) {
  // Формат: id|name|uuid|serviceData|mac|threshold
  // Пример: 3|Анна|0000ff33-0000-1000-8000-00805f9b34fb|Abc123Xy|AA:BB:CC:DD:EE:FF|-65
  
  int delimiterIndex[6];
  int count = 0;
  int start = 0;
  
  // Парсим разделители '|'
  for (int i = 0; i < params.length() && count < 5; i++) {
    if (params[i] == '|') {
      delimiterIndex[count++] = i;
    }
  }
  
  if (count < 5) {
    log("Ошибка: неверный формат ADDUSER");
    return;
  }
  
  // Извлекаем поля
  int id = params.substring(0, delimiterIndex[0]).toInt();
  String name = params.substring(delimiterIndex[0]+1, delimiterIndex[1]);
  String uuid = params.substring(delimiterIndex[1]+1, delimiterIndex[2]);
  String serviceData = params.substring(delimiterIndex[2]+1, delimiterIndex[3]);
  String mac = params.substring(delimiterIndex[3]+1, delimiterIndex[4]);
  int rssiThreshold = params.substring(delimiterIndex[4]+1).toInt();
  
  // Проверяем лимит
  if (trustedDevicesCount >= MAX_USERS) {
    log("Ошибка: достигнут лимит пользователей (" + String(MAX_USERS) + ")");
    return;
  }
  
  // Создаём структуру для NVS
  TrustedDeviceNVS newUser;
  newUser.id = id;
  name.toCharArray(newUser.name, sizeof(newUser.name));
  uuid.toCharArray(newUser.uuid, sizeof(newUser.uuid));
  serviceData.toCharArray(newUser.serviceDataHex, sizeof(newUser.serviceDataHex));
  mac.toCharArray(newUser.macAddress, sizeof(newUser.macAddress));
  
  // Сохраняем в NVS
  if (saveTrustedDevice(&newUser)) {
    // Перезагружаем runtime-массив
    trustedDevicesCount = loadTrustedDevices(trustedDevices, MAX_USERS);
    
    // Устанавливаем порог для нового пользователя
    int idx = findDeviceByMAC(trustedDevices, trustedDevicesCount, mac);
    if (idx >= 0) {
      trustedDevices[idx].rssiThreshold = rssiThreshold;
    }
    
    log("Пользователь '" + name + "' добавлен (ID:" + String(id) + ")");
  } else {
    log("Ошибка сохранения в NVS");
  }
}

// ----------------------- Отправка списка пользователей в приложение -----------------------
void sendUserListChunked() {
  const int MAX_CHUNK_PAYLOAD = 120;  // ✅ УМЕНЬШЕНО с 170 до 120 для надёжности!
  
  // === 1. Формируем полный список ===
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
    
    // Отправляем специальный пакет: USERLIST_PKT:0/1|END
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

  // === 3. Отправляем чанками ===
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

    // ✅ Если не нашли разделитель — берём всё до конца
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
    
    // ✅ Отправляем через BLE
    pCharacteristic->setValue((uint8_t*)packet.c_str(), packet.length());
    pCharacteristic->notify();

    position = cutPos;
    chunkIdx++;
  }

  Serial.println("✅ All " + String(chunkIdx) + " chunks sent");
  Serial.println("📊 Sent " + String(position) + " / " + String(fullList.length()) + " bytes");
}

// ----------------------Обработчик команд из приложения ----------------
void commandHandler () {
  if (rxValue.length() > 0) {
    String cmd = String(rxValue.c_str());
    //log("Получена команда: " + cmd);
    
    if (cmd.equalsIgnoreCase("OPENDOOR")) {
      openDoor(0, "|APP|");
    }

    if (cmd.equalsIgnoreCase("LIGHTON")) {
      lightSwitches ("lightON", "Освещение включено");
    }

    if (cmd.equalsIgnoreCase("LIGHTOFF")) {
      lightSwitches ("lightOFF", "Освещение отключено");
    }

    if (cmd.equalsIgnoreCase("PASHAOUTSIDE")) {
      trustedDevices[0].location = "outside";
      log("Паша сменил статус на outside");
    }

    if (cmd.equalsIgnoreCase("PASHAINSIDE")) {
      trustedDevices[0].location = "inside";
      log("Паша сменил статус на inside");
    }

    if (cmd.equalsIgnoreCase("SLAVAOUTSIDE")) {
      trustedDevices[1].location = "outside";
      log("Слава сменил статус на outside");
    }

    if (cmd.equalsIgnoreCase("SLAVAINSIDE")) {
      trustedDevices[1].location = "inside";
      log("Слава сменил статус на inside");
    }

    if (cmd.equalsIgnoreCase("VOLODIAOUTSIDE")) {
      trustedDevices[2].location = "outside";
      log("Володя сменил статус на outside");
    }

    if (cmd.equalsIgnoreCase("VOLODIAINSIDE")) {
      trustedDevices[2].location = "inside";
      log("Володя сменил статус на inside");
    }

    // ADDUSER:id|name|uuid|serviceData|mac|threshold
    if (cmd.startsWith("ADDUSER:")) {
      String params = cmd.substring(8); 
      addNewUserFromCommand(params);
    }
    
    // DELUSER:id
    if (cmd.startsWith("DELUSER:")) {
      int id = cmd.substring(8).toInt();
      if (deleteTrustedDevice(id)) {
        // Перезагружаем массив после удаления
        trustedDevicesCount = loadTrustedDevices(trustedDevices, MAX_USERS);
        log("Пользователь #" + String(id) + " удалён");
      }
    }
    
    // LISTUSERS — отправить список всех пользователей в приложение
    if (cmd.equalsIgnoreCase("LISTUSERS")) {
      sendUserListChunked();
    }

    rxValue = "";
  }
}

// ----------------------- Отправка служебных данных ------------------------------
void sendCommand() {
  String sendCommand = ("" + lightStatus + "|");
  
  for (int i = 0; i < trustedDevicesCount; i++) {
      sendCommand += (trustedDevices[i].name + "-" + trustedDevices[i].location + "|");
  }  
  pCharacteristic->setValue("" + sendCommand);
  pCharacteristic->notify();  
}
// ----------------------- Включение и отключение освещения -------------------------
void lightSwitches(String command, String message) {
  if (jdeConnect && command == "lightON") {
    lightStatus = "LIGHTSTATUS:1";

    uint8_t cmd[] = {0xA0, 0x01, 0x01, 0xA2};
    pRemoteCharacteristic->writeValue(cmd, sizeof(cmd)); //pRemoteCharacteristic
    log(message);
  } 
  else if (jdeConnect && command == "lightOFF") {
    lightStatus = "LIGHTSTATUS:0";

    uint8_t cmd[] = {0xA0, 0x01, 0x00, 0xA1};
    pRemoteCharacteristic->writeValue(cmd, sizeof(cmd)); //pRemoteCharacteristic
    log(message);
  } else log ("JDE-33 не подключен");
}

// ---------- Проверка людей внутри и автоматическое включение освещения ------------
void checkPeopleInside () {
  byte numberOfPeopleInside = 0;

  for (int i = 0; i < trustedDevicesCount; i++) {
    if (trustedDevices[i].location == "inside")
      numberOfPeopleInside++;
  }

  if (lightStatus == "LIGHTSTATUS:0" && numberOfPeopleInside == 1 && !autoLightOutside) {
    autoLightOutside = true;
    lightSwitches ("lightON", "Автоматическое включение освещения");
  } else if (numberOfPeopleInside == 0 && autoLightOutside) {
    autoLightOutside = false;
    lightSwitches ("lightOFF", "Автоматическое отключение освещения");
  }
}
