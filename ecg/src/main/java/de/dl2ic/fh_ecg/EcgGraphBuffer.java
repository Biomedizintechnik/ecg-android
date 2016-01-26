package de.dl2ic.fh_ecg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class EcgGraphBuffer {
    private ByteBuffer buffer;
    private int pos;
    private int bounds;
    private int size;
    private int scaler;
    private int counter;

    public EcgGraphBuffer(int bounds) {
        this.bounds = bounds;
        this.size = 0;
        scaler = 1;
        counter = 0;

        // 3 coordinates, each a 4-byte float
        buffer = ByteBuffer.allocateDirect(bounds * 4 * 3);
        buffer.order(ByteOrder.nativeOrder());
        pos = 0;
    }

    public void insertValue(int value) {
        float x = ((float) (pos * 2) / bounds) - 1.0f;
        float y = ((float) (value * 2) / 4095.0f) - 1.0f;
        float z = 0.0f;

        buffer.position(pos * 4 * 3);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);

        if (counter < scaler) {
            counter++;
        } else {
            pos = (pos + 1) % bounds;
            size = Math.min(size + 1, bounds);
            counter = 0;
        }
    }

    public ByteBuffer getByteBuffer() {
        ByteBuffer readOnlyBuffer = buffer.asReadOnlyBuffer();
        readOnlyBuffer.position(0);
        return readOnlyBuffer;
    }

    public int getSize() {
        return size;
    }

    public int getPos() {
        return pos;
    }
}