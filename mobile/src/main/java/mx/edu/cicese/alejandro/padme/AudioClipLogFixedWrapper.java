package mx.edu.cicese.alejandro.padme;

import android.app.Activity;

import root.gast.audio.interp.LoudNoiseDetector;
import root.gast.audio.processing.ZeroCrossing;
import root.gast.audio.record.AudioClipListener;
import root.gast.audio.util.AudioUtil;

/**
 * Created by Alejandro on 1/28/15.
 */
public class AudioClipLogFixedWrapper implements AudioClipListener {

    private Activity context;

    private double previousFrequency = -1;

    public AudioClipLogFixedWrapper(Activity context)
    {
        this.context = context;
    }

    @Override
    public boolean heard(short[] audioData, int sampleRate)
    {
        final double zero = ZeroCrossing.calculate(sampleRate, audioData);
        final double volume = AudioUtil.rootMeanSquared(audioData);

        final boolean isLoudEnough = volume > LoudNoiseDetector.DEFAULT_LOUDNESS_THRESHOLD;
        //range threshold of 100
        final boolean isDifferentFromLast = Math.abs(zero - previousFrequency) > 100;

        final StringBuilder message = new StringBuilder();
        message.append("volume: ").append((int)volume);
        if (!isLoudEnough)
        {
            message.append(" (silence) ");
        }
        message.append(" freqency: ").append((int)zero);
        if (isDifferentFromLast)
        {
            message.append(" (diff)");
        }

        previousFrequency = zero;

        return false;
    }
}
