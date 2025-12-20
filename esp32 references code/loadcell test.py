from hx711 import HX711
import time

# 1. Initialize the HX711
# Parameters: (SCK Pin, DT Pin)
hx = HX711(18, 19)

# 2. CALIBRATION (Important!)
# By default, the scale is 1. You must calculate your own ratio.
# Formula: ratio = (Raw Value / Known Weight)
# For now, we will leave it as 1 so you can see raw numbers.
hx.set_scale(1)

# 3. Tare (Zero the scale)
print("Taring... Please remove any weight from the scale.")
hx.tare()
print("Tare done! Ready to weigh.")

# 4. Main Loop
while True:
    try:
        # Read the value (average of 5 readings for stability)
        val = hx.get_units(5)

        # Print the value
        print("Weight: ", val)

        # Wait 0.5 seconds before next read
        time.sleep(0.5)

    except OSError:
        print("Sensor error - check wiring")
