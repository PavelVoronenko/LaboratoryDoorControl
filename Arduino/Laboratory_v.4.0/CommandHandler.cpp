#include "CommandHandler.h"
#include "DeviceStorage.h"
#include "BLEManager.h"
#include "DoorControl.h"
#include "Utils.h"
#include "OTAManager.h"

// ----------------------- Парсинг команды ADDUSER и сохранение в NVS -----------------------
void addNewUserFromCommand(String params) {
  // Формат: id|name|uuid|serviceData|mac|thresholdEntry|thresholdExit
  // Пример: 3|Анна|0000ff33-0000-1000-8000-00805f9b34fb|Abc123Xy|AA:BB:CC:DD:EE:FF|-65|-70

  int delimiterIndex[6];
  int count = 0;
  int start = 0;

  // Парсим разделители '|'
  for (int i = 0; i < params.length() && count < 6; i++) {
    if (params[i] == '|') {
      delimiterIndex[count++] = i;
    }
  }

  if (count < 5) {
    log("Ошибка: неверный формат ADDUSER", LOG_WARN);
    return;
  }

  // Извлекаем поля
  int id = params.substring(0, delimiterIndex[0]).toInt();
  String name = params.substring(delimiterIndex[0]+1, delimiterIndex[1]);
  String uuid = params.substring(delimiterIndex[1]+1, delimiterIndex[2]);
  String serviceData = params.substring(delimiterIndex[2]+1, delimiterIndex[3]);
  String mac = params.substring(delimiterIndex[3]+1, delimiterIndex[4]);

  int rssiThresholdEntry, rssiThresholdExit;
  if (count == 6) {
    rssiThresholdEntry = params.substring(delimiterIndex[4]+1, delimiterIndex[5]).toInt();
    rssiThresholdExit = params.substring(delimiterIndex[5]+1).toInt();
  } else {
    // Совместимость со старым форматом (один порог для обоих)
    rssiThresholdEntry = params.substring(delimiterIndex[4]+1).toInt();
    rssiThresholdExit = rssiThresholdEntry;
  }

  // Нормализуем MAC-адрес: заменяем дефисы на двоеточия, приводим к верхнему регистру
  mac.replace("-", ":");
  mac.toUpperCase();

  // Проверяем лимит
  if (trustedDevicesCount >= MAX_USERS) {
    log("Ошибка: достигнут лимит пользователей (" + String(MAX_USERS) + ")", LOG_WARN);
    return;
  }

  // Создаём структуру для NVS
  TrustedDeviceNVS newUser;
  newUser.id = id;
  name.toCharArray(newUser.name, sizeof(newUser.name));
  uuid.toCharArray(newUser.uuid, sizeof(newUser.uuid));
  serviceData.toCharArray(newUser.serviceDataHex, sizeof(newUser.serviceDataHex));
  mac.toCharArray(newUser.macAddress, sizeof(newUser.macAddress));
  newUser.rssiThresholdEntry = rssiThresholdEntry;
  newUser.rssiThresholdExit = rssiThresholdExit;

  // Сохраняем в NVS
  if (saveTrustedDevice(&newUser)) {
    // Сохраняем текущие статусы перед перезагрузкой
    String savedLocations[MAX_USERS];
    int savedIds[MAX_USERS];
    int savedCount = trustedDevicesCount;
    for (int i = 0; i < savedCount; i++) {
      savedIds[i] = trustedDevices[i].id;
      savedLocations[i] = trustedDevices[i].location;
    }

    // Перезагружаем runtime-массив
    trustedDevicesCount = loadTrustedDevices(trustedDevices, MAX_USERS);

    // Восстанавливаем статусы
    for (int i = 0; i < trustedDevicesCount; i++) {
      for (int j = 0; j < savedCount; j++) {
        if (trustedDevices[i].id == savedIds[j]) {
          trustedDevices[i].location = savedLocations[j];
          break;
        }
      }
    }

    // Устанавливаем порог для нового пользователя
    log("Пользователь '" + name + "' добавлен/обновлён (id:" + String(id) + ")", LOG_USER);
  } else {
    log("Ошибка сохранения в NVS", LOG_WARN);
  }
}

