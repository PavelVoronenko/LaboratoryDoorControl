#ifndef DEVICE_STORAGE_H
#define DEVICE_STORAGE_H

#include <Arduino.h>
#include <Preferences.h>
#include "Pins.h"

// -------------- Структура для хранения в NVS (минимальная) ------------
struct TrustedDeviceNVS {
  int id;                    // Уникальный ID (ключ)
  char name[64];             // Фиксированный размер для NVS
  char uuid[37];             // UUID формата: 00000000-0000-0000-0000-000000000000
  char serviceDataHex[32];   // Service data
  char macAddress[18];       // MAC: XX:XX:XX:XX:XX:XX
  int rssiThresholdEntry;    // Порог RSSI для входа
  int rssiThresholdExit;     // Порог RSSI для выхода
};

// ------------------ Структура доверенного устройства ------------------
struct TrustedDevice {
  int id;
  String name;
  String uuid;
  String serviceDataHex;
  String macAddress;
  String location; // "inside" или "outside"
  int rssiThresholdEntry;
  int rssiThresholdExit;
  int lastRssi;
  uint64_t userTime;
  uint64_t processStartTime;
  uint64_t lastTransitionTime;
  bool entryInProgress;
  bool exitInProgress;
};

// ------------------ Инициализация хранилища ------------------
void initStorage();

// ------------------ Загрузка всех пользователей из NVS в runtime-массив ------------------
int loadTrustedDevices(TrustedDevice* outArray, int maxSize);

// ------------------ Сохранение одного пользователя в NVS ------------------
bool saveTrustedDevice(const TrustedDeviceNVS* device);

// ------------------ Удаление пользователя по ID ------------------
bool deleteTrustedDevice(int id);

// ------------------ Поиск устройства по MAC ------------------
int findDeviceByMAC(const TrustedDevice* array, int count, const String& mac);

// ------------------ Поиск устройства по UUID + ServiceData ------------------
int findDeviceByUUID(const TrustedDevice* array, int count, const String& uuid, const String& serviceData);

// ------------------ Управление флагом перезагрузки ------------------
void setManualRebootFlag(bool status);
bool getManualRebootFlag();

// ------------------ Управление порогом расстояния ------------------
void saveDistanceThreshold(int threshold);
int loadDistanceThreshold();

// ------------------ Управление параметрами двери ------------------
void saveDoorParams(int openTime, int cooldown);
int loadDoorOpenTime();
int loadDoorCooldown();

// ------------------ Управление флагом OTA ------------------
void setOtaBootFlag(bool status);
bool getOtaBootFlag();

// Глобальные переменные
extern Preferences usersPrefs;
extern TrustedDevice trustedDevices[];
extern int trustedDevicesCount;
extern int currentDistanceThreshold;
extern int currentDoorOpenTime;
extern int currentDoorCooldown;

#endif // DEVICE_STORAGE_H
