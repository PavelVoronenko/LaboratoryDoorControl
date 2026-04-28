#ifndef OTA_MANAGER_H
#define OTA_MANAGER_H

#include <Arduino.h>

extern bool otaModeActive;
extern TaskHandle_t Task1;

void startOtaMode();
void handleOta();
void setupOtaInBoot();

#endif // OTA_MANAGER_H
