package de.dl2ic.ecg_spo2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class GraphBuffer {
    private ByteBuffer ecgBuffer;
    private ByteBuffer spo2Buffer;
    private int pos;
    private int bounds;
    private int size;

    public GraphBuffer(int bounds) {
        this.bounds = bounds;
        this.size = 0;

        // 3 coordinates, each a 4-byte float
        ecgBuffer = ByteBuffer.allocateDirect(bounds * 4 * 3);
        ecgBuffer.order(ByteOrder.nativeOrder());

        spo2Buffer = ByteBuffer.allocateDirect(bounds * 4 * 3);
        spo2Buffer.order(ByteOrder.nativeOrder());

        pos = 0;
    }

    public void insertEcgValue(int value) {
        float x = ((float) (pos * 2) / bounds) - 1.0f;
        //float y = ((float) (value * 2) / 4095.0f) - 1.0f;
        float y = ((float)value / 4095.0f);
        float z = 0.0f;

        ecgBuffer.position(pos * 4 * 3);
        ecgBuffer.putFloat(x);
        ecgBuffer.putFloat(y);
        ecgBuffer.putFloat(z);
    }

    public void insertSpo2Value(int value) {
        float x = ((float) (pos * 2) / bounds) - 1.0f;
        //float y = ((float) (value * 2) / 4095.0f) - 1.0f;
        float y = ((float)value  / 4095.0f) - 1.0f;
        float z = 0.0f;

        spo2Buffer.position(pos * 4 * 3);
        spo2Buffer.putFloat(x);
        spo2Buffer.putFloat(y);
        spo2Buffer.putFloat(z);
    }

    public void next() {
        pos = (pos + 1) % bounds;
        size = Math.min(size + 1, bounds);
    }


    public ByteBuffer getEcgByteBuffer() {
        ByteBuffer readOnlyBuffer = ecgBuffer.asReadOnlyBuffer();
        readOnlyBuffer.position(0);
        return readOnlyBuffer;
    }

    public ByteBuffer getSpo2ByteBuffer() {
        ByteBuffer readOnlyBuffer = spo2Buffer.asReadOnlyBuffer();
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