#include "Utils.h"
#include "DoorControl.h"
#include "BLEManager.h"

// ------------------ Глобальные переменные ------------------
String lightStatus = "LIGHTSTATUS:0";
bool autoLightOutside = true;
unsigned long SendCommandTime = 0;
float lastMeasuredDistance = 0.0f;

// ------------------ Измерение расстояния HC-SR04 ------------------
float getDistance() {
  const int SAMPLES = 3;
  long durations[SAMPLES];
  int validCount = 0;

  for (int i = 0; i < SAMPLES; i++) {
    digitalWrite(TRIG_PIN, LOW);
    delayMicroseconds(5);
    digitalWrite(TRIG_PIN, HIGH);
    delayMicroseconds(10);
    digitalWrite(TRIG_PIN, LOW);
    
    // Увеличим таймаут до 30000 (около 5 метров)
    long duration = pulseIn(ECHO_PIN, HIGH, 30000);

    if (duration > 100) { // Игнорируем слишком короткие импульсы (шум)
      durations[validCount++] = duration;
    }

    // ОБЯЗАТЕЛЬНАЯ ПАУЗА между импульсами, чтобы эхо затухло
    delay(30);
  }

  if (validCount == 0) return 999.0;

  // Сортировка только валидных значений
  for (int i = 0; i < validCount - 1; i++) {
    for (int j = i + 1; j < validCount; j++) {
      if (durations[i] > durations[j]) {
        long tmp = durations[i];
        durations[i] = durations[j];
        durations[j] = tmp;
      }
    }
  }
  
  // Берем среднее значение (медиану)
  long resultDuration = durations[validCount / 2];

  // Возвращаем с твоим коэффициентом
  return (resultDuration * 0.03632) / 2.0;
}

//---------------Обновление статуса доверенных устройств--------------
void updateDeviceStatus(String name, String newStatus) {
  for (int i = 0; i < trustedDevicesCount; i++) {
    if (trustedDevices[i].name == name) {
      trustedDevices[i].location = newStatus;
      trustedDevices[i].userTime = millis();
      trustedDevices[i].lastTransitionTime = millis(); // Фиксируем время перехода
    }
  }
}

// ----------------- Проверка на присутствие BLE -----------------------
void processExitAndEntry(DevicesDetected *devDetected) {
  for (int i = 0; i < 10; i++) {
    if (devDetected[i].name == "") continue;

    for (int j = 0; j < trustedDevicesCount; j++) {
      if (devDetected[i].name == trustedDevices[j].name) {
        // Просто фиксируем, что устройство в зоне видимости BLE
        trustedDevices[j].userTime = millis();
      }
    }
  }
}

