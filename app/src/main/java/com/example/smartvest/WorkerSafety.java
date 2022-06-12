package com.example.smartvest;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class WorkerSafety extends AppCompatActivity {
    private BluetoothSPP bt;
    ImageView bluetooth_connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_safety);
        Intent intent = getIntent();
        String userID= intent.getStringExtra("userID");
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String str_datetime = sdfNow.format(date);

        bt = new BluetoothSPP(this);
        if (!bt.isBluetoothAvailable()) { //블루투스 사용 불가
            Toast.makeText(getApplicationContext()
                    , "블루투스 사용 불가가"
                    , Toast.LENGTH_SHORT).show();
        }
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            //데이터 수신
            TextView dustText =  findViewById(R.id.dustText);
            TextView tempText = findViewById(R.id.tempText);
            TextView humText =  findViewById(R.id.humText);
            TextView coText = findViewById(R.id.coText);

            public void onDataReceived(byte[] data, String message) {
                String btn= message.substring(0,1);
                String dust = message.substring(1,5);
                String co= message.substring(5,10);
                String hum=message.substring(10,13);
                String temp = message.substring(13);
                String str_btn = btn;
                String str_dust = dust;
                String str_co = co;
                String str_hum = hum;
                String str_temp = temp;

                int bbtn = Integer.parseInt(btn);
                int ddust = Integer.parseInt(dust);
                int cco = Integer.parseInt(co);
                if(bbtn == 1)
                {
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(WorkerSafety.this);
                    builder.setMessage("긴급상황입니다.")
                            .setPositiveButton("확인", null)
                            .create()
                            .show();
                }
                if(cco >=0 && cco <=20)
                {
                    coText.setTextColor(Color.BLUE);
                    coText.setText("좋음\n"+co.concat("ppm"));
                }else if( ddust>=21 && ddust <=400)
                {   coText.setTextColor(Color.GREEN);
                    coText.setText("보통\n"+co.concat("ppm"));
                }else if( ddust>=401 && ddust <= 800)
                {    coText.setTextColor(Color.parseColor("#FF7F00"));
                    coText.setText("나쁨 \n" +co.concat("ppm"));
                }else if( ddust>=801 )
                {   coText.setTextColor(Color.RED);
                    coText.setText("매우나쁨 \n" +co.concat("ppm"));
                }
                if(ddust >=0 && ddust <=30)
                {
                    dustText.setTextColor(Color.BLUE);
                    dustText.setText("좋음\n"+dust.concat("㎍/㎥"));
                }else if( ddust>=31 && ddust <=80)
                {   dustText.setTextColor(Color.GREEN);
                    dustText.setText("보통\n"+dust.concat("㎍/㎥"));
                }else if( ddust>=81 && ddust <= 150)
                {    dustText.setTextColor(Color.parseColor("#FF7F00"));
                    dustText.setText("나쁨 \n" +dust.concat("㎍/㎥"));
                }else if( ddust>=151 )
                {   dustText.setTextColor(Color.RED);
                    dustText.setText("매우나쁨 \n" +dust.concat("㎍/㎥"));
                }
                tempText.setText(temp.concat(" ℃"));
                humText.setText(hum.concat("%"));


                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                };
                BluetoothRequest BluetoothRequest = new BluetoothRequest(userID, str_datetime, str_btn, str_dust, str_co,str_hum,str_temp,responseListener);
                RequestQueue queue = Volley.newRequestQueue(WorkerSafety.this);
                queue.add(BluetoothRequest);


            }

        });





        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() { //연결됐을 때
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(getApplicationContext()
                        , "Connected to " + name + "\n" + address, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceDisconnected() {//연결해제
                Toast.makeText(getApplicationContext()
                        , "연결해제", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceConnectionFailed() {//연결실패
                Toast.makeText(getApplicationContext()
                        , "연결실패", Toast.LENGTH_SHORT).show();
            }
        });

        bluetooth_connect = findViewById(R.id.bluetooth_connect);
        bluetooth_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
                    bt.disconnect();
                } else {
                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);

                }
            }
        });
    }
    public void onDestroy() {
        super.onDestroy();
        bt.stopService(); //블루투스 중지
    }

    public void onStart() {
        super.onStart();
        if (!bt.isBluetoothEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);

            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            } else {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }
}