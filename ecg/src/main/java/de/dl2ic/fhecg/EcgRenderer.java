package de.dl2ic.fhecg;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class EcgRenderer implements GLSurfaceView.Renderer {
    private EcgGraphBuffer buffer;
    private int lineProgram;

    public EcgRenderer(EcgGraphBuffer buffer) {
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
        GLES20.glClearColor(0.023529f, 0.239215f, 0.3647058f, 1.0f);

        int lineVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, lineVertexShaderCode);
        int lineFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, lineFragmentShaderCode);

        lineProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(lineProgram, lineVertexShader);
        GLES20.glAttachShader(lineProgram, lineFragmentShader);
        GLES20.glLinkProgram(lineProgram);

        int positionHandle = GLES20.glGetAttribLocation(lineProgram, "vPosition");

        GLES20.glUseProgram(lineProgram);
        GLES20.glLineWidth(3.0f);
        GLES20.glVertexAttribPointer(positionHandle , 3, GLES20.GL_FLOAT, false, 12, buffer.getByteBuffer());
        GLES20.glEnableVertexAttribArray(positionHandle);
    }

    public void onDrawFrame(GL10 unused) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        int pos = buffer.getPos();
        int size = buffer.getSize();

        if (pos > 0) {
            GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, pos);
        }
        pos += 5; // space between new and old values
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

    private final String lineFragmentShaderCode =
            "precision mediump float;" +
                    "void main(){" +
                    " gl_FragColor = vec4 (0.6588235, 0.819607, 0.905882, 1.0);" +
                    "}";
}