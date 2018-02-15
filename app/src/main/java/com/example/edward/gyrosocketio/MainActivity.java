package com.example.edward.gyrosocketio;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Button mConnectButton;
    private Button mDisconnectButton;
    private Socket mSocket;
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private static final float NS2S = 1.0f/1000000000.0f;
    private float[] last_values = null;
    private float[] velocity = null;
    private float[] position = null;
    private long last_timestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        try{
            mSocket = IO.socket("http://10.200.16.61:3002/");
        } catch (URISyntaxException e){
            throw new RuntimeException(e);
        }

        mConnectButton = (Button) findViewById(R.id.connectButton);
        mDisconnectButton = (Button) findViewById(R.id.disconnectButton);

        mConnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("CBUTTON", "Clicked");
                if(!mSocket.connected()){
                    Log.d("CBUTTON", "Here");
                    mSocket.connect();
                }
            }
        });

        mDisconnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.disconnect();
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if(!mSocket.connected()){
            return;
        }

        if(mySensor.getType() == Sensor.TYPE_ACCELEROMETER){

            if(last_values != null){
                float dt = (sensorEvent.timestamp - last_timestamp) * NS2S;
                for(int index = 0; index < 3; ++index){
                    velocity[index] += (sensorEvent.values[index] + last_values[index])/2 * dt;
                    position[index] += (velocity[index] * dt);
                }

                try{
                    JSONObject obj = new JSONObject();
                    obj.put("x", position[0]);
                    obj.put("y", position[1]);
                    obj.put("z", position[2]);

                    mSocket.emit("position", obj);

//                    long curTime = System.currentTimeMillis();

                }catch (JSONException e){
                    Log.e("ERROR", e.getMessage());
                    return;
                }

            } else {
                last_values = new float[3];
                velocity = new float[3];
                position = new float[3];
                velocity[0]=velocity[1]=velocity[2] = 0f;
                position[0]=position[1]=position[2] = 0f;
            }

            System.arraycopy(sensorEvent.values, 0, last_values, 0, 3);
            last_timestamp = sensorEvent.timestamp;


//            Log.d("SENSOR", "x: " + sensorEvent.values[0]);
//            Log.d("SENSOR", "y: " + sensorEvent.values[1]);
//            Log.d("SENSOR", "z: " + sensorEvent.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        mSocket.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSocket.connect();
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private Emitter.Listener onConnect = new Emitter.Listener(){
        @Override
        public void call(Object... args) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
    }
}
