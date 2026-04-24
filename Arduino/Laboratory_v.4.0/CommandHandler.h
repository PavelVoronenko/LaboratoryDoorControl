#ifndef COMMAND_HANDLER_H
#define COMMAND_HANDLER_H

#include <Arduino.h>
#include "DeviceStorage.h"
#include "BLEManager.h"
#include "Utils.h"

// ------------------ Обработчик команд из приложения ------------------
void commandHandler();

// ------------------ Добавление нового пользователя ------------------
void addNewUserFromCommand(String params);
void updateThresholdsFromCommand(String params);

// ------------------ Функции из других модулей ------------------
void openDoor(int pause, String source);
void lightSwitches(String command, String message);
void sendUserListChunked();
int loadTrustedDevices(TrustedDevice* outArray, int maxSize);

#endif // COMMAND_HANDLER_H