void checkEntryExitStatus() {
  float distance = getDistance();
  lastMeasuredDistance = distance;
  bool pirTriggered = (digitalRead(SENSOR_PIN) == LOW);
  unsigned long now = millis();

  // Вывод расстояния в Serial раз в 2 секунды
  static unsigned long lastDistPrintTime = 0;
  if (now - lastDistPrintTime > 2000) {
    lastDistPrintTime = now;
    Serial.printf("Расстояние (HC-SR04): %.2f см (Порог: %d)\n", distance, currentDistanceThreshold);
  }

  // 1. Обработка датчика HC-SR04 (Снаружи)
  if (distance > 2) {
    bool isWithinThreshold = (distance < currentDistanceThreshold);
    bool doorOpenedThisCycle = false;

    for (int j = 0; j < trustedDevicesCount; j++) {
      // Человек подходит снаружи: он outside и его BLE недавно видели (увеличим до 15 сек)
      if (trustedDevices[j].location == "outside" && (now - trustedDevices[j].userTime < 15000)) {
        if (!trustedDevices[j].entryInProgress && (now - trustedDevices[j].lastTransitionTime > (unsigned long)currentDoorCooldown)) {
          if (isWithinThreshold) {
            trustedDevices[j].entryInProgress = true;
            trustedDevices[j].processStartTime = now;
            trustedDevices[j].exitInProgress = false;
            if (!doorOpenedThisCycle) {
              openDoor(8, "HC-SR04");
              doorOpenedThisCycle = true;
            }
          }
        }
      }

      // Человек выходит: у него стоял флаг exitInProgress и теперь он появился перед внешним датчиком
      if (trustedDevices[j].exitInProgress) {
        if (isWithinThreshold) {
          trustedDevices[j].exitInProgress = false;
          updateDeviceStatus(trustedDevices[j].name, "outside");
          log(trustedDevices[j].name + " покинул лабораторию", LOG_USER);
        } else if (distance < 150) {
          // Лог для отладки: видим объект, но он дальше порога
          static unsigned long lastWarnTime = 0;
          if (now - lastWarnTime > 2000) {
            Serial.printf("DEBUG: Объект на выходе (%.2f см) дальше порога (%d см)\n", distance, currentDistanceThreshold);
            lastWarnTime = now;
          }
        }
      }
    }
  }

  // 2. Обработка PIR сенсора (Внутри)
  if (pirTriggered) {
    bool doorOpenedThisCycle = false;
    for (int j = 0; j < trustedDevicesCount; j++) {
      // Человек подходит изнутри: он inside и его BLE недавно видели (увеличим до 15 сек)
      if (trustedDevices[j].location == "inside" && (now - trustedDevices[j].userTime < 15000)) {
        // Проверяем "кулдаун" после последнего перехода
        if (!trustedDevices[j].exitInProgress && (now - trustedDevices[j].lastTransitionTime > (unsigned long)currentDoorCooldown)) {
          trustedDevices[j].exitInProgress = true;
          trustedDevices[j].processStartTime = now;
          trustedDevices[j].entryInProgress = false;
          if (!doorOpenedThisCycle) {
            openDoor(8, "PIR_INSIDE");
            doorOpenedThisCycle = true;
          }
        }
      }

      // Человек входит: у него стоял флаг entryInProgress и теперь он прошел внутренний датчик
      if (trustedDevices[j].entryInProgress) {
        trustedDevices[j].entryInProgress = false;
        updateDeviceStatus(trustedDevices[j].name, "inside");
        log(trustedDevices[j].name + " вошёл в лабораторию", LOG_USER);
      }
    }
  }
}

// ----------------------- Включение и отключение освещения -------------------------
void lightSwitches(String command, String message) {
  if (jdeConnect && command == "lightON") {
    lightStatus = "LIGHTSTATUS:1";

    uint8_t cmd[] = {0xA0, 0x01, 0x01, 0xA2};
    pRemoteCharacteristic->writeValue(cmd, sizeof(cmd));
    updateAdvertising(); // Обновляем BLE рекламу
    //log(message, LOG_INFO);
  }
  else if (jdeConnect && command == "lightOFF") {
    lightStatus = "LIGHTSTATUS:0";

    uint8_t cmd[] = {0xA0, 0x01, 0x00, 0xA1};
    pRemoteCharacteristic->writeValue(cmd, sizeof(cmd));
    updateAdvertising(); // Обновляем BLE рекламу
    //log(message, LOG_INFO);
  } else {
    log("JDE-33 не подключен", LOG_WARN);
  }
}

// ---------- Проверка людей внутри и автоматическое включение освещения ------------
void checkPeopleInside() {
  bool oldJde = jdeConnect;
  byte numberOfPeopleInside = 0;

  for (int i = 0; i < trustedDevicesCount; i++) {
    if (trustedDevices[i].location == "inside")
      numberOfPeopleInside++;
  }

  if (lightStatus == "LIGHTSTATUS:0" && numberOfPeopleInside == 1 && !autoLightOutside) {
    autoLightOutside = true;
    lightSwitches("lightON", "Автоматическое включение освещения");
  } else if (numberOfPeopleInside == 0 && autoLightOutside) {
    autoLightOutside = false;
    lightSwitches("lightOFF", "Автоматическое отключение освещения");
  }

  // Если статус соединения с JDY-33 изменился, обновляем рекламу
  if (oldJde != jdeConnect) {
    updateAdvertising();
  }
}
