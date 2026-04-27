#include <Arduino.h>
#include "Pins.h"
#include "DeviceStorage.h"
#include "BLEManager.h"
#include "DoorControl.h"
#include "CommandHandler.h"
#include "Utils.h"

// ------------------ Многозадачность --------------------
TaskHandle_t Task1;
TaskHandle_t Task2;

// ------------------ Setup ------------------
void setup() {
  Serial.begin(115200);

  // Устанавливаем частоту CPU (80 МГц)
  setCpuFrequencyMhz(80);

  // Инициализация NVS
  initStorage();

  // Загрузка порога расстояния
  loadDistanceThreshold();

  // Загрузка пользователей из памяти
  trustedDevicesCount = loadTrustedDevices(trustedDevices, MAX_USERS);
  Serial.printf("Загружено %d доверенных устройств из NVS\n", trustedDevicesCount);

  // Инициализация пинов
  initPins();

  // Инициализация BLE сервера
  initBLEServer();

  // Многозадачность
  xTaskCreatePinnedToCore(Task1code, "Task1", 26000, NULL, 0, &Task1, 0);
  delay(500);
  xTaskCreatePinnedToCore(Task2code, "Task2", 26000, NULL, 1, &Task2, 1);
  delay(500);

  // Попытка подключиться к JDY-33
  if (scanAndConnect()) {
    Serial.println("Подключение к JDY-33 успешно");
    jdeConnect = true;
    connected = true;
  } else {
    Serial.println("Подключение к JDY-33 не удалось");
    jdeConnect = false;
  }

  log("Система запущена. Версия 4.0", LOG_INFO);

  // Проверка флага ручной перезагрузки
  if (getManualRebootFlag()) {
    log("Выполнена ручная перезагрузка системы через приложение", LOG_INFO);
    setManualRebootFlag(false); // Сбрасываем флаг
  }
}

unsigned long lastDebugTime = 0;

//-----------------------------------First Core------------------------------------------------//
void Task1code(void * pvParameters) {
  for (;;) {
    // Проверка BLE устройств (для доверенных устройств)
    scanForTrustedDevices();

    // Отправка служебных данных
    if (millis() - SendCommandTime > 1000) {
      SendCommandTime = millis();
      sendCommand();
    }

    // Отправка отладочных данных (раз в 500мс)
    if (millis() - lastDebugTime > 500) {
      lastDebugTime = millis();
      sendDebugData(lastMeasuredDistance, currentDistanceThreshold);
    }

    // Попытка восстановить соединение с JDY-33
    if (connected && !pClient->isConnected()) {
      Serial.println("Соединение потеряно, повторное сканирование...");
      if (scanAndConnect()) {
        Serial.println("Повторное подключение успешно");
        jdeConnect = true;
      } else {
        Serial.println("Не удалось повторно подключиться");
        jdeConnect = false;
      }
    }
  }
}

//--------------------------------------Second core--------------------------------------------//
void Task2code(void * pvParameters) {
  for (;;) {
    // Обработчик команд приложения
    commandHandler();

    // Закрытие двери
    closedDoor();

    // Проверка статуса выхода
    checkEntryExitStatus();

    // Автоматическое отключение освещения
    checkPeopleInside();

    // Обработка датчика движения
    if (digitalRead(SENSOR_PIN) == LOW) {
      openDoor(10, "PIR SENSOR");
    }

    // Обнуление процесса входа/выхода (таймаут 20 секунд)
    for (int i = 0; i < trustedDevicesCount; i++) {
      if (trustedDevices[i].entryInProgress || trustedDevices[i].exitInProgress) {
        if (millis() - trustedDevices[i].processStartTime > 20000) {
          trustedDevices[i].entryInProgress = false;
          trustedDevices[i].exitInProgress = false;
          log(trustedDevices[i].name + " таймаут перехода", LOG_WARN);
        }
      }
    }
  }
}

// ------------------------- Основной цикл ---------------------------
void loop() {
  vTaskDelay(10000);
}
