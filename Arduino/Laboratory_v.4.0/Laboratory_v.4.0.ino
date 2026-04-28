#include <Arduino.h>
#include "Pins.h"
#include "DeviceStorage.h"
#include "BLEManager.h"
#include "DoorControl.h"
#include "CommandHandler.h"
#include "Utils.h"
#include "OTAManager.h"

// ------------------ Многозадачность --------------------
TaskHandle_t Task1;
TaskHandle_t Task2;

// ------------------ Глобальные переменные ------------------
bool batteryWarning = false;

// ------------------ Setup ------------------
void setup() {
  Serial.begin(115200);

  // Инициализация NVS для доступа к настройкам
  initStorage();

  // ПРОВЕРКА ФЛАГА OTA ИЗ NVS
  if (getOtaBootFlag()) {
    setOtaBootFlag(false); // Сбрасываем флаг сразу
    otaModeActive = true;

    setCpuFrequencyMhz(160);
    initPins();
    setupOtaInBoot();

    Serial.println("Система загружена в режиме OTA. BLE отключен.");

    // Запускаем только Task2 для обслуживания OTA и управления дверью
    xTaskCreatePinnedToCore(Task2code, "Task2", 26000, NULL, 1, &Task2, 1);

    return; // Пропускаем остальную инициализацию
  }

  // Устанавливаем частоту CPU (160 МГц для стабильности Wi-Fi+BLE)
  setCpuFrequencyMhz(160);

  // Инициализация RTC DS3231
  if (!rtc.begin()) {
    log("Не удалось найти RTC", LOG_VERBOSE);
  }
  if (rtc.lostPower()) {
    log("RTC потерял питание, установите время!", LOG_VERBOSE);
    batteryWarning = true;
  }

  // Загрузка порога расстояния
  loadDistanceThreshold();

  // Загрузка параметров двери
  loadDoorOpenTime();
  loadDoorCooldown();

  // Загрузка пользователей из памяти
  trustedDevicesCount = loadTrustedDevices(trustedDevices, MAX_USERS);
  Serial.printf("Загружено %d доверенных устройств из NVS\n", trustedDevicesCount);

  // Инициализация пин-кодов
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
    log("Подключение к JDY-33 успешно", LOG_VERBOSE);
    jdeConnect = true;
    connected = true;
  } else {
    log("Подключение к JDY-33 не удалось", LOG_VERBOSE);
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
    if (otaModeActive) {
      vTaskDelay(1000 / portTICK_PERIOD_MS);
      continue;
    }

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
      sendDebugData(lastMeasuredDistance, currentDistanceThreshold, currentDoorOpenTime, currentDoorCooldown);
    }

    // Попытка восстановить соединение с JDY-33
    if (connected && pClient != NULL && !pClient->isConnected()) {
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
    if (otaModeActive) {
      handleOta();
      vTaskDelay(1 / portTICK_PERIOD_MS); // Минимальная задержка для OTA
      continue;
    }

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

    // Обнуление процесса входа/выхода (таймаут 10 секунд)
    for (int i = 0; i < trustedDevicesCount; i++) {
      if (trustedDevices[i].entryInProgress || trustedDevices[i].exitInProgress) {
        if (millis() - trustedDevices[i].processStartTime > 10000) {
          trustedDevices[i].entryInProgress = false;
          trustedDevices[i].exitInProgress = false;
          log(trustedDevices[i].name + " таймаут перехода", LOG_VERBOSE);
        }
      }
    }
  }
}

// ------------------------- Основной цикл ---------------------------
void loop() {
  vTaskDelay(10000);
}
