package jp.ac.titech.itpro.sdl.habittimer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.support.v7.widget.CardView;
import java.util.List;
import java.util.ArrayList;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.media.AudioManager;
import android.media.MediaPlayer;
import java.io.IOException;



public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final static String TAG = "MainActivity";


    private Button startButton, stopButton;
    private CardView cardView1, cardView2, cardView3, cardView4;
    private TextView timerText1, timerText2, timerText3, timerText4;
    static CountDown timer1, timer2, timer3, timer4, countDown;
    private boolean isRunning;
    private int runningTimer;
    private long countMillis;

    private SensorRefreshThread th = null;

    private Handler handler;
    private SensorManager sensorMgr;
    private Sensor lightsensor;
    private float brightness;
    private TextView brightnessView;
    private final static long SENSOR_REFRESH_WAIT_MS = 200;

    private Switch switchState;
    private boolean motivated = false;

    private MediaPlayer mediaPlayer;
    private boolean playing = false;
    private boolean laidDown = false;

    private TextView[] textlist = new TextView[4];
    private CardView[] cardlist = new CardView[4];
    private List<CountDown> timerlist = new ArrayList<>();

    private long countTimes[] = new long[]{5000, 20000, 30000, 10000};

    public String formatFromMillisec(long millisec) {
        countMillis = millisec;
        // 残り時間を分、秒、ミリ秒に分割
        long mm = millisec / 1000 / 60;
        long ss = millisec / 1000 % 60;
        long ms = millisec - ss * 1000 - mm * 1000 * 60;

        return(String.format("%1$02d:%2$02d.%3$03d", mm, ss, ms));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button)findViewById(R.id.start_button);

        cardlist[0] = (cardView1 = (CardView)findViewById(R.id.activity1));
        cardlist[1] = (cardView2 = (CardView)findViewById(R.id.activity2));
        cardlist[2] = (cardView3 = (CardView)findViewById(R.id.activity3));
        cardlist[3] = (cardView4 = (CardView)findViewById(R.id.activity4));

        textlist[0] = (timerText1 = (TextView)findViewById(R.id.timer1));
        textlist[1] = (timerText2 = (TextView)findViewById(R.id.timer2));
        textlist[2] = (timerText3 = (TextView)findViewById(R.id.timer3));
        textlist[3] = (timerText4 = (TextView)findViewById(R.id.timer4));
        brightnessView = (TextView)findViewById(R.id.brightnessView);


        timer4 =  new CountDown(countTimes[3], 100, timerText4, null);
        timer3 =  new CountDown(countTimes[2], 100, timerText3, timer4);
        timer2 = new CountDown(countTimes[1], 100, timerText2, timer3);
        timer1 = new CountDown(countTimes[0], 100, timerText1, timer2);

        switchState = (Switch)findViewById(R.id.studyswitch);
        switchState.setChecked(false);
        switchState.setBackgroundResource(R.drawable.motivationoff);
        switchState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    // The toggle is enabled
                    switchState.setText("on");
                    motivated = true;
//                    audioPlay();
                    switchState.setBackgroundResource(R.drawable.motivationon);


                } else {
                    // The toggle is disabled
                    switchState.setText("off");
                    motivated = false;
//                    audioStop();
                    switchState.setBackgroundResource(R.drawable.motivationoff);

                }
            }
        });

        isRunning = false;
        runningTimer = 0;

//        cardView1.setOnClickListener(new View.OnClickListener() {
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    timer1.cancel();
                    isRunning = false;
                } else {
                    Log.d(TAG, "onClick");
                    timer1.start();
                    isRunning = true;
                }
            }
        });

        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightsensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
//        Log.d(TAG, lightsensor.toString());
        if (lightsensor == null) {
            finish();
            return;
        }

        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
//        sensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorMgr.registerListener(this, lightsensor, SensorManager.SENSOR_DELAY_FASTEST);
        th = new SensorRefreshThread();
        th.start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // accuracy に変更があった時の処理
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        float[] values = event.values;
        long timestamp = event.timestamp;
