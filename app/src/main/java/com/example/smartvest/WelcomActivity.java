package com.example.smartvest;

import static app.akexorcist.bluetotohspp.library.BluetoothState.REQUEST_ENABLE_BT;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class WelcomActivity extends AppCompatActivity {
    TextView logout_worker;
    ConstraintLayout location_worker;
    ConstraintLayout safety_worker;
    ConstraintLayout manual_worker;
    ConstraintLayout report_worker;
    TextView vest_connection;
    String input_name = "";
    private long backKeyPressedTime = 0;
    private Toast toast;
    private List<UserLocation> userLocationList;
    private List<UserLocation> saveLocationList;
    private BluetoothSPP bt;

    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        String userID= intent.getStringExtra("userID");
        if (System.currentTimeMillis() > backKeyPressedTime + 2500) {
            backKeyPressedTime = System.currentTimeMillis();
            toast = Toast.makeText(this, "뒤로 가기 버튼을 한 번 더 누르시면 종료됩니다.", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2500) {
            finish();
            toast.cancel();
            toast = Toast.makeText(this,"이용해 주셔서 감사합니다.",Toast.LENGTH_LONG);
            toast.show();

            Response.Listener<String> responseListener = new Response.Listener<String>(){
                @Override
                public void onResponse(String response)
                {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        if (success) {
                            //userLocationList.remove(final i);
                            for(int i = 0; i<saveLocationList.size(); i++)
                            {
                                if(saveLocationList.get(i).getUserID().equals(userID))
                                {
                                    saveLocationList.remove(i);
                                    break;
                                }
                            }
                            // notifyDataSetChanged();
                        }
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            };
            LocationDeleteRequest LocationdeleteRequest = new LocationDeleteRequest(userID, responseListener);
            RequestQueue queue = Volley.newRequestQueue(WelcomActivity.this);
            queue.add(LocationdeleteRequest);
        }


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcom);
        Intent intent = getIntent();
        String userID = intent.getStringExtra("userID");

        location_worker = findViewById(R.id.location_worker);
        manual_worker = findViewById(R.id.manual_worker);
        safety_worker = findViewById(R.id.safety_worker);

        //로그아웃 버튼, 관리자 맵에서 마커 삭제
        logout_worker = findViewById(R.id.logout_worker);
        logout_worker.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        logout_worker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bt.disconnect();
                bt.stopService();
                Toast.makeText(WelcomActivity.this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                Intent in = new Intent(WelcomActivity.this, MainActivity.class);
                in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(in);
                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            if (success) {
                                //userLocationList.remove(final i);
                                for (int i = 0; i < saveLocationList.size(); i++) {
                                    if (saveLocationList.get(i).getUserID().equals(userID)) {
                                        saveLocationList.remove(i);
                                        break;
                                    }
                                }
                                // notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                };
                LocationDeleteRequest LocationdeleteRequest = new LocationDeleteRequest(userID, responseListener);
                RequestQueue queue = Volley.newRequestQueue(WelcomActivity.this);
                queue.add(LocationdeleteRequest);
            }
        });
        //캘린더
        report_worker = findViewById(R.id.report_worker);
        report_worker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WorkerCalendarView.class);
                startActivity(intent);
                intent.putExtra("userID", userID);
            }
        });
        //작업자 현재 위치 보기
        location_worker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WorkerMapActivity.class);
                intent.putExtra("userID", userID);
                WelcomActivity.this.startActivity(intent);

            }
        });
        //작업자 매뉴얼
        manual_worker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WorkerManualActivity.class);
                startActivity(intent);
            }
        });
        //작업자 안전도 확인
        safety_worker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), WorkerSafety.class);
                intent.putExtra("userID", userID);
                WelcomActivity.this.startActivity(intent);
            }
        });

    }
}



