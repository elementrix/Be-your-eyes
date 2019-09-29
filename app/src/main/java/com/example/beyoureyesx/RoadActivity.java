package com.example.beyoureyesx;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Locale;

public class RoadActivity extends AppCompatActivity {

    TextToSpeech tts2;
    LocationManager lm;
    LocationListener mLocationListener;

    ArrayList<String> path = new ArrayList<>();
    ArrayList<Double> path_coor = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_road);
        boolean isOnTrack = true;

        Intent intent = getIntent();
        double spLat = intent.getExtras().getDouble("spLat");
        double spLng = intent.getExtras().getDouble("spLng");
        double epLat = intent.getExtras().getDouble("epLat");
        double epLng = intent.getExtras().getDouble("epLng");

        final TMapView tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("5d39a358-e7e5-4f76-9b91-a6de462b5042");

        LinearLayout linearLayoutTmap = (LinearLayout) findViewById(R.id.linearLayoutTmap);
        TMapData tmapdata = new TMapData();//경로 데이터를 저장하기 위한 변수

        TMapPoint tMapPointStart = new TMapPoint(spLat, spLng);// 경로의 시작점
        TMapPoint tMapPointEnd = new TMapPoint(epLat, epLng);// 경로의 끝점

        TMapGpsManager tmapgps = null;

        tmapdata.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, tMapPointStart, tMapPointEnd, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine polyLine) {
                tMapView.addTMapPath(polyLine);
            }
        }); //경로를 받아와서 지도에 line으로 그려주는 함수
        tMapView.setIconVisibility(true);//현재위치로 표시될 아이콘을 표시할지 여부를 설정합니다.

        tmapdata.findPathDataAllType(TMapData.TMapPathType.PEDESTRIAN_PATH, tMapPointStart, tMapPointEnd, new TMapData.FindPathDataAllListenerCallback() {
            @Override
            public void onFindPathDataAll(Document document) {
                Element root = document.getDocumentElement();
                NodeList nodeListPlacemark = root.getElementsByTagName("Placemark");
                for (int i = 0; i < nodeListPlacemark.getLength(); i++) {
                    NodeList nodeListPlacemarkItem = nodeListPlacemark.item(i).getChildNodes();
                    //Log.d("debug","hi", (Throwable) nodeListPlacemarkItem.item(i));
                    for (int j = 0; j < nodeListPlacemarkItem.getLength(); j++) {
                        if (nodeListPlacemarkItem.item(j).getNodeName().equals("description")) {
                            boolean insert_check = false;
                            if (!nodeListPlacemarkItem.item(j).getTextContent().contains(",")) {
                                insert_check=true;
                                Log.d("debug", nodeListPlacemarkItem.item(j).getTextContent());
                                path.add(nodeListPlacemarkItem.item(j).getTextContent());
                            }
                        }
                        if (nodeListPlacemarkItem.item(j).getNodeName().equals("Linestring") || nodeListPlacemarkItem.item(j).getNodeName().equals("Point")) {
                            NodeList coordi_node = nodeListPlacemarkItem.item(j).getChildNodes();
                            //Log.d("debug","I'm here 1");
                            for (int k =0; k<coordi_node.getLength(); k++) {
                                if(coordi_node.item(k).getNodeName().equals("coordinates")){
                                    Log.d("debug",coordi_node.item(k).getTextContent());
                                    String[] coor_array = coordi_node.item(k).getTextContent().split(",");
                                    path_coor.add(Double.parseDouble(coor_array[0]));
                                    path_coor.add(Double.parseDouble(coor_array[1]));
                                }
                            }
                        }
                    }
                }
            }
        }); // KML파싱함수

        tts2 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                tts2.setPitch(1.0f); //1.5톤 올려서
                tts2.setSpeechRate(1.2f); //1배속으로 읽기
                tts2.setLanguage(Locale.KOREAN);
            }
        });

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            int nodeCurrent = 0; // 알려주어야 할 노드가 무엇인지 알아봐야 하므로 알려주면 하나씩 증가
            public void onLocationChanged(Location location) {
                if(location!=null) {

                    double longitude = location.getLongitude(); //경도
                    double latitude = location.getLatitude();   //위도

                    //Log.d("Campus","latitude: "+latitude+"\nlongitude: "+longitude);

                    tMapView.setLocationPoint(longitude, latitude);
                    tMapView.setCenterPoint(longitude, latitude, true);

                    Log.d("Back button out", "path size is : " + path.size() + " path_coor is : " + path_coor.size() + " nodecurrent: " + nodeCurrent);


                    if(nodeCurrent==0){
                        tts2.speak("안내를 시작할게요."+path.get(nodeCurrent)+"하세요.",TextToSpeech.QUEUE_ADD,null,null);
                        Log.d("debug","nodeCurrent: "+nodeCurrent);
                        Log.d("debug",Integer.toString(path_coor.size()));
                        nodeCurrent++;
                    }else if (2*nodeCurrent+2 < path_coor.size()) {
                        //Log.d("debug", "경도오차" + Double.toString(path_coor.get(2 * nodeCurrent) - longitude) + "// 위도오차" + Double.toString(path_coor.get(2 * nodeCurrent + 1) - latitude));
                        if ((path_coor.get(2 * nodeCurrent) > longitude - 0.00008) && (path_coor.get(2 * nodeCurrent) < longitude + 0.00008) && ((path_coor.get(2 * nodeCurrent + 1) > latitude - 0.00008) && (path_coor.get(2 * nodeCurrent + 1) < latitude + 0.00008))) {
                            tts2.speak(path.get(nodeCurrent) + "하세요.", TextToSpeech.QUEUE_FLUSH, null,null);
                            //Log.d("debug", "Got it!");
                            Log.d("debug", "nodeCurrent: " + nodeCurrent);
                            nodeCurrent++;
                        }
                    }else if (2*nodeCurrent+2 == path_coor.size()){
                        if ((path_coor.get(2 * nodeCurrent) > longitude - 0.00008) && (path_coor.get(2 * nodeCurrent) < longitude + 0.00008) && ((path_coor.get(2 * nodeCurrent + 1) > latitude - 0.00008) && (path_coor.get(2 * nodeCurrent + 1) < latitude + 0.00008))) {
                            tts2.speak(path.get(nodeCurrent) + "하셨어요. 안내를 종료할게요", TextToSpeech.QUEUE_FLUSH, null,null);
                            Log.d("debug", "Got it!");
                            Log.d("debug", "nodeCurrent: " + nodeCurrent);
                            nodeCurrent++;
                        }
                    }else if(2*nodeCurrent+2 > path_coor.size()){
                        toast("Turn off listener");
                        lm.removeUpdates(this);
                    }
                }
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
                //Log.d("test", "onStatusChanged, provider:" + provider + ", status:" + status + " ,Bundle:" + extras);
            }

        };

        try {
            if (isOnTrack) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 50, 1, mLocationListener);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 50, 1, mLocationListener);

            } else {
                lm.removeUpdates(mLocationListener);  //  미수신할때는 반드시 자원해체를 해주어야 한다.
            }
        } catch (SecurityException ex) {
        }

        String locationProvider = LocationManager.NETWORK_PROVIDER;

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = lm.getLastKnownLocation(locationProvider);

        //tMapView.setCompassMode(true);
        tMapView.setIconVisibility(true);
        tMapView.setZoomLevel(20);
        tMapView.setMapType(TMapView.MAPTYPE_STANDARD);
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN);
        tMapView.setTrackingMode(true);
        tMapView.setSightVisible(true);

        toast("현재위치" + Double.toString(location.getLongitude()) + "/" + Double.toString(location.getLatitude()));

        linearLayoutTmap.addView(tMapView);
    }
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "안내를 종료합니다", Toast.LENGTH_SHORT).show();
        lm.removeUpdates(mLocationListener);
        path.clear();
        path_coor.clear();
        Log.d("Back button in","path size is : "+path.size()+"path_coor is : "+path_coor.size());
        super.onBackPressed();
    }
    /*
    @Override
    public void onStop(){
        super.onStop();
        if(lm !=null)
            lm.removeUpdates((LocationListener) this);
    }
    */
    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(tts2 != null){
            tts2.stop();
            tts2.shutdown();
            tts2 = null;
        }
    }

}