//        Log.d(TAG, "onSensorChanged");

        if(sensor.getType() == Sensor.TYPE_LIGHT){
//            Log.d("SENSOR_DATA", "TYPE_LIGHT = " + String.valueOf(values[0]));
            brightnessView.setText(String.valueOf(values[0]));
            brightness = values[0];
            if(brightness == 0){
                laidDown = true;
            }
        }

        if (motivated && brightness > 50){
            if (!playing && laidDown){
                audioPlay();
                laidDown = false;
            }
        } else {
            if (playing) {
                audioStop();
            }
        }
    }

    class CountDown extends CountDownTimer {

        private TextView timerText;
        private CountDown next;

        public CountDown(long millisInFuture, long countDownInterval, TextView timerTextView, CountDown nextTimer) {
            super(millisInFuture, countDownInterval);
            timerText = timerTextView;
            timerTextView.setText(formatFromMillisec(millisInFuture));
            next = nextTimer;
        }

        @Override
        public void onFinish() {
            // 完了
//            timerText.setText("0:00.000");
            timerText.setText("Done");
            try {
                next.start();
            } catch (NullPointerException e) {
                ;
            }
        }

        // インターバルで呼ばれる
        @Override
        public void onTick(long millisUntilFinished) {
            countMillis = millisUntilFinished;
            // 残り時間を分、秒、ミリ秒に分割
            long mm = millisUntilFinished / 1000 / 60;
            long ss = millisUntilFinished / 1000 % 60;
            long ms = millisUntilFinished - ss * 1000 - mm * 1000 * 60;

            timerText.setText(String.format("%1$02d:%2$02d.%3$03d", mm, ss, ms));
        }

        @Override
        public String toString(){
            return this.timerText.getText().toString();
        }

    }

    private  class SensorRefreshThread extends Thread {
        public void run() {
            try {
                while (th != null) {
                    handler.post(new Runnable() {
                        public void run() {
//                            rateView.setText(String.format(Locale.getDefault(), "%f", rate));
//                            accuracyView.setText(String.format(Locale.getDefault(), "%d", accuracy));
//                            xView.addData(vx, true);
//                            yView.addData(vy, true);
////                            zView.addData(vz, true);
//                            brightnessView.setText(String.format(Locale.getDefault(), "%f", brightness));
                        }
                    });
                    Thread.sleep(SENSOR_REFRESH_WAIT_MS);
                }
            }
            catch (InterruptedException e) {
                Log.e(TAG, e.toString());
                th = null;
            }
        }
    }

    private boolean audioSetup(){
        boolean fileCheck = false;

//        int mp3sound = R.raw.n128;
        // インタンスを生成
//        mediaPlayer = new MediaPlayer();
        mediaPlayer = MediaPlayer.create(this, R.raw.aaaaa_large );

//        int mediaResource = getResources().getIdentifier("n128", "raw", getPackageName());
//        mediaPlayer = MediaPlayer.create(getApplicationContext(), mediaResource);

        //音楽ファイル名, あるいはパス
//        String filePath = "n128.mp3";

        try {
            // assetsから mp3 ファイルを読み込み
//            AssetFileDescriptor afdescripter = getAssets().openFd(filePath);
            // MediaPlayerに読み込んだ音楽ファイルを指定
//            mediaPlayer.setDataSource(afdescripter.getFileDescriptor(),
//                    afdescripter.getStartOffset(),
//                    afdescripter.getLength());
            // 音量調整を端末のボタンに任せる
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
//            mediaPlayer.start();
//            fileCheck = true;
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return fileCheck;
    }

    private void audioPlay() {

//        if (mediaPlayer == null) {
//            // audio ファイルを読出し
//            if (audioSetup()){
//                Toast.makeText(getApplication(), "Read audio file", Toast.LENGTH_SHORT).show();
//            }
//            else{
//                Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
//                return;
//            }
//        }
//        else{
//            Log.d(TAG, "AudioPlay stop");
//            // 繰り返し再生する場合
//            mediaPlayer.stop();
//            mediaPlayer.reset();
//            // リソースの解放
//            mediaPlayer.release();
//        }

        mediaPlayer = MediaPlayer.create(this, R.raw.aaaaa_large );
        // 再生する
        mediaPlayer.start();

        playing = true;

        // 終了を検知するリスナー
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                Log.d("debug","end of audio");
            }
        });
    }

    private void audioStop() {
        if(mediaPlayer != null) {
            Log.d(TAG, "audioStop");
            // 再生終了
            mediaPlayer.stop();
            // リセット
            mediaPlayer.reset();
            // リソースの解放
            mediaPlayer.release();

            playing = false;
        }

        mediaPlayer = null;
    }

}