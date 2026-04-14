#ifndef DOOR_CONTROL_H
#define DOOR_CONTROL_H

#include <Arduino.h>
#include "Pins.h"

// ------------------ Глобальные переменные ------------------
extern unsigned long doorOpenTimestamp;

// ------------------ Инициализация пинов ------------------
void initPins();

// ------------------ Открытие двери ------------------
void openDoor(int pause, String source);

// ------------------ Закрытие двери ------------------
void closedDoor();

// ------------------ Сигнал когда все на улице ------------------
bool allUsersOutside();
void playThreeBeeps();

#endif // DOOR_CONTROL_H
