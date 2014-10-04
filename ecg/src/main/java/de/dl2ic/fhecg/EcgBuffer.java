package de.dl2ic.fhecg;

import java.nio.IntBuffer;

class EcgBuffer {
    private IntBuffer buffer;
    private int bounds;
    private int pos = 0;
    private int size = 0;

    private int min = 0;
    private int max = 4095;
    private float scaler = 1.0f;

    public EcgBuffer(int bounds) {
        this.bounds = bounds;
        buffer = IntBuffer.allocate(bounds);
    }

    public int process(int value) {
        buffer.put(pos, value);
        size = Math.min(size + 1, bounds);
        pos = (pos + 1) % bounds;
        return (int)((float)(value - this.min) * scaler);
    }

    public void scale() {
        int min = 4095;
        int max = 0;

        for (int i = 0; i < size; i++) {
            int val = buffer.get(i);

            if (val < min) min = val;
            if (val > max) max = val;
        }

        this.min = Math.max(200, min);
        this.max = max;
        this.scaler = (4095.0f/((float)(this.max - this.min)))*0.95f;
    }
}