// ----------------------- Обновление только порогов RSSI -----------------------
void updateThresholdsFromCommand(String params) {
  // Формат: id|entry|exit
  int firstPipe = params.indexOf('|');
  int lastPipe = params.lastIndexOf('|');

  if (firstPipe == -1 || lastPipe == -1 || firstPipe == lastPipe) return;

  int id = params.substring(0, firstPipe).toInt();
  int entryThreshold = params.substring(firstPipe + 1, lastPipe).toInt();
  int exitThreshold = params.substring(lastPipe + 1).toInt();

  // 1. Обновляем в runtime массиве
  bool found = false;
  for (int i = 0; i < trustedDevicesCount; i++) {
    if (trustedDevices[i].id == id) {
      trustedDevices[i].rssiThresholdEntry = entryThreshold;
      trustedDevices[i].rssiThresholdExit = exitThreshold;
      found = true;

      // 2. Обновляем в NVS (нужно прочитать старые данные, изменить пороги и сохранить)
      String key = "user_" + String(id);
      TrustedDeviceNVS temp;
      if (usersPrefs.getBytes(key.c_str(), &temp, sizeof(TrustedDeviceNVS)) == sizeof(TrustedDeviceNVS)) {
        temp.rssiThresholdEntry = entryThreshold;
        temp.rssiThresholdExit = exitThreshold;
        saveTrustedDevice(&temp);
        log(String(temp.name) + ": пороги обновлены", LOG_INFO);
      }
      break;
    }
  }
}

