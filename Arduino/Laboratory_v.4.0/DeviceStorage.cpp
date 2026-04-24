#include "DeviceStorage.h"
#include <Arduino.h>

// ------------------ Глобальные переменные -----------------------
Preferences usersPrefs;
TrustedDevice trustedDevices[MAX_USERS];
int trustedDevicesCount = 0;

// ------------------ Инициализация хранилища ------------------
void initStorage() {
  usersPrefs.begin(NVS_NAMESPACE, false); // false = read/write
}

// ------------------ Загрузка всех пользователей из NVS в runtime-массив ------------------
int loadTrustedDevices(TrustedDevice* outArray, int maxSize) {
  int count = 0;

  Serial.println("=== NVS Load Debug ===");
  Serial.printf("Namespace: %s, MaxUsers: %d\n", NVS_NAMESPACE, maxSize);

  // Проверяем диапазон ID (0..99), не прерываемся при пропусках
  for (int potentialId = 0; potentialId < 100 && count < maxSize; potentialId++) {
    String key = "user_" + String(potentialId);

    if (!usersPrefs.isKey(key.c_str())) {
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
      outArray[count].rssiThresholdEntry = temp.rssiThresholdEntry;
      outArray[count].rssiThresholdExit = temp.rssiThresholdExit;

      // Нормализуем MAC при загрузке
      outArray[count].macAddress.replace("-", ":");
      outArray[count].macAddress.toUpperCase();

      // Runtime-поля
      outArray[count].location = "inside";
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

// ------------------ Сохранение одного пользователя в NVS ------------------
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
    usersPrefs.end();
    usersPrefs.begin(NVS_NAMESPACE, false);
    Serial.println("NVS committed");
  }

  return result;
}

// ------------------ Удаление пользователя по ID ------------------
bool deleteTrustedDevice(int id) {
  String key = "user_" + String(id);
  return usersPrefs.remove(key.c_str());
}

// ------------------ Поиск устройства по MAC ------------------
int findDeviceByMAC(const TrustedDevice* array, int count, const String& mac) {
  for (int i = 0; i < count; i++) {
    if (array[i].macAddress.equalsIgnoreCase(mac)) {
      return i;
    }
  }
  return -1;
}

// ------------------ Поиск устройства по UUID + ServiceData ------------------
int findDeviceByUUID(const TrustedDevice* array, int count, const String& uuid, const String& serviceData) {
  for (int i = 0; i < count; i++) {
    if (array[i].uuid.equalsIgnoreCase(uuid) &&
        array[i].serviceDataHex.equalsIgnoreCase(serviceData)) {
      return i;
    }
  }
  return -1;
}

// ------------------ Управление флагом перезагрузки ------------------
void setManualRebootFlag(bool status) {
  usersPrefs.putBool("reboot_flag", status);
}

bool getManualRebootFlag() {
  return usersPrefs.getBool("reboot_flag", false);
}
