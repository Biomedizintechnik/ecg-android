package de.dl2ic.ecg_spo2;

import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;
import java.util.UUID;
import java.io.InputStream;

import java.io.IOException;

enum ProtocolState {
    WaitForFrameStart,
    FrameStarted,
    WaitForOptionalData
}

public class MainActivity extends Activity {
    private GLSurfaceView glView;
    private GraphRenderer renderer;
    private BluetoothSocket socket;
    private BluetoothAdapter adapter;
    private GraphBuffer graphBuffer;
    private DataTransport transport;
    private boolean connected;
    private ProtocolState protocolState;

    private TextView statusText;
    private TextView rateText;
    private TextView spo2Text;
    private ImageView heartImage;
    private Handler updateHandler;

    private int pulse;
    private int spo2;

    private ToneGenerator toneGenerator;

    private static final UUID SSP_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.mainview);

        graphBuffer = new GraphBuffer(500);
        renderer = new GraphRenderer(graphBuffer);
        toneGenerator = new ToneGenerator(100, 800);
        adapter = BluetoothAdapter.getDefaultAdapter();

        glView = (GLSurfaceView)findViewById(R.id.glView);
        glView.setEGLContextClientVersion(2);
        glView.setEGLConfigChooser(new MultisampleConfigChooser());
        glView.setRenderer(renderer);

        statusText = (TextView)findViewById(R.id.statusText);
        rateText = (TextView)findViewById(R.id.rateText);
        spo2Text = (TextView)findViewById(R.id.spo2Text);
        heartImage = (ImageView)findViewById(R.id.heartImage);
        updateHandler = new Handler();

        //transport = new DataTransport();
        //transport.start();
        //transport.connect("10.0.0.19", 43531);

        protocolState = ProtocolState.WaitForFrameStart;
        setConnected(false);

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    connect();
                    if (connected) {
                        listenForData();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.finish();
        System.exit(0);
    }

    private void connect() {
        disconnect();

        adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null || !adapter.isEnabled()) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Bluetooth disabled!", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        BluetoothDevice ecgDevice = null;

        for (BluetoothDevice device : bondedDevices) {
            if (device.getName().startsWith("ECG")) {
                ecgDevice = device;
            }
        }

        if (ecgDevice == null) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "No ECG device found!", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        else {
            final BluetoothDevice device = ecgDevice;
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        try {
            socket = ecgDevice.createRfcommSocketToServiceRecord(SSP_UUID);
            socket.connect();
            setConnected(true);
        }
        catch (IOException e) {
            disconnect();
        }
    }

    private void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            }
            catch (IOException e) {
                // ignore
            }
        }
        socket = null;
        setConnected(false);
    }

    private final Runnable beatOn = new Runnable() {
        public void run() {
            rateText.setText(String.valueOf(pulse));
            heartImage.setVisibility(View.VISIBLE);
            toneGenerator.play();
        }
    };

    private final Runnable beatOff = new Runnable() {
        public void run() {
            heartImage.setVisibility(View.INVISIBLE);
        }
    };

    private final Runnable spo2Update = new Runnable() {
        public void run() {
            spo2Text.setText(String.valueOf(spo2));
        }
    };

    private void pulseUpdate(int pulse) {
        this.pulse = pulse;
        updateHandler.post(beatOn);
        updateHandler.postDelayed(beatOff, 100);
    }


    private void spo2Update(int spo2) {
        this.spo2 = spo2;
        updateHandler.post(spo2Update);
    }

    private void listenForData() {
        InputStream stream;
        try {
            stream = socket.getInputStream();
        }
        catch (IOException e) {
            return;
        }

        while (true) {
            int byte1, byte2;
            switch (protocolState) {
                case WaitForFrameStart:
                    try { byte1 = stream.read(); }
                    catch (IOException e) { return; }
                    if (byte1 == 0b10101010) protocolState = ProtocolState.FrameStarted;
                    break;

                case FrameStarted:
                    int ecgCurve, spo2Curve;
                    try {
                        ecgCurve = this.read14Bit(stream);
                        spo2Curve = this.read14Bit(stream);
                    }
                    catch (IOException e) { return; }

                    if (ecgCurve < 0 || spo2Curve < 0) {
                        protocolState = ProtocolState.WaitForFrameStart;
                        Log.d("ECG", "Invalid ECG/SpO2 value");
                        break;
                    }

                    graphBuffer.insertEcgValue(ecgCurve);
                    graphBuffer.insertSpo2Value(spo2Curve);
                    graphBuffer.next();

                    protocolState = ProtocolState.WaitForOptionalData;
                    break;

                case WaitForOptionalData:
                    try { byte1 = stream.read(); }
                    catch (IOException e) { return; }
                    if (byte1 == 0b10101010) {
                        protocolState = ProtocolState.FrameStarted;
                        break;
                    }
                    else if ((byte1 & 0b11000000) != 0b11000000) {
                        protocolState = ProtocolState.WaitForFrameStart;
                        break;
                    }

                    int type = (byte1 & 0b00111000) >> 3;

                    try { byte2 = stream.read(); }
                    catch (IOException e) { return; }
                    if ((byte2 & 0b10000000) != 0) {
                        protocolState = ProtocolState.WaitForFrameStart;
                        break;
                    }

                    int value = ((byte1 & 0b00000111) << 7) | byte2;

                    switch (type) {
                        case 0b000:
                            pulseUpdate(value);
                            break;

                        case 0b001:
                            spo2Update(value);
                            break;
                    }
                    break;
            }
            //if (transport != null) {
            //     transport.put(new byte[]{(byte) byte1, (byte) byte2});
            //}
        }
    }

    private int read14Bit(InputStream stream) throws IOException {
        int byte1 = stream.read();
        int byte2 = stream.read();
        if ((byte1 & 0b10000000) != 0 || (byte2 & 0b10000000) != 0) return -1;
        return byte1 << 7 | byte2;
    }

    private void setConnected(boolean status) {
        connected = status;
        runOnUiThread(new Runnable() {
            public void run() {
                if (connected) {
                    statusText.setText("Connected");
                }
                else {
                    statusText.setText("Disconnected");
                }
            }
        });
    }
}