// ----------------------Обработчик команд из приложения ----------------
void commandHandler() {
  if (rxValue.length() > 0) {
    String cmd = String(rxValue.c_str());

    if (cmd.equalsIgnoreCase("OPENDOOR")) {
      openDoor(0, "APP");
    }

    if (cmd.equalsIgnoreCase("LIGHTON")) {
      lightSwitches("lightON", "Освещение включено");
    }

    if (cmd.equalsIgnoreCase("LIGHTOFF")) {
      lightSwitches("lightOFF", "Освещение отключено");
    }

    // SETUSER:id-state (state: 1=inside, 0=outside)
    if (cmd.startsWith("SETUSER:")) {
      String params = cmd.substring(8);
      int dashIndex = params.indexOf('-');
      if (dashIndex > 0) {
        int userId = params.substring(0, dashIndex).toInt();
        int state = params.substring(dashIndex + 1).toInt();

        // Находим устройство по ID
        for (int i = 0; i < trustedDevicesCount; i++) {
          if (trustedDevices[i].id == userId) {
            String userName = String(trustedDevices[i].name);
            if (state == 1) {
              trustedDevices[i].location = "inside";
              log(userName + ": в лаборатории", LOG_USER);
            } else {
              trustedDevices[i].location = "outside";
              log(userName + ": на улице", LOG_USER);
            }
            break;
          }
        }
      }
    }

    // ADDUSER:id|name|uuid|serviceData|mac|threshold
    if (cmd.startsWith("ADDUSER:")) {
      String params = cmd.substring(8);
      addNewUserFromCommand(params);
    }

    // SETTHRESH:id|entry|exit
    if (cmd.startsWith("SETTHRESH:")) {
      String params = cmd.substring(10);
      updateThresholdsFromCommand(params);
    }

    // SETDIST:value
    if (cmd.startsWith("SETDIST:")) {
      int newDist = cmd.substring(8).toInt();
      if (newDist > 0) {
        saveDistanceThreshold(newDist);
        log("Порог расстояния изменён: " + String(newDist) + " см", LOG_INFO);
      }
    }

    // SETDOOR:time|cooldown
    if (cmd.startsWith("SETDOOR:")) {
      String params = cmd.substring(8);
      int pipeIndex = params.indexOf('|');
      if (pipeIndex != -1) {
        int doorTime = params.substring(0, pipeIndex).toInt();
        int doorCooldown = params.substring(pipeIndex + 1).toInt();
        saveDoorParams(doorTime, doorCooldown);
        log("Параметры двери: " + String(doorTime) + "мс / " + String(doorCooldown) + "мс", LOG_INFO);
      }
    }

    // DELUSER:id
    if (cmd.startsWith("DELUSER:")) {
      int id = cmd.substring(8).toInt();

      String userName = "Неизвестный";
      for (int i = 0; i < trustedDevicesCount; i++) {
        if (trustedDevices[i].id == id) {
          userName = String(trustedDevices[i].name);
          break;
        }
      }

      if (deleteTrustedDevice(id)) {
        // Сохраняем текущие статусы перед перезагрузкой
        String savedLocations[MAX_USERS];
        int savedIds[MAX_USERS];
        int savedCount = trustedDevicesCount;
        for (int i = 0; i < savedCount; i++) {
          savedIds[i] = trustedDevices[i].id;
          savedLocations[i] = trustedDevices[i].location;
        }

        // Перезагружаем массив из NVS
        trustedDevicesCount = loadTrustedDevices(trustedDevices, MAX_USERS);

        // Восстанавливаем статусы
        for (int i = 0; i < trustedDevicesCount; i++) {
          for (int j = 0; j < savedCount; j++) {
            if (trustedDevices[i].id == savedIds[j]) {
              trustedDevices[i].location = savedLocations[j];
              break;
            }
          }
        }

        log("Пользователь '" + userName + "' удалён (id:"+ String(id) + ")", LOG_USER);
      }
    }

    // LISTUSERS — отправить список всех пользователей в приложение
    if (cmd.equalsIgnoreCase("LISTUSERS")) {
      sendUserListChunked();
    }

    // LOGLIST — отправить историю логов
    if (cmd.equalsIgnoreCase("LOGLIST")) {
      sendLogHistoryChunked();
    }

    // RECONNECT_JDE — попытка переподключиться к модулю освещения
    if (cmd.equalsIgnoreCase("RECONNECT_JDE")) {
      log("Запрос на переподключение к JDY-33...", LOG_INFO);
      if (scanAndConnect()) {
        log("Подключение к JDY-33 успешно", LOG_INFO);
      } else {
        log("Не удалось подключиться к JDY-33", LOG_WARN);
      }
    }

    // REBOOT — ручная перезагрузка контроллера
    if (cmd.equalsIgnoreCase("REBOOT")) {
      setManualRebootFlag(true);
      delay(200);
      ESP.restart();
    }

    // START_WIFI_OTA — переход в режим прошивки по Wi-Fi
    if (cmd.equalsIgnoreCase("START_WIFI_OTA")) {
      startOtaMode();
    }

    // SETTIME:YYYY|MM|DD|HH|MM|SS
    if (cmd.startsWith("SETTIME:")) {
      String params = cmd.substring(8);
      int p[6];
      int count = 0;
      int start = 0;
      for (int i = 0; i < params.length() && count < 6; i++) {
        if (params[i] == '|') {
          p[count++] = params.substring(start, i).toInt();
          start = i + 1;
        }
      }
      if (count == 5) {
        p[5] = params.substring(start).toInt();
        rtc.adjust(DateTime(p[0], p[1], p[2], p[3], p[4], p[5]));
        batteryWarning = false; // Сбрасываем предупреждение после синхронизации
        log("Время синхронизировано", LOG_INFO);
      }
    }

    // SET_VERBOSE:0/1 — управление детальным логированием
    if (cmd.startsWith("SET_VERBOSE:")) {
      verboseLogging = (cmd.substring(12) == "1");
      log("Подробное логирование: " + String(verboseLogging ? "ВКЛ" : "ВЫКЛ"), LOG_INFO);
    }

    // SETWIFI:ssid|password
    if (cmd.startsWith("SETWIFI:")) {
      String params = cmd.substring(8);
      int pipeIndex = params.indexOf('|');
      if (pipeIndex != -1) {
        String ssid = params.substring(0, pipeIndex);
        String pass = params.substring(pipeIndex + 1);
        saveWifiSettings(ssid, pass);
        log("Настройки WiFi сохранены: " + ssid, LOG_INFO);
      }
    }

    rxValue = "";
  }
}
