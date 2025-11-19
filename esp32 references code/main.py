import network
import urequests
import json
import time
import machine
from machine import Pin, time_pulse_us

# --- USER CONFIGURATION ---
#WIFI_SSID = "POLINEMA"
#WIFI_PASS = "polinemajoss"
WIFI_SSID = "OTT"
WIFI_PASS = "asdfghjkl"

# URL Firebase
FIREBASE_BASE_URL = "https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app/IOT"

# --- SETUP LED (JANGAN UBAH) ---
led = machine.Pin(2, machine.Pin.OUT)

# --- SETUP ULTRASONIC SENSOR ---
# Trig = Pin 23, Echo = Pin 22
trig = Pin(23, Pin.OUT)
echo = Pin(22, Pin.IN)

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

# --- FUNGSI BACA JARAK ---
def get_distance():
    # Pastikan trig low dulu
    trig.value(0)
    time.sleep_us(2)

    # Kirim sinyal 10 microsecond
    trig.value(1)
    time.sleep_us(10)
    trig.value(0)

    # Baca durasi pulse pada echo (timeout 30ms = 30000us)
    # Jika timeout, time_pulse_us mengembalikan -1 atau -2
    duration = time_pulse_us(echo, 1, 30000)

    if duration > 0:
        # Hitung jarak (cm) = (durasi * kecepatan suara) / 2
        # Kecepatan suara = 0.0343 cm/us
        distance_cm = (duration * 0.0343) / 2
        return distance_cm
    else:
        return 0

# --- MAIN EXECUTION ---
connect_wifi()

print("System Ready. Starting Loop...")

while True:
    try:
        # --- BAGIAN 1: BACA STATUS LED (TETAP/TIDAK DIUBAH) ---
        try:
            response = urequests.get(f"{FIREBASE_BASE_URL}/led.json")
            if response.status_code == 200:
                led_status = response.json()
                if led_status is None:
                    led_status = False

                # Kontrol LED Fisik
                if led_status == True:
                    led.value(1)
                else:
                    led.value(0)
            response.close()
        except Exception as e_led:
            print("Error membaca LED:", e_led)

        # --- BAGIAN 2: BACA & KIRIM DATA ULTRASONIC ---
        jarak = get_distance()
        print(f"Jarak terukur: {jarak:.2f} cm")

        data_payload = {
            "distance": jarak
        }

        try:
            # Kirim ke path /sensor.json
            response = urequests.put(f"{FIREBASE_BASE_URL}/sensor.json", json=data_payload)
            if response.status_code == 200:
                print("Data jarak terkirim ke Firebase.")
            else:
                print("Gagal kirim sensor:", response.text)
            response.close()
        except Exception as e_sensor:
            print("Error kirim sensor:", e_sensor)

    except Exception as e:
        print("Global Error:", e)

    # Delay 2 detik sesuai permintaan
    time.sleep(2)
