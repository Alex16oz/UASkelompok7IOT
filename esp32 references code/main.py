import network
import urequests
import json
import time
import machine

# --- USER CONFIGURATION ---
WIFI_SSID = "POLINEMA"
WIFI_PASS = "polinemajoss"

# URL Firebase (Perhatikan kita menghapus .json di akhir base URL untuk fleksibilitas)
FIREBASE_BASE_URL = "https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app/IOT"

# --- SETUP LED ---
# Pin 2 biasanya adalah Built-In LED pada board ESP32
# Jika menggunakan LED eksternal, ganti angka 2 dengan nomor pin GPIO yang digunakan
led = machine.Pin(2, machine.Pin.OUT)

# --- CONNECT TO WIFI ---
def connect_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if not wlan.isconnected():
        print('Connecting to network...')
        wlan.connect(WIFI_SSID, WIFI_PASS)
        while not wlan.isconnected():
            pass
    print('Network config:', wlan.ifconfig())

# --- MAIN EXECUTION ---
connect_wifi()

print("System Ready. Starting Loop...")

while True:
    try:
        # --- BAGIAN 1: BACA STATUS LED (GET) ---
        # Membaca path khusus /led.json
        try:
            response = urequests.get(f"{FIREBASE_BASE_URL}/led.json")
            if response.status_code == 200:
                # Parse response
                led_status = response.json()

                # Firebase kadang mengembalikan None jika data belum pernah diset
                if led_status is None:
                    led_status = False

                print(f"Status LED dari Firebase: {led_status}")

                # Kontrol LED Fisik
                if led_status == True:
                    led.value(1) # Nyalakan
                else:
                    led.value(0) # Matikan
            else:
                print("Gagal mengambil status LED")
            response.close()
        except Exception as e_led:
            print("Error membaca LED:", e_led)

        # --- BAGIAN 2: KIRIM DATA SENSOR (PUT) ---
        # Kita simpan data sensor di path /sensor.json agar terpisah dari led
        # Simulasi data sensor
        data_payload = {
            "temperature": 25.5,
            "humidity": 60,
            "uptime": time.ticks_ms() / 1000
        }

        print("Mengirim data sensor...")
        try:
            # Menggunakan .put ke path /sensor.json
            response = urequests.put(f"{FIREBASE_BASE_URL}/sensor.json", json=data_payload)
            if response.status_code == 200:
                print("Data sensor terkirim.")
            else:
                print("Gagal kirim sensor:", response.text)
            response.close()
        except Exception as e_sensor:
            print("Error kirim sensor:", e_sensor)

    except Exception as e:
        print("Global Error:", e)


    time.sleep(3)
