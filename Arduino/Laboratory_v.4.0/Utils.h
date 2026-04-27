#ifndef UTILS_H
#define UTILS_H

#include <Arduino.h>
#include "DeviceStorage.h"
#include "BLEManager.h"
#include "DoorControl.h"

// ------------------ Измерение расстояния HC-SR04 ------------------
float getDistance();

// ------------------ Обновление статуса доверенных устройств --------------
void updateDeviceStatus(String name, String newStatus);

// ----------------- Проверка на вход и выход -----------------------
void processExitAndEntry(DevicesDetected *devDetected);

// ----------------- Проверка статуса входа/выхода -----------------------
void checkEntryExitStatus();

// ----------------------- Включение и отключение освещения -------------------------
void lightSwitches(String command, String message);

// ---------- Проверка людей внутри и автоматическое включение освещения ------------
void checkPeopleInside();

#endif // UTILS_H
