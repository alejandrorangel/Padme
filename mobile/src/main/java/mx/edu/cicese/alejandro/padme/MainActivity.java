package mx.edu.cicese.alejandro.padme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;



public class MainActivity extends ActionBarActivity {

    private static final String TAG = "ClapperPlay";

    private RecordAudioTask recordAudioTask;

    private TextView log;

    private ProgressBar progressBar;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        log = (TextView) findViewById(R.id.textView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        bluetoothInit();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void bluetoothInit() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                //Log.w('D', "Audio SCO state: " + state);

                if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                    Log.d("D", "Audio SCO state: " + state);
                      /*
	                   * Now the connection has been established to the bluetooth device.
	                   * Record audio or whatever (on another thread).With AudioRecord you can record with an object created like this:
	                   * new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
	                   * AudioFormat.ENCODING_PCM_16BIT, audioBufferSize);
	                   *
	                   * After finishing, don't forget to unregister this receiver and
	                   * to stop the bluetooth connection with am.stopBluetoothSco();
	                   */
                    startTask(createAudioLogger(), "Voice Tracker");
                    unregisterReceiver(this);
                }

            }
        }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED));

        Log.d("D", "starting bluetooth");
        audioManager.startBluetoothSco();
    }

    private void startTask(root.gast.audio.record.AudioClipListener detector, String name) {
        stopAll();


        recordAudioTask = new RecordAudioTask(MainActivity.this, log, progressBar, name);
        //wrap the detector to show some output
        List<AudioClipListener> observers = new ArrayList<>();
        observers.add(new AudioClipLogWrapper(log, progressBar, this));
        observers.add(new AudioClipLogFixedWrapper(this));
        OneDetectorManyObservers wrapped =
                new OneDetectorManyObservers(detector, observers);
        recordAudioTask.execute(wrapped);
    }

    private void stopAll() {
        Log.d(TAG, "stop record audio");
        shutDownTaskIfNecessary(recordAudioTask);
    }

    private void shutDownTaskIfNecessary(final AsyncTask task) {
        if ((task != null) && (!task.isCancelled())) {
            if ((task.getStatus().equals(AsyncTask.Status.RUNNING))
                    || (task.getStatus()
                    .equals(AsyncTask.Status.PENDING))) {
                Log.d(TAG, "CANCEL " + task.getClass().getSimpleName());
                task.cancel(true);
            } else {
                Log.d(TAG, "task not running");
            }
        }
    }

    private AudioClipListener createAudioLogger() {
        AudioClipListener audioLogger = new AudioClipListener() {
            @Override
            public boolean heard(short[] audioData, int sampleRate) {
                if (audioData == null || audioData.length == 0) {
                    return true;
                }

                // returning false means the recording won't be stopped
                // users have to manually stop it via the stop button
                return false;
            }
        };

        return audioLogger;
    }
}
