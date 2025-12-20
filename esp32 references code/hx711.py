# Simpan file ini dengan nama: hx711.py
from machine import Pin
import time

class HX711:
    def __init__(self, pd_sck, dout, gain=128):
        self.p_sck = Pin(pd_sck, Pin.OUT)
        self.p_dout = Pin(dout, Pin.IN)
        self.p_sck.value(0)
        self.GAIN = 0
        self.OFFSET = 0
        self.SCALE = 1
        self.set_gain(gain)

    def set_gain(self, gain):
        if gain is 128:
            self.GAIN = 1
        elif gain is 64:
            self.GAIN = 3
        elif gain is 32:
            self.GAIN = 2
        self.read()

    def is_ready(self):
        return self.p_dout.value() == 0

    def read(self):
        while not self.is_ready():
            pass
        count = 0
        for i in range(24):
            self.p_sck.value(1)
            count = count << 1
            self.p_sck.value(0)
            if self.p_dout.value():
                count += 1
        self.p_sck.value(1)
        count = count ^ 0x800000
        self.p_sck.value(0)
        for i in range(self.GAIN - 1):
            self.p_sck.value(1)
            self.p_sck.value(0)
        return count

    def read_average(self, times=3):
        sum = 0
        for i in range(times):
            sum += self.read()
        return sum / times

    def get_value(self, times=3):
        return self.read_average(times) - self.OFFSET

    def get_units(self, times=3):
        return self.get_value(times) / self.SCALE

    def tare(self, times=15):
        sum = self.read_average(times)
        self.set_offset(sum)

    def set_scale(self, scale):
        self.SCALE = scale

    def set_offset(self, offset):
        self.OFFSET = offset
