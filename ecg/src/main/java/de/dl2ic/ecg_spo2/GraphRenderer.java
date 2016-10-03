package de.dl2ic.ecg_spo2;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class GraphRenderer implements GLSurfaceView.Renderer {
    private GraphBuffer buffer;
    private int ecgLineProgram;
    private int ecgPositionHandle;
    private int spo2LineProgram;
    private int spo2PositionHandle;

    public GraphRenderer(GraphBuffer buffer) {
        super();
        this.buffer = buffer;
    }

    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        int ecgLineVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, lineVertexShaderCode);
        int spo2LineVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, lineVertexShaderCode);
        int ecgLineFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, ecgLineFragmentShaderCode);
        int spo2LineFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, spo2LineFragmentShaderCode);

        {
            ecgLineProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(ecgLineProgram, ecgLineVertexShader);
            GLES20.glAttachShader(ecgLineProgram, ecgLineFragmentShader);
            GLES20.glLinkProgram(ecgLineProgram);
            ecgPositionHandle = GLES20.glGetAttribLocation(ecgLineProgram, "vPosition");
        }

        {
            spo2LineProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(spo2LineProgram, spo2LineVertexShader);
            GLES20.glAttachShader(spo2LineProgram, spo2LineFragmentShader);
            GLES20.glLinkProgram(spo2LineProgram);
            spo2PositionHandle = GLES20.glGetAttribLocation(spo2LineProgram, "vPosition");
        }
    }

    public void onDrawFrame(GL10 unused) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glLineWidth(4.0f);

        GLES20.glUseProgram(ecgLineProgram);
        GLES20.glVertexAttribPointer(ecgPositionHandle, 3, GLES20.GL_FLOAT, false, 12, buffer.getEcgByteBuffer());
        GLES20.glEnableVertexAttribArray(ecgPositionHandle);

        int pos = buffer.getPos();
        int size = buffer.getSize();

        if (pos > 0) {
             GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, pos);
        }
        pos += 10; // space between new and old values
        if (pos < size) {
            GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, pos, size - pos);
        }

        GLES20.glUseProgram(spo2LineProgram);
        GLES20.glVertexAttribPointer(spo2PositionHandle, 3, GLES20.GL_FLOAT, false, 12, buffer.getSpo2ByteBuffer());
        GLES20.glEnableVertexAttribArray(spo2PositionHandle);

        pos = buffer.getPos();
        size = buffer.getSize();

        if (pos > 0) {
            GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, pos);
        }
        pos += 10; // space between new and old values
        if (pos < size) {
            GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, pos, size - pos);
        }
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width , height);
    }

    private final String lineVertexShaderCode =
            "attribute vec4 vPosition;" +
                    "void main(){" +
                    " gl_Position = vPosition;" +
                    "}";

    private final String ecgLineFragmentShaderCode =
            "precision mediump float;" +
                    "void main(){" +
                    " gl_FragColor = vec4 (0, 0.6941, 0.6745, 1.0);" +
                    "}";

    private final String spo2LineFragmentShaderCode =
            "precision mediump float;" +
                    "void main(){" +
                    " gl_FragColor = vec4 (1.0, 0.663, 0.0, 1.0);" +
                    "}";
}