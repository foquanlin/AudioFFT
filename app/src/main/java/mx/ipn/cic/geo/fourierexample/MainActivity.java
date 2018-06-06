package mx.ipn.cic.geo.fourierexample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class MainActivity extends Activity implements OnClickListener {
    
    int frequency = 8000;
    int blockSize = 256;
    @SuppressWarnings("deprecation")
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private RealDoubleFFT transformer;

    Button buttonStartStopRecording;
    boolean startedRecording = false;

    RecordAudio recordTask;
    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;
    
    static MainActivity mainActivity;
    
    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private class RecordAudio extends AsyncTask<Void, double[], Void> {
        @Override
        protected Void doInBackground(Void... params) {

            int bufferSize = AudioRecord.getMinBufferSize(frequency,
                    channelConfiguration, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT, frequency,
                    channelConfiguration, audioEncoding, bufferSize);

            short[] buffer = new short[blockSize];
            double[] toTransform = new double[blockSize];
            try {
                audioRecord.startRecording();
            }
            catch(IllegalStateException e) {
                Log.e("Error inicio grabación:", e.toString());
            }
            while (startedRecording) {
                int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

                for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                    toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
                }

                transformer.ft(toTransform);
                publishProgress(toTransform);
            }
            try {
                audioRecord.stop();
            }
            catch(IllegalStateException e){
                Log.e("Error fin grabación:", e.toString());
            }
            return null;
        }
        
        protected void onProgressUpdate(double[]... toTransform) {
            int x, downy, upy;
            
            Log.e("Proceso de grabación", "Despliegue en proceso");
            canvas.drawColor(Color.DKGRAY);
            for (int i = 0; i < toTransform[0].length; i++) {
                x = i;
                downy = (int) (100 - (toTransform[0][i] * 10));
                upy = 100;
                canvas.drawLine(x, downy, x, upy, paint);
            }
            imageView.invalidate();
        }
    }

    public void onClick(View v) {
        if (this.startedRecording) {
            this.startedRecording = false;
            this.buttonStartStopRecording.setText(R.string.button_text_start_record);
            this.recordTask.cancel(true);
            this.recordTask = null;
        } 
        else {
            startedRecording = true;
            buttonStartStopRecording.setText(R.string.button_text_stop_record);
            this.recordTask = new RecordAudio();
            this.recordTask.execute();
        }
    }
        
    static MainActivity getMainActivity() {
        return mainActivity;
    }
        
    public void onStop()
    {
        super.onStop();
        startedRecording = false;
        this.buttonStartStopRecording.setText(R.string.button_text_start_record);
        this.recordTask.cancel(true);
    }
        
    public void onStart() {
        super.onStart();
            
        setContentView(R.layout.activity_main);
            
        this.buttonStartStopRecording = (Button) this.findViewById(R.id.buttonStartStopRecording);
        this.buttonStartStopRecording.setOnClickListener(this);

        this.transformer = new RealDoubleFFT(blockSize);

        this.imageView = (ImageView) this.findViewById(R.id.imageViewSpectrum);
        this.bitmap = Bitmap.createBitmap(this.blockSize,100,
                Bitmap.Config.ARGB_8888);
        this.canvas = new Canvas(this.bitmap);
        this.paint = new Paint();
        this.paint.setColor(Color.CYAN);
        this.imageView.setImageBitmap(bitmap);
        mainActivity = this;
    }
}
