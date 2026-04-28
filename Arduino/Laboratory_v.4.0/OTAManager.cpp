#include "OTAManager.h"
#include <WiFi.h>
#include <WebServer.h>
#include <NetBIOS.h>
#include <Update.h>
#include "Utils.h"
#include "BLEManager.h"
#include "DeviceStorage.h"

bool otaModeActive = false;
WebServer server(80);

// ================= WIFI CONFIGURATION =================
const char* ssid = "antago";
const char* password = "98798701";
// ======================================================

// HTML-страница обновления в современном стиле
const char* updateServerIndex = R"rawliteral(
<!DOCTYPE html>
<html lang='ru'>
<head>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>Lab Update</title>
    <style>
        body { background: #0F172A; color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
        .card { background: #1E293B; padding: 2.5rem; border-radius: 1.5rem; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3); width: 90%; max-width: 400px; text-align: center; border: 1px solid #334155; }
        h2 { color: #38BDF8; margin-bottom: 0.5rem; font-size: 1.5rem; }
        p { color: #94A3B8; margin-bottom: 2rem; font-size: 0.9rem; }
        .file-label { display: block; background: #0F172A; padding: 1rem; border-radius: 0.75rem; border: 1px solid #334155; color: #94A3B8; cursor: pointer; transition: all 0.2s; text-align: center; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 1.5rem; }
        .file-label:hover { border-color: #38BDF8; color: #F8FAFC; }
        input[type=file] { display: none; }
        #btn { background: #38BDF8; color: #0F172A; border: none; padding: 1rem; border-radius: 0.75rem; cursor: pointer; font-size: 1rem; font-weight: bold; width: 100%; transition: all 0.2s; }
        #btn:hover { background: #7DD3FC; transform: translateY(-1px); }
        #btn:disabled { background: #334155; color: #475569; cursor: not-allowed; transform: none; }
        .progress-area { display: none; margin-top: 1rem; }
        .progress-bar { background: #0F172A; height: 10px; border-radius: 5px; overflow: hidden; margin-bottom: 10px; border: 1px solid #334155; }
        .progress-fill { background: #38BDF8; height: 100%; width: 0%; transition: width 0.2s; }
        .status-text { color: #38BDF8; font-weight: bold; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 1px; }
        #success-msg, #error-msg { display: none; }
        .success-icon { color: #4ADE80; font-size: 3rem; margin-bottom: 1rem; }
        .error-icon { color: #F87171; font-size: 3rem; margin-bottom: 1rem; }
        .footer { margin-top: 2rem; font-size: 0.75rem; color: #475569; }
    </style>
</head>
<body>
    <div class='card' id='card'>
        <div id='upload-form'>
            <h2>Laboratory</h2>
            <p>Firmware Update System</p>
            <label class='file-label'>
                <span id='fname'>SELECT FIRMWARE (.BIN)</span>
                <input type='file' id='file-input' accept='.bin' onchange='document.getElementById("fname").innerHTML = this.files[0].name'>
            </label>
            <button id='btn' onclick='uploadFile()'>DOWNLOAD AND FLASH</button>
            <div class='progress-area' id='prg-area'>
                <div class='progress-bar'><div class='progress-fill' id='prg-fill'></div></div>
                <div class='status-text' id='stat'>Uploading... 0%</div>
            </div>
        </div>

        <div id='success-msg'>
            <div class='success-icon'>✓</div>
            <h2 style='color: #4ADE80'>SUCCESS!</h2>
            <p>Firmware updated successfully.<br>The controller is rebooting...</p>
            <p style='font-size: 0.8rem'>Please wait 10 seconds before reconnecting.</p>
        </div>

        <div id='error-msg'>
            <div class='error-icon'>✕</div>
            <h2 style='color: #F87171'>UPDATE FAILED</h2>
            <p id='err-desc'>Unknown error occurred</p>
            <button id='btn' style='background: #334155; color: white' onclick='location.reload()'>TRY AGAIN</button>
        </div>

        <div class='footer'>ESP32 OTA SYSTEM v4.0</div>
    </div>

    <script>
        function uploadFile() {
            var fileInput = document.getElementById('file-input');
            if (fileInput.files.length === 0) { alert('Please select a file!'); return; }

            var file = fileInput.files[0];
            var formData = new FormData();
            formData.append('update', file);

            document.getElementById('btn').disabled = true;
            document.getElementById('prg-area').style.display = 'block';

            var xhr = new XMLHttpRequest();
            xhr.open('POST', '/update', true);

            xhr.upload.onprogress = function(e) {
                if (e.lengthComputable) {
                    var percent = Math.round((e.loaded / e.total) * 100);
                    document.getElementById('prg-fill').style.width = percent + '%';
                    document.getElementById('stat').innerHTML = 'Uploading... ' + percent + '%';
                    if(percent === 100) document.getElementById('stat').innerHTML = 'Flashing... Please wait';
                }
            };

            xhr.onload = function() {
                if (xhr.status === 200 && xhr.responseText.includes('OK')) {
                    document.getElementById('upload-form').style.display = 'none';
                    document.getElementById('success-msg').style.display = 'block';
                } else {
                    showError(xhr.responseText || 'Server error');
                }
            };

            xhr.onerror = function() { showError('Connection lost during update'); };
            xhr.send(formData);
        }

        function showError(msg) {
            document.getElementById('upload-form').style.display = 'none';
            document.getElementById('error-msg').style.display = 'block';
            document.getElementById('err-desc').innerHTML = msg;
        }
    </script>
</body>
</html>
)rawliteral";

void startOtaMode() {
    Serial.println("Запрос на OTA получен. Перезагрузка в режим прошивки...");
    setOtaBootFlag(true);
    delay(500);
    ESP.restart();
}

void setupOtaInBoot() {
    setCpuFrequencyMhz(240);

    WiFi.mode(WIFI_STA);
    WiFi.setSleep(false);
    WiFi.begin(ssid, password);

    Serial.print("Connecting to Wi-Fi");
    int retry = 0;
    while (WiFi.status() != WL_CONNECTED && retry < 30) {
        delay(500);
        Serial.print(".");
        retry++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\nConnected! IP: " + WiFi.localIP().toString());
    }

    // Настройка Web-сервера для прошивки
    server.on("/", HTTP_GET, []() {
        server.send(200, "text/html", updateServerIndex);
    });

    server.on("/update", HTTP_POST, []() {
        server.sendHeader("Connection", "close");
        server.send(200, "text/plain", (Update.hasError()) ? "FAIL" : "OK. Rebooting...");
        delay(1000);
        ESP.restart();
    }, []() {
        HTTPUpload& upload = server.upload();
        if (upload.status == UPLOAD_FILE_START) {
            Serial.printf("Update: %s\n", upload.filename.c_str());
            if (!Update.begin(UPDATE_SIZE_UNKNOWN, U_FLASH)) {
                Update.printError(Serial);
            }
        } else if (upload.status == UPLOAD_FILE_WRITE) {
            if (Update.write(upload.buf, upload.currentSize) != upload.currentSize) {
                Update.printError(Serial);
            }
        } else if (upload.status == UPLOAD_FILE_END) {
            if (Update.end(true)) {
                Serial.printf("Update Success: %u bytes\n", upload.totalSize);
            } else {
                Update.printError(Serial);
            }
        }
    });

    server.begin();

    // Запуск NetBIOS для доступа по имени http://LABORATORY
    NBNS.begin("LAB");
    Serial.println("NetBIOS started: http://LABORATORY");

    Serial.println("Web Update Server ready at http://" + WiFi.localIP().toString());
}

void handleOta() {
    if (otaModeActive) {
        server.handleClient(); // Обслуживаем Web-сервер

        static unsigned long lastCheck = 0;
        if (millis() - lastCheck > 5000) {
            lastCheck = millis();
            if (WiFi.status() == WL_CONNECTED) {
                Serial.print("Web Update Ready! IP: ");
                Serial.println(WiFi.localIP());
            } else {
                Serial.println("Wi-Fi connecting...");
            }
        }
    }
}
