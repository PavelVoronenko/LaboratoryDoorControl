#include "CommandHandler.h"
#include "DeviceStorage.h"
#include "BLEManager.h"
#include "DoorControl.h"
#include "Utils.h"

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
    log("Ошибка: неверный формат ADDUSER", LOG_WARN);
    return;
  }

  // Извлекаем поля
  int id = params.substring(0, delimiterIndex[0]).toInt();
  String name = params.substring(delimiterIndex[0]+1, delimiterIndex[1]);
  String uuid = params.substring(delimiterIndex[1]+1, delimiterIndex[2]);
  String serviceData = params.substring(delimiterIndex[2]+1, delimiterIndex[3]);
  String mac = params.substring(delimiterIndex[3]+1, delimiterIndex[4]);
  int rssiThreshold = params.substring(delimiterIndex[4]+1).toInt();

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
    int idx = findDeviceByMAC(trustedDevices, trustedDevicesCount, mac);
    if (idx >= 0) {
      trustedDevices[idx].rssiThreshold = rssiThreshold;
    }

    log("Пользователь '" + name + "' добавлен (id:" + String(id) + ")", LOG_USER);
  } else {
    log("Ошибка сохранения в NVS", LOG_WARN);
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

    rxValue = "";
  }
}
