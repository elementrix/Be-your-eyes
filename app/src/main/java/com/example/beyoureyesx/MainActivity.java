package com.example.beyoureyesx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
//음성인식에 필요한 import 문
import android.content.Intent;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;

import androidx.appcompat.app.AlertDialog;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;

import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.widget.ImageButton;
import android.speech.RecognizerIntent;
import android.widget.Toast;
//음성인식 결과 출력에 필요한 import 문
import java.util.ArrayList;
import java.util.List;
//음성인식 및 인터넷 권한습득을 위한 import문
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    //블루투스
    private final int REQUEST_BLUETOOTH_ENABLE = 100;
    private final int REQUEST_CONTACT_ENABLE = 10001;

    ConnectedTask mConnectedTask = null;
    static BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    static boolean isConnectionError = false;
    private static final String TAG = "BluetoothClient";
    //블루투스
    public static final String SELECTED_PHONE = "selectedphone";
    public static final int SUCCESS = 1;
    public static final int FAIL = -1;

    String tel;
    static TextToSpeech tts;

    public static String onRead(String text) {
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,null);
        return text;
    }

    //음성입력 함수
    public void inputVoice(final TextView txt){
        try{
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");

            final SpeechRecognizer stt = SpeechRecognizer.createSpeechRecognizer(this);
            stt.startListening(intent);
            stt.setRecognitionListener(new RecognitionListener(){
                @Override
                public void onReadyForSpeech(Bundle bundle) {
                    //toast("음성입력을 시작합니다");
                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float v) {

                }

                @Override
                public void onBufferReceived(byte[] bytes) {

                }

                @Override
                public void onEndOfSpeech() {
                    //toast("음성입력이 끝났습니다");
                }

                @Override
                public void onError(int error) {
                    toast("오류발생: " + error);
                }

                @Override
                public void onResults(Bundle results) {
                    //toast("하이");
                    ArrayList<String> result = (ArrayList<String>) results.get(SpeechRecognizer.RESULTS_RECOGNITION);
                    //toast(result.get(0));
                    txt.setText(result.get(0));
                    stt.destroy();
                }

                @Override
                public void onPartialResults(Bundle bundle) {

                }

                @Override
                public void onEvent(int i, Bundle bundle) {

                }
            });
        } catch (Exception e) {
            toast(e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isOnTrack = true;
        if((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                &&(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                &&(ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
                &&(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                &&(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)){

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS,Manifest.permission.CALL_PHONE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},5);
            toast("권한모두 줌");
        }

        setContentView(R.layout.activity_main);
        ConstraintLayout mlayout = findViewById(R.id.background);
        mlayout.setBackgroundColor(Color.rgb(255,242,204));
        getWindow().setStatusBarColor(Color.rgb(242,158,62));

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            showErrorDialog("This device is not implement Bluetooth.");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        }
        else {
            Log.d(TAG, "Initialisation successful.");

            showPairedDevicesListDialog();
        }


        findViewById(R.id.bt_get_contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactlist();
            }
        });


        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                tts.setPitch(1.0f); //1.5톤 올려서
                tts.setSpeechRate(1.2f); //1배속으로 읽기
                tts.setLanguage(Locale.KOREAN);
            }
        });

        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final LocationListener mLocationListener = new LocationListener() {
            int nodeCurrent = 0; // 알려주어야 할 노드가 무엇인지 알아봐야 하므로 알려주면 하나씩 증가
            public void onLocationChanged(Location location) {
                Log.d("test", "onLocationChanged, location:" + location);
                double longitude = location.getLongitude(); //경도
                double latitude = location.getLatitude();   //위도
                lm.removeUpdates(this);
            }
            public void onProviderDisabled(String provider) {
                // Disabled시
                Log.d("test", "onProviderDisabled, provider:" + provider);
            }

            public void onProviderEnabled(String provider) {
                // Enabled시
                Log.d("test", "onProviderEnabled, provider:" + provider);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                // 변경시
                Log.d("test", "onStatusChanged, provider:" + provider + ", status:" + status + " ,Bundle:" + extras);
            }
        };

        try {
            if (isOnTrack) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, mLocationListener);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100, 1, mLocationListener);

            } else {
                lm.removeUpdates(mLocationListener);  //  미수신할때는 반드시 자원해체를 해주어야 한다.
            }
        } catch (SecurityException ex) {
        }
        String locationProvider = LocationManager.NETWORK_PROVIDER;

        final Location location = lm.getLastKnownLocation(locationProvider);

        //버튼 및 음성입력 결과 값이 들어가는 텍스트뷰 정의

        ImageButton btnEP =  findViewById(R.id.EP);
        Button btnCOM = findViewById(R.id.complete);

        final TextView txtEP = findViewById(R.id.txtEP);

        txtEP.setTextSize(18);

        btnEP.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                //좀결지점 버튼 클릭시 음성입력 함수
                inputVoice(txtEP);
                //toast(txtEP.getText().toString());
            }
        });

        btnCOM.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sendMessage = "1";//mInputEditText.getText().toString();
                sendMessage(sendMessage);
                Log.d(TAG, "Successfully send 1!");
                //블루투스
                Geocoder geo = new Geocoder(MainActivity.this);
                try{

                    double spLat = location.getLatitude();
                    double spLng = location.getLongitude();

                    //toast(Double.toString(spLat)+Double.toString(spLng));

                    List<Address> epLocation = geo.getFromLocationName(txtEP.getText().toString(),1);
                    //toast(Double.toString(epLat)+Double.toString(epLng));
                    //toast("주소변환값"+":"+spLat);
                    if(epLocation.size()>0) {
                        double epLat = epLocation.get(0).getLatitude();
                        double epLng = epLocation.get(0).getLongitude();

                        Intent intent = new Intent(getApplicationContext(), RoadActivity.class);
                        intent.putExtra("spLat", spLat);
                        intent.putExtra("spLng", spLng);
                        intent.putExtra("epLat", epLat);
                        intent.putExtra("epLng", epLng);

                        startActivity(intent);
                    }else{
                        tts.speak("주소변환에 실패했어요. 다시시도해 주세요", TextToSpeech.QUEUE_ADD, null,null);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ( mConnectedTask != null ) {
            mConnectedTask.cancel(true);
        }
        if(tts != null){
            toast("I'm gone!");
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        private BluetoothSocket mBluetoothSocket = null;
        private BluetoothDevice mBluetoothDevice = null;

        ConnectTask(BluetoothDevice bluetoothDevice) {
            mBluetoothDevice = bluetoothDevice;
            mConnectedDeviceName = bluetoothDevice.getName();

            //SPP
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                Log.d( TAG, "create socket for "+mConnectedDeviceName);

            } catch (IOException e) {
                Log.e( TAG, "socket create failed " + e.getMessage());
            }

        }


        @Override
        protected Boolean doInBackground(Void... params) {

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mBluetoothSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mBluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " +
                            " socket during connection failure", e2);
                }
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean isSucess) {
            final TextView txtEP = (TextView) findViewById(R.id.txtEP);
            if ( isSucess ) {
                connected(mBluetoothSocket);
                tts.speak("도착지를 말씀해주세요!", TextToSpeech.QUEUE_FLUSH, null,null);
                inputVoice(txtEP);
            }
            else{
                tts.speak("블루투스 연결에 실패했어요. 앱 종료후 다시 시도해 주세요", TextToSpeech.QUEUE_FLUSH, null,null);
                isConnectionError = true;
                Log.d( TAG,  "Unable to connect device");
                showErrorDialog("블루투스 연결에 실패했어요");
            }
        }
    }

    public void connected( BluetoothSocket socket ) {
        mConnectedTask = new ConnectedTask(socket);
        mConnectedTask.execute();
    }

    private class ConnectedTask extends AsyncTask<Void, String, Boolean> {
        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private BluetoothSocket mBluetoothSocket = null;
        ConnectedTask(BluetoothSocket socket){

            mBluetoothSocket = socket;

            try {
                mInputStream = mBluetoothSocket.getInputStream();
                mOutputStream = mBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "socket not created", e );
            }
            Log.d( TAG, "connected to "+mConnectedDeviceName);
            toast(mConnectedDeviceName+"에 연결되었습니다.");
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            byte [] readBuffer = new byte[1024];
            int readBufferPosition = 0;

            while (true) {
                if ( isCancelled() ) return false;
                try {
                    int bytesAvailable = mInputStream.available();
                    if(bytesAvailable > 0) {
                        Log.d(TAG, "bytesAvailable: " +bytesAvailable );
                        byte[] packetBytes = new byte[bytesAvailable];
                        mInputStream.read(packetBytes);
                        for(int i=0;i<bytesAvailable;i++) {
                            byte b = packetBytes[i];
                            if(b== '\n')
                            {
                                Log.d(TAG, "b in if: " +b );
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                        encodedBytes.length);
                                String recvMessage = new String(encodedBytes, "UTF-8");

                                readBufferPosition = 0;

                                Log.d(TAG, "recv message: " + recvMessage);
                                tts.setPitch(2.0f); //1.5톤 올려서
                                tts.setSpeechRate(2.0f); //1배속으로 읽기
                                if(recvMessage.equals("bicycleright")) {
                                    tts.speak("자전거가오른쪽에서와요!!", TextToSpeech.QUEUE_FLUSH, null,null);
                                }else if (recvMessage.equals("bicycleleft")){
                                    tts.speak("자전거가왼쪽에서와요!!", TextToSpeech.QUEUE_FLUSH, null,null);
                                }else if (recvMessage.equals("bicyclefront")){
                                    tts.speak("자전거가정면에서와요!!", TextToSpeech.QUEUE_FLUSH, null,null);
                                }else if (recvMessage.equals("motorcycleright")){
                                    tts.speak("오토바이가오른쪽에서와요!!", TextToSpeech.QUEUE_FLUSH, null,null);
                                }else if (recvMessage.equals("motorcycleleft")){
                                    tts.speak("오토바이가왼쪽에서와요!!", TextToSpeech.QUEUE_FLUSH, null,null);
                                }else if (recvMessage.equals("motorcyclefront")){
                                    tts.speak("오토바이가정면에서와요!!", TextToSpeech.QUEUE_FLUSH, null,null);
                                }else if (recvMessage.equals("emergency")){
                                    tts.speak("보호자에게연락할게요!!", TextToSpeech.QUEUE_FLUSH, null,null);
                                    startActivity(new Intent("android.intent.action.CALL", Uri.parse(tel)));
                                }
                                tts.setPitch(1.5f); //1.5톤 올려서
                                tts.setSpeechRate(1.0f); //1배속으로 읽기
                                publishProgress(recvMessage);
                            }
                            else
                            {
                                Log.d(TAG, "b in else: " +b );
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    return false;
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean isSucess) {
            super.onPostExecute(isSucess);
            if ( !isSucess ) {
                closeSocket();
                Log.d(TAG, "블루투스 연결이 끊겼습니다");
                isConnectionError = true;
                showErrorDialog("블루투스 연결이 끊겼습니다");
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);
            closeSocket();
        }

        void closeSocket(){
            try {
                mBluetoothSocket.close();
                Log.d(TAG, "close socket()");
            } catch (IOException e2) {
                Log.e(TAG, "unable to close() " +
                        " socket during connection failure", e2);
            }
        }

        void write(String msg){
            msg += "\n";
            try {
                mOutputStream.write(msg.getBytes());
                mOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during send", e );
            }
        }
    }


    public void showPairedDevicesListDialog()
    {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);

        if ( pairedDevices.length == 0 ){
            showQuitDialog( "어느 기기와도 연결되지 않았습니다.\n"
                    +"한개의 기기와는 연결되어야 합니다.");
            return;
        }

        String[] items;
        items = new String[pairedDevices.length];
        for (int i=0;i<pairedDevices.length;i++) {
            items[i] = pairedDevices[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select device");
        builder.setCancelable(false);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                ConnectTask task = new ConnectTask(pairedDevices[which]);
                task.execute();
            }
        });
        builder.create().show();
    }

    public void showErrorDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("연결실패");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("확인",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if ( isConnectionError  ) {
                    isConnectionError = false;
                    //finish();
                }
            }
        });
        builder.create().show();
    }

    public void showQuitDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("종료");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("확인",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();
    }

    void sendMessage(String msg){

        if ( mConnectedTask != null ) {
            mConnectedTask.write(msg);
            Log.d(TAG, "send message: " + msg);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_BLUETOOTH_ENABLE){
            if (resultCode == RESULT_OK){
                //BlueTooth is now Enabled
                showPairedDevicesListDialog();
            }
            if(resultCode == RESULT_CANCELED){
                showQuitDialog( "You need to enable bluetooth");
            }
        }
        if (requestCode == REQUEST_CONTACT_ENABLE) {
            if (resultCode == SUCCESS) {
                tel="tel:"+data.getExtras().getString("returnPhone");
                toast("긴급연락처가 등록되었습니다: "+data.getExtras().getString("returnPhone"));
            } else {
                toast("권한이 없습니다. 설정->애플리케이션->그대의 눈동자->권한에서 권한을 등록해 주세요.");
            }
        }
    }

    private void showContactlist() {
        Intent intent = new Intent(MainActivity.this,
                ContactListActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivityForResult(intent, REQUEST_CONTACT_ENABLE);
    }

    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
