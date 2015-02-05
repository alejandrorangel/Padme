package mx.edu.cicese.alejandro.padme;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import mx.edu.cicese.alejandro.audio.record.AudioClipListener;
import mx.edu.cicese.alejandro.audio.record.OneDetectorManyObservers;

public class MainActivity extends Activity {

    private static final String TAG = "VoiceTracker";
    private static final int TAKE_PICTURE_REQUEST = 1;
    private RecordAudioTask recordAudioTask;
    private Context context;
    private TextView log;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        EventBus.getDefault().register(this);
        context = this.getApplicationContext();
        setContentView(R.layout.activity_main);
        log = (TextView) findViewById(R.id.textView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        startTask(createAudioLogger(), "Voice Tracker");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopAll();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void startTask(AudioClipListener detector, String name) {
        stopAll();

        recordAudioTask = new RecordAudioTask(MainActivity.this, log, progressBar, name);
        List<AudioClipListener> observers = new ArrayList<>();
        observers.add(new AudioClipLogWrapper(this));
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

    /*
    TODO Esto se puede cambiar
     */
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

    public void turnOnScreen() {
        PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "Wake Lock");
        if (powerManager.isScreenOn() == false) {
            wakeLock.acquire();
        }
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void onEventMainThread(AudioClipLogWrapper event) {
        progressBar.setProgress((int) event.getCurrentVolume());
        log.setText(event.toString());
    }

    public void onEvent(AudioClipLogWrapper event) {
        if (event.photo) {
            this.runOnUiThread(new Runnable() {

                public void run() {

                    //takePicture();

                }
            });
        }
        event.photo = false;
    }

    /*
    *** TODO Buscar un mejor lugar para esto
    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            String thumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);

            processPictureWhenReady(picturePath);
            // TODO: Show the thumbnail to the user while the full picture is being
            // processed.
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);

        if (pictureFile.exists()) {
            // The picture is ready; process it.
        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(pictureFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }
    */
}
