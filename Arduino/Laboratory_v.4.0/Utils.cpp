#include "Utils.h"
#include "DoorControl.h"

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
      log(trustedDevices[i].name + " теперь " + newStatus);
    }
  }
}

// ----------------- Проверка на вход и выход -----------------------
void processExitAndEntry(DevicesDetected *devDetected) {
  for (int i = 0; i < 10; i++) {
    for (int j = 0; j < trustedDevicesCount; j++) {
      if (devDetected[i].name == trustedDevices[j].name) {
        if (millis() - trustedDevices[j].userTime > 11000) {
          trustedDevices[j].userTime = millis();

          if (trustedDevices[j].location == "outside") {
            openDoor(8, "|BLE| ");
            trustedDevices[j].entryInProgress = true;
            log(devDetected[i].name + " начинает вход");
          }

          if (trustedDevices[j].location == "inside") {
            trustedDevices[j].exitInProgress = true;
            log(devDetected[i].name + " начинает выход");
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
      } else if (trustedDevices[j].exitInProgress && trustedDevices[j].location == "inside") {
        trustedDevices[j].exitInProgress = false;
        updateDeviceStatus(trustedDevices[j].name, "outside");
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
    log(message);
  }
  else if (jdeConnect && command == "lightOFF") {
    lightStatus = "LIGHTSTATUS:0";

    uint8_t cmd[] = {0xA0, 0x01, 0x00, 0xA1};
    pRemoteCharacteristic->writeValue(cmd, sizeof(cmd));
    log(message);
  } else {
    log("JDE-33 не подключен");
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
