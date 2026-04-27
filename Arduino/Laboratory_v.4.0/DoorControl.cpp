#include "DoorControl.h"
#include "DeviceStorage.h"
#include "BLEManager.h"

// ------------------ Глобальные переменные ------------------
unsigned long doorOpenTimestamp = 0;

// ------------------ Инициализация пинов ------------------
void initPins() {
  pinMode(OPENING_PIN, OUTPUT);
  pinMode(SPEAKER_PIN, OUTPUT);
  pinMode(SENSOR_PIN, INPUT_PULLUP);
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);

  digitalWrite(OPENING_PIN, LOW);
  digitalWrite(TRIG_PIN, LOW);
  ledcAttach(SPEAKER_PIN, 4, 8);
}

// ---------------------------- Открытие и закрытие -----------------------------
void openDoor(int pause, String source) {
  if (millis() - doorOpenTimestamp > (unsigned long)pause * 1000) {
    doorOpenTimestamp = millis();
    ledcAttach(SPEAKER_PIN, 4, 8);
    ledcWrite(SPEAKER_PIN, 150);
    digitalWrite(OPENING_PIN, HIGH);

    String logMsg = "Дверь открыта [" + source + "]";
    log(logMsg, LOG_DOOR);
  }
}

void closedDoor() {
  if (digitalRead(OPENING_PIN) == HIGH) {
    if (millis() - doorOpenTimestamp > 3000) {
      ledcDetach(SPEAKER_PIN);
      digitalWrite(SPEAKER_PIN, LOW);
      digitalWrite(OPENING_PIN, LOW);

      // Проиграть сигнал если все на улице
      if (allUsersOutside()) {
        playThreeBeeps();
        log("Все сотрудники на улице, проигран сигнал", LOG_INFO);
      }
    }
  }
}

// ---------------------------- Сигнал когда все на улице ------------------------
bool allUsersOutside() {
  int outsideCount = 0;

  for (int j = 0; j < trustedDevicesCount; j++) {
    if (trustedDevices[j].location == "outside") {
      outsideCount++;
    }
  }

  return (outsideCount == 4);
}

void playThreeBeeps() {
  for (int i = 0; i < 3; i++) {
    ledcWrite(SPEAKER_PIN, 150);
    delay(150);
    ledcDetach(SPEAKER_PIN);
    digitalWrite(SPEAKER_PIN, LOW);
    delay(150);
    ledcAttach(SPEAKER_PIN, 4, 8);
  }
  ledcDetach(SPEAKER_PIN);
  digitalWrite(SPEAKER_PIN, LOW);
}
