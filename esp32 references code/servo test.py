from machine import Pin, PWM
import time

# Setup Servo on Pin 21
servo = PWM(Pin(21))
servo.freq(50)

def set_servo_angle(angle):
    # Map angle 0-180 to duty cycle 1638-8192
    duty = int((angle / 180) * (8192 - 1638) + 1638)
    servo.duty_u16(duty)

print("Starting 0 to 120 Loop...")

try:
    while True:
        # "Open"
        print("Opening (120°)")
        set_servo_angle(120)
        time.sleep(2)

        # "Close" - Move to 0 degrees
        print("Closing (0°)")
        set_servo_angle(0)
        time.sleep(2)

except KeyboardInterrupt:
    servo.deinit()
    print("Stopped")
