#ifndef PINS_H
#define PINS_H

// ------------------------------- Пины ---------------------------------
#define OPENING_PIN 32
#define SPEAKER_PIN 22
#define SENSOR_PIN 23

// ------------------------- BLE параметры ------------------------------
#define SCAN_TIME 1 // 1 сек
#define SERVICE_UUID "0000ffe0-0000-1000-8000-00805f9b34fb"
#define CHAR1_UUID   "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define CHAR2_UUID_TERMINAL   "e3223119-9445-4e96-a4a1-85358c4046a2"
#define JDYCHAR_UUID "0000ffe1-0000-1000-8000-00805f9b34fb"
#define TARGET_NAME "JDY-33-BLE"

// ----------------- Максимальное количество пользователей --------------
#define MAX_USERS 10
#define NVS_NAMESPACE "trusted_users"

#endif // PINS_H
