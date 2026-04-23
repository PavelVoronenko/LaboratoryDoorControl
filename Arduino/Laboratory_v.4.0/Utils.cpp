#include "Utils.h"
#include "DoorControl.h"
#include "BLEManager.h"

// ------------------ Глобальные переменные ------------------
String lightStatus = "LIGHTSTATUS:0";
bool autoLightOutside = true;
unsigned long SendCommandTime = 0;

//---------------Обновление статуса доверенных устройств--------------
void updateDeviceStatus(String name, String newStatus) {
  for (int i = 0; i < trustedDevicesCount; i++) {
    if (trustedDevices[i].name == name) {
      trustedDevices[i].location = newStatus;
      trustedDevices[i].userTime = millis();
    }
  }
}

// ----------------- Проверка на вход и выход -----------------------
void processExitAndEntry(DevicesDetected *devDetected) {
  for (int i = 0; i < 10; i++) {
    if (devDetected[i].name == "") continue;

    for (int j = 0; j < trustedDevicesCount; j++) {
      if (devDetected[i].name == trustedDevices[j].name) {
        unsigned long now = millis();

        if (now - trustedDevices[j].userTime > 11000) {
          trustedDevices[j].userTime = now;

          if (trustedDevices[j].location == "outside") {
            String source = String(String(devDetected[i].rssi) + " dBm");
            openDoor(8, source);
            trustedDevices[j].entryInProgress = true;
          }

          if (trustedDevices[j].location == "inside") {
            trustedDevices[j].exitInProgress = true;
          }
        }
      }
    }
  }
}

void checkEntryExitStatus() {
  if (digitalRead(SENSOR_PIN) == LOW) {
    for (int j = 0; j < trustedDevicesCount; j++) {
      if (trustedDevices[j].entryInProgress && trustedDevices[j].location == "outside") {
        trustedDevices[j].entryInProgress = false;
        updateDeviceStatus(trustedDevices[j].name, "inside");
        log(trustedDevices[j].name + " вошёл в лабораторию", LOG_USER);
      } else if (trustedDevices[j].exitInProgress && trustedDevices[j].location == "inside") {
        trustedDevices[j].exitInProgress = false;
        updateDeviceStatus(trustedDevices[j].name, "outside");
        log(trustedDevices[j].name + " покинул лабораторию", LOG_USER);
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
    //log(message, LOG_INFO);
  }
  else if (jdeConnect && command == "lightOFF") {
    lightStatus = "LIGHTSTATUS:0";

    uint8_t cmd[] = {0xA0, 0x01, 0x00, 0xA1};
    pRemoteCharacteristic->writeValue(cmd, sizeof(cmd));
    //log(message, LOG_INFO);
  } else {
    log("JDE-33 не подключен", LOG_WARN);
  }
}

// ---------- Проверка людей внутри и автоматическое включение освещения ------------
void checkPeopleInside() {
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
}
