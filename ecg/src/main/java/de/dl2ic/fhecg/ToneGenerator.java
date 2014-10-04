package de.dl2ic.fhecg;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

class ToneGenerator {
    private AudioTrack audioTrack;
    private final int sampleRate = 8000;
    private final int fadeSamples = 50;
    private int numSamples;

    private double freq;
    private byte pcm[];

    public ToneGenerator(int duration, int freq) {
        this.freq = freq;
        this.numSamples = duration * sampleRate / 1000;
        this.pcm = new byte[2 * numSamples];

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, pcm.length,
                    AudioTrack.MODE_STATIC);

        genTone();
    }

    private void genTone(){
        int idx = 0;
        for (int i = 0; i < numSamples; ++i) {
            double dVal  = Math.sin(2 * Math.PI * i / (sampleRate/freq));

            if (i <= fadeSamples) {
                double factor = ((double)i)/((double)fadeSamples);
                dVal *= 0.5 * (1-Math.cos(factor*Math.PI));
            }
            else if (i >=  numSamples-fadeSamples) {
                double factor = ((double)(numSamples-i))/((double)fadeSamples);
                dVal *= 0.5 * (1-Math.cos(factor*Math.PI));
            }

            // scale to maximum amplitude
            short val = (short)(dVal * 32767);
            // in 16 bit wav PCM, first byte is the low order byte
            pcm[idx++] = (byte)(val & 0x00ff);
            pcm[idx++] = (byte)((val & 0xff00) >>> 8);
        }

        audioTrack.write(pcm, 0, pcm.length);
    }

    public void play(){
        audioTrack.pause();
        audioTrack.setPlaybackHeadPosition(0);
        audioTrack.play();
    }
}
