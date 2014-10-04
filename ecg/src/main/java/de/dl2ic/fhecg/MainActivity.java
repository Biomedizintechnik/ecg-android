package de.dl2ic.fhecg;

import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
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

public class MainActivity extends Activity {
    private GLSurfaceView glView;
    private EcgRenderer renderer;
    private BluetoothSocket socket;
    private BluetoothAdapter adapter;
    private EcgGraphBuffer graphBuffer;
    private boolean connected;

    private TextView statusText;
    private TextView rateText;
    private ImageView heartImage;

    private ToneGenerator toneGenerator;

    private static final UUID SSP_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.mainview);

        graphBuffer = new EcgGraphBuffer(500);
        renderer = new EcgRenderer(graphBuffer);
        toneGenerator = new ToneGenerator(100, 800);
        adapter = BluetoothAdapter.getDefaultAdapter();

        glView = (GLSurfaceView)findViewById(R.id.glView);
        glView.setEGLContextClientVersion(2);
        glView.setEGLConfigChooser(new MultisampleConfigChooser());
        glView.setRenderer(renderer);

        statusText = (TextView)findViewById(R.id.statusText);
        rateText = (TextView)findViewById(R.id.rateText);
        heartImage = (ImageView)findViewById(R.id.heartImage);

        //transport = new DataTransport();
        //transport.start();
        //transport.connect("10.0.0.19", 43531);

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
            if (device.getName().equals("ECG")) {
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

    private void ecgUpdate(int value) {
        graphBuffer.insertValue(value);
    }

    private void pulseUpdate(final int pulse) {
        new Thread(new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        rateText.setText(String.valueOf(pulse));
                        heartImage.setVisibility(View.VISIBLE);
                    }
                });
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    // ignore
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        heartImage.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }).start();

        toneGenerator.play();
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
            try {
                byte1 = stream.read();
            }
            catch (IOException e) {
                return;
            }

            if ((byte1 & 0b10000000) == 0) continue;

            try {
                byte2 = stream.read();
            }
            catch (IOException e) {
                return;
            }

            if ((byte2 & 0b10000000) != 0) continue;

            if (transport != null) {
                transport.put(new byte[]{(byte) byte1, (byte) byte2});
            }

            int type = (byte1 & 0b01100000) >> 5;

            if (type == 0) {
                int value = byte2;
                value |= (byte1 & 0b00011111) << 7;

                ecgUpdate(value);
            }
            else if (type == 1) {
                int value = byte2;
                value |= (byte1 & 0b00001111) << 6;
                pulseUpdate(value);
            }
            else {
                Log.d("ECG", "unknown type " + type);
            }
        }
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
