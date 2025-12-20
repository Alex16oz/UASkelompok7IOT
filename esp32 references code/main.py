import network
import urequests
import json
import time
import machine
import ntptime
import gc
from machine import Pin, PWM, time_pulse_us
from hx711 import HX711

# --- KONFIGURASI WIFI & FIREBASE ---
WIFI_SSID = "OTT"       # Ganti dengan WiFi Anda.
WIFI_PASS = "asdfghjkl" # Ganti dengan Password Anda
# URL Firebase (tanpa akhiran slash)
FIREBASE_BASE_URL = "https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app/IOT"

# --- KONFIGURASI HARDWARE ---
# 1. Servo (Pintu Pakan)
servo_pin = PWM(Pin(21), freq=50) # Pin 21 sesuai referensi Anda
SERVO_OPEN_ANGLE = 120
SERVO_CLOSE_ANGLE = 0

# 2. Ultrasonic (Sensor Kedalaman Pakan)
trig = Pin(23, Pin.OUT) # Pin 23
echo = Pin(22, Pin.IN)  # Pin 22

# 3. Load Cell (Sensor Berat Tempat Makan)
# DT=19, SCK=18 sesuai referensi Anda
hx = HX711(18, 19)
# PENTING: Anda harus mencari nilai ini dengan kalibrasi manual (baca nilai raw / berat benda diketahui)
hx.set_scale(400.0)
hx.tare() # Nol-kan timbangan saat nyala pertama kali

# 4. LED Indikator
led = Pin(2, Pin.OUT)

# --- KONFIGURASI LOGIKA ---
TARGET_WEIGHT = 100  # Target berat pakan di mangkok (gram) saat memberi makan
UTC_OFFSET = 7 * 3600 # Waktu Indonesia Barat (WIB) adalah UTC+7

# Variabel Global
last_fed_minute = -1

# --- FUNGSI-FUNGSI ---

def connect_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if not wlan.isconnected():
        print('Menghubungkan ke WiFi...')
        wlan.connect(WIFI_SSID, WIFI_PASS)
        timeout = 0
        while not wlan.isconnected() and timeout < 20:
            time.sleep(1)
            timeout += 1
            print('.', end='')
    print('\nWiFi Terhubung:', wlan.ifconfig())

    # Sinkronisasi Waktu (NTP)
    try:
        ntptime.settime()
        print("Waktu tersinkronisasi")
    except:
        print("Gagal sinkronisasi waktu")

def set_servo(angle):
    # Mengubah sudut (0-180) menjadi duty cycle (sekitar 1638-8192 untuk ESP32)
    # Rumus bisa bervariasi tergantung jenis servo, sesuaikan jika perlu
    duty = int((angle / 180) * (8192 - 1638) + 1638)
    servo_pin.duty_u16(duty)

def get_distance():
    try:
        trig.value(0)
        time.sleep_us(2)
        trig.value(1)
        time.sleep_us(10)
        trig.value(0)
        duration = time_pulse_us(echo, 1, 30000)
        if duration > 0:
            return (duration * 0.0343) / 2
        return 0
    except Exception as e:
        print("Error Ultrasonic:", e)
        return 0

def get_weight_avg():
    # Ambil rata-rata 5 bacaan agar stabil
    try:
        val = hx.get_units(5)
        return max(0, val) # Hindari nilai negatif
    except:
        return 0

def feed_pet():
    print("--- MEMULAI PEMBERIAN PAKAN ---")

    # 1. Buka Pintu
    set_servo(SERVO_OPEN_ANGLE)
    print("Pintu Terbuka")

    # 2. Monitoring Berat (Looping sampai target tercapai atau timeout)
    start_time = time.time()
    timeout = 10 # Maksimal 10 detik pintu terbuka (keamanan)

    while (time.time() - start_time) < timeout:
        current_weight = get_weight_avg()
        print(f"Berat saat ini: {current_weight:.1f} gr")

        if current_weight >= TARGET_WEIGHT:
            print("Target berat tercapai!")
            break
        time.sleep(0.5)

    # 3. Tutup Pintu
    set_servo(SERVO_CLOSE_ANGLE)
    print("Pintu Tertutup")
    print("--- SELESAI ---")

def firebase_get(path):
    try:
        res = urequests.get(f"{FIREBASE_BASE_URL}/{path}.json")
        if res.status_code == 200:
            data = res.json()
            res.close()
            return data
        res.close()
    except Exception as e:
        print(f"Error GET {path}:", e)
    return None

def firebase_put(path, data):
    try:
        res = urequests.put(f"{FIREBASE_BASE_URL}/{path}.json", json=data)
        res.close()
    except Exception as e:
        print(f"Error PUT {path}:", e)

# --- MAIN LOOP ---
connect_wifi()
set_servo(SERVO_CLOSE_ANGLE) # Pastikan pintu tertutup saat mulai

print("Sistem Siap...")

while True:
    try:
        # 1. Cek Koneksi & Waktu
        current_time = time.localtime(time.time() + UTC_OFFSET)
        curr_hour = current_time[3]
        curr_min = current_time[4]

        # 2. Ambil Data Kontrol & Jadwal dari Firebase
        control_data = firebase_get("control") # Ambil folder control sekaligus
        schedule_data = firebase_get("schedule")

        is_active = False
        feed_now = False

        if control_data:
            is_active = control_data.get("is_active", False)
            feed_now = control_data.get("feed_now", False)

        sched_hour = -1
        sched_min = -1
        if schedule_data:
            sched_hour = schedule_data.get("hour", -1)
            sched_min = schedule_data.get("minute", -1)

        # 3. Logika Utama
        if is_active:
            # Baca Sensor
            jarak = get_distance()
            berat = get_weight_avg()
            print(f"Jarak: {jarak:.1f} cm | Berat: {berat:.1f} gr | Jam: {curr_hour}:{curr_min}")

            # Kirim Data Sensor ke Firebase
            firebase_put("sensor", {"distance": jarak, "weight": berat})

            # --- LOGIKA PEMBERIAN MAKAN ---
            should_feed = False

            # A. Cek Jadwal Otomatis
            # Pastikan hanya makan sekali dalam menit tersebut (agar servo tidak buka-tutup terus menerus selama 1 menit)
            if (curr_hour == sched_hour and curr_min == sched_min and curr_min != last_fed_minute):
                print("Waktunya makan (Jadwal)!")
                should_feed = True
                last_fed_minute = curr_min # Tandai menit ini sudah diberi makan

            # B. Cek Tombol Manual (Feed Now)
            if feed_now:
                print("Perintah Manual (Feed Now) diterima!")
                should_feed = True
                # Matikan trigger feed_now di Firebase
                firebase_put("control/feed_now", False)

            # Eksekusi
            if should_feed:
                feed_pet()

                # Opsional: Kirim history setelah makan
                try:
                    urequests.post(f"{FIREBASE_BASE_URL}/history.json", json={"val": jarak, "ts": time.time()}).close()
                except:
                    pass

        else:
            print("Sistem Standby (OFF)...")
            time.sleep(2)

    except Exception as e:
        print("Global Error:", e)
        # Coba reconnect jika error jaringan
        try:
            if not network.WLAN(network.STA_IF).isconnected():
                connect_wifi()
        except:
            pass

    gc.collect() # Bersihkan memori
    time.sleep(1) # Delay loop utama
