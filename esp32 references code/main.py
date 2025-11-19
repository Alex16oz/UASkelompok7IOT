import network
import urequests
import json
import time
import machine
import gc  # Import Garbage Collector untuk manajemen memori
from machine import Pin, time_pulse_us

# --- KONFIGURASI WIFI ---
WIFI_SSID = "OTT"       # Ganti dengan WiFi Anda.
WIFI_PASS = "asdfghjkl" # Ganti dengan Password Anda

# URL Firebase (Pastikan diakhiri dengan benar)
FIREBASE_BASE_URL = "https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app/IOT"

# --- SETUP HARDWARE ---
led = machine.Pin(2, machine.Pin.OUT)
trig = Pin(23, Pin.OUT)
echo = Pin(22, Pin.IN)

def connect_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if not wlan.isconnected():
        print('Connecting to network...')
        wlan.connect(WIFI_SSID, WIFI_PASS)
        timeout = 0
        while not wlan.isconnected() and timeout < 20:
            time.sleep(1)
            timeout += 1
            print('.', end='')
    print('\nNetwork config:', wlan.ifconfig())

def get_distance():
    try:
        trig.value(0)
        time.sleep_us(2)
        trig.value(1)
        time.sleep_us(10)
        trig.value(0)

        # Timeout ditingkatkan sedikit untuk stabilitas
        duration = time_pulse_us(echo, 1, 30000)

        if duration > 0:
            return (duration * 0.0343) / 2
        else:
            return 0
    except Exception as e:
        print("Error sensor:", e)
        return 0

# --- EKSEKUSI UTAMA ---
connect_wifi()
print("System Ready...")

while True:
    try:
        # 1. CEK STATUS MONITOR (Tombol ON/OFF)
        is_active = False
        try:
            # Gunakan variabel response 'res' dan tutup segera
            res = urequests.get(f"{FIREBASE_BASE_URL}/control/is_active.json")
            if res.status_code == 200:
                is_active = res.json()
                if is_active is None: is_active = False
            res.close() # WAJIB DITUTUP
        except Exception as e:
            print("Error baca config:", e)

        # 2. CEK STATUS LED
        try:
            res = urequests.get(f"{FIREBASE_BASE_URL}/led.json")
            if res.status_code == 200:
                led_status = res.json()
                led.value(1 if led_status else 0)
            res.close() # WAJIB DITUTUP
        except Exception as e:
            print("Error baca LED:", e)

        # 3. UKUR DAN KIRIM DATA
        if is_active:
            jarak = get_distance()
            print(f"Mengukur: {jarak:.2f} cm")

            # Kirim hanya jika jarak valid (bukan 0) atau sesuai kebutuhan
            # A. Update Realtime
            try:
                res = urequests.put(f"{FIREBASE_BASE_URL}/sensor.json", json={"distance": jarak})
                res.close() # WAJIB DITUTUP
            except Exception as e:
                print("Gagal kirim realtime:", e)

            # B. Push History (Menggunakan POST)
            try:
                history_data = {"val": jarak, "ts": time.time()}
                res = urequests.post(f"{FIREBASE_BASE_URL}/history.json", json=history_data)
                res.close() # WAJIB DITUTUP
            except Exception as e:
                print("Gagal kirim history:", e)
        else:
            print("Standby (Monitoring OFF)...")

    except Exception as e:
        print("Global Error:", e)

    # BERSIHKAN MEMORI SETIAP LOOP
    gc.collect()
    time.sleep(2)
