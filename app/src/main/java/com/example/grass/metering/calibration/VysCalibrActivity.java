package com.example.grass.metering.calibration;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.grass.metering.R;
import com.example.grass.metering.validation.ValidationCallback;

import java.io.IOException;
import java.util.ArrayList;


public class VysCalibrActivity extends Activity implements View.OnClickListener, SensorEventListener,
        ValidationCallback, SoundPool.OnLoadCompleteListener {
    VysCalibrDialog dialog;
    SensorManager sensorManager;

    private float[] accelerometerValues;
    private ArrayList<Double> angles;
    private double[] task_data;
    TextView heightView;
    TextView angleView;

    MeteringTask task;


    Sensor accelerometer;
    SoundPool sp;
    int sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_meter);

        sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        sp.setOnLoadCompleteListener(this);

        try {
            sound = sp.load(getAssets().openFd("1897.ogg"), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        task_data = new double[]{0, 0};

        dialog = new VysCalibrDialog();
        dialog.setMeteringActivity(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Получаем менеджер сенсоров
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);

        heightView = (TextView) findViewById(R.id.heightValue);
        angleView = (TextView) findViewById(R.id.angleValue);
        angles = new ArrayList<>();

        dialog.show(getFragmentManager(), "Налаштування");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layer:
                Log.d("ran", "ran");
                stopTask();
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            accelerometerValues = event.values;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public float[] getOrientation() {
        float[] values = new float[3];
        double ax = accelerometerValues[0];
        double ay = accelerometerValues[1];
        double az = accelerometerValues[2];
        double x = Math.atan(ax / Math.sqrt(ay * ay + az * az));
        double y = Math.atan(ay / Math.sqrt(ax * ax + az * az));
        double z = Math.atan(az / Math.sqrt(ay * ay + ax * ax));
        values[0] = (float) Math.toDegrees(x);
        values[1] = (float) Math.toDegrees(y);
        values[2] = (float) Math.toDegrees(z) - 90;
        Log.d("orientation", "orientation 2 " + values[0] + " " + values[1] + " " + values[2]);

        return values;
    }

    public boolean checkRotate(float angle) {
        if (Math.abs(angle) > 60 && Math.abs(angle) < 115) {
            return true;
        }
        return false;
    }
    public double[] calculateHeight(double angle, double length) {
        angles.add((double) roundNumber(angle, 2));

        if (angles.size() == 3) {
            angle = roundNumber(averageAngle(), 2);
            double tan = Math.tan(Math.toRadians(Math.abs(angle)));
            Log.d("orientation", "tan = " + tan + " length1 = " + length);
            task_data[0] = Math.abs(angle);
            if(angle ==0)
                task_data[1] = 0;
            else
                task_data[1] = length*tan;
            angles = new ArrayList<>();

        }
        return task_data;
    }

    public double averageAngle() {
        double sum = 0;
        for (double value : angles)
            sum += value;
        return sum / angles.size();
    }



    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

    }

    @Override
    public void valid(Boolean valid) {

    }

    public class MeteringTask extends AsyncTask<Double, String, double[]> {

        private boolean runFlag = true;

        public void stopTask() {
            runFlag = false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            heightView.setText("00.00");
            angleView.setText("00.00");
        }

        @Override
        protected double[] doInBackground(Double... params) {
            double length = params[0];
            while (runFlag) {
                try {
                    new Thread().sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                float[] values = getOrientation();
                if (checkRotate(values[2])) {
                    Log.d("ff",""+values[1]);
                    if (values[1] > 0)
                        task_data = calculateHeight(values[1], length);
                    else task_data = calculateHeight(0, length);
                    publishProgress("" + roundNumber(task_data[0], 2));
                }
            }
            return task_data;
        }

        protected void onProgressUpdate(String... data) {
            angleView.setText(data[0]);
        }

        @Override
        protected void onPostExecute(double[] doubles) {
            super.onPostExecute(doubles);

            heightView.setText("" + roundNumber(doubles[1], 2));
            angleView.setText("" + doubles[0]);

            Intent intent = new Intent();
            intent.putExtra("eyeLength",dialog.eyeLength);
            intent.putExtra("eyeHeight",dialog.eyeHeight);
            intent.putExtra("height", roundNumber(doubles[1], 2));
            intent.putExtra("angle", doubles[0]);
            setResult(RESULT_OK, intent);

            sp.play(sound, 1, 1, 0, 0, 1);
            finish();
        }
    }

    public void stopTask() {
        task.stopTask();
    }

    public void startTask(Double height) {
        task = new MeteringTask();
        task.execute(height);
    }


    public double roundNumber(double number, double accurancy) {
        accurancy = Math.pow(10, accurancy);
        number = Math.round(number * accurancy);

        return number / accurancy;
    }
}