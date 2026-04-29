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
        .card { background: #1E293B; padding: 2.5rem; border-radius: 1.5rem; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3); width: 90%; max-width: 400px; text-align: center; border: 1px solid #334155; position: relative; }
        h2 { color: #38BDF8; margin-bottom: 0.5rem; font-size: 1.5rem; }
        p { color: #94A3B8; margin-bottom: 2rem; font-size: 0.9rem; }
        .file-label { display: block; background: #0F172A; padding: 1rem; border-radius: 0.75rem; border: 1px solid #334155; color: #94A3B8; cursor: pointer; transition: all 0.2s; text-align: center; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 1.5rem; }
        .file-label:hover { border-color: #38BDF8; color: #F8FAFC; }
        input[type=file] { display: none; }
        #btn { background: #38BDF8; color: #0F172A; border: none; padding: 1rem; border-radius: 0.75rem; cursor: pointer; font-size: 1rem; font-weight: bold; width: 100%; transition: all 0.2s; }
        #btn:hover { background: #7DD3FC; transform: translateY(-1px); }
        #btn:disabled { background: #334155; color: #475569; cursor: not-allowed; transform: none; }
        #reboot-btn { background: #F87171; color: #0F172A; border: none; padding: 0.6rem 0.8rem; border-radius: 0.75rem; cursor: pointer; position: absolute; bottom: 0.8rem; right: 0.8rem; transition: all 0.2s; display: flex; align-items: center; gap: 0.4rem; box-shadow: 0 4px 15px rgba(248, 113, 113, 0.4); }
        #reboot-btn:hover { background: #EF4444; color: white; transform: translateY(-1px); box-shadow: 0 6px 20px rgba(248, 113, 113, 0.5); }
        #reboot-btn svg { width: 1.2rem; height: 1.2rem; stroke-width: 3; }
        #reboot-btn span { font-weight: bold; font-size: 0.7rem; letter-spacing: 0.05em; }
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
            <button id='reboot-btn' onclick='rebootDevice()' title='Reboot Controller'>
                <svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M23 4v6h-6'></path><path d='M20.49 15a9 9 0 1 1-2.12-9.36L23 10'></path></svg>
                <span>REBOOT</span>
            </button>
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
            <p>There was a problem with the update.<br>The controller is restarting in normal mode.</p>
            <p id='err-desc' style='font-size: 0.7rem; color: #64748b; margin-top: 1rem;'></p>
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

        function rebootDevice() {
            if (!confirm('Are you sure you want to reboot the controller?')) return;
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '/reboot', true);
            xhr.send();
            document.getElementById('upload-form').style.display = 'none';
            document.getElementById('success-msg').style.display = 'block';
            document.getElementById('success-msg').innerHTML = "<div class='success-icon'>🔄</div><h2>REBOOTING...</h2><p>The controller is restarting.<br>Please wait 10 seconds.</p>";
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

    String savedSsid = loadWifiSsid();
    String savedPass = loadWifiPass();
    WiFi.begin(savedSsid.c_str(), savedPass.c_str());

    Serial.print("Connecting to Wi-Fi: " + savedSsid);
    int retry = 0;
    while (WiFi.status() != WL_CONNECTED && retry < 30) {
        delay(500);
        Serial.print(".");
        retry++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\nConnected! IP: " + WiFi.localIP().toString());
    } else {
        Serial.println("\nFailed to connect to Wi-Fi. Rebooting to normal mode...");
        delay(2000);
        ESP.restart();
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

    server.on("/reboot", HTTP_GET, []() {
        server.send(200, "text/plain", "OK. Rebooting...");
        delay(500);
        ESP.restart();
    });

    server.begin();

    // Запуск NetBIOS для доступа по имени http://LAB
    NBNS.begin("LAB");
    Serial.println("NetBIOS started: http://LAB");

    Serial.println("Web Update Server ready at http://" + WiFi.localIP().toString());
}

void handleOta() {
    if (otaModeActive) {
        static int reconnectAttempts = 0;
        static unsigned long lastReconnectAttempt = 0;

        if (WiFi.status() != WL_CONNECTED) {
            unsigned long now = millis();
            if (now - lastReconnectAttempt > 5000) {
                lastReconnectAttempt = now;
                reconnectAttempts++;
                Serial.printf("Потеряно соединение Wi-Fi. Попытка переподключения %d из 5...\n", reconnectAttempts);

                if (reconnectAttempts > 5) {
                    Serial.println("Не удалось восстановить связь. Возврат в нормальный режим...");
                    delay(1000);
                    ESP.restart();
                }

                WiFi.disconnect();
                String ssid = loadWifiSsid();
                String pass = loadWifiPass();
                WiFi.begin(ssid.c_str(), pass.c_str());
            }
            return;
        } else {
            reconnectAttempts = 0; // Сброс при успешном соединении
        }

        server.handleClient(); // Обслуживаем Web-сервер

        static unsigned long lastCheck = 0;
        if (millis() - lastCheck > 5000) {
            lastCheck = millis();
            Serial.print("Web Update Ready! IP: ");
            Serial.println(WiFi.localIP());
        }
    }
}
