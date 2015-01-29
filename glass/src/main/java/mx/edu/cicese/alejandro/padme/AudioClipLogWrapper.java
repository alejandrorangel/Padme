/*
 * Copyright 2012 Greg Milette and Adam Stroud
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mx.edu.cicese.alejandro.padme;

import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import mx.edu.cicese.alejandro.audio.record.AudioClipListener;


public class AudioClipLogWrapper implements AudioClipListener {
    private static final String TAG = "Voice Tracker";

    private TextView log;

    private ProgressBar progressBar;

    private Activity context;

    private double averageVolume;

    private double lowPassAlpha = 0.5;

    private double STARTING_AVERAGE = 200.0;

    private double INCREASE_FACTOR = 5.0;


    public AudioClipLogWrapper(TextView log, ProgressBar progressBar, Activity context) {
        this.log = log;
        this.progressBar =  progressBar;
        this.context = context;
        averageVolume = STARTING_AVERAGE;
    }

    @Override
    public boolean heard(short[] audioData, int sampleRate) {

        final double currentVolume = rootMeanSquared(audioData);

        double volumeThreshold = averageVolume * INCREASE_FACTOR;

        Log.d(TAG, "actual: " + currentVolume + " promedio: " + averageVolume
                + " threshold: " + volumeThreshold);

        final StringBuilder message = new StringBuilder();
        message.append("volume: ").append((int) currentVolume);

        if (currentVolume > volumeThreshold) {
            Log.e(TAG, "Alto");

            message.append(" (Alto) ");
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "Wake Lock");
            if (powerManager.isScreenOn() == false) {
                wakeLock.acquire();
            }
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        } else {
            averageVolume = lowPass(currentVolume, averageVolume);
        }

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AudioTaskUtil.appendToStartOfLog(log, message.toString());
            }
        });
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AudioTaskUtil.updateProgressBar(progressBar, currentVolume);
            }
        });

        return false;
    }


    private double lowPass(double current, double last) {
        return last * (1.0 - lowPassAlpha) + current * lowPassAlpha;
    }

    private double rootMeanSquared(short[] nums) {
        double ms = 0;
        for (int i = 0; i < nums.length; i++) {
            ms += nums[i] * nums[i];
        }
        ms /= nums.length;
        return Math.sqrt(ms);
    }

}
