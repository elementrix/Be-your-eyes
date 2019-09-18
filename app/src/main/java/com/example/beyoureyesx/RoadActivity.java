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

    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_road);
        boolean isOnTrack = true;

        final ArrayList<String> path = new ArrayList<String>();;
        final ArrayList<Double> path_coor = new ArrayList<Double>();

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

        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final LocationListener mLocationListener = new LocationListener() {
            int nodeCurrent = 0; // 알려주어야 할 노드가 무엇인지 알아봐야 하므로 알려주면 하나씩 증가
            public void onLocationChanged(Location location) {
                double longitude = location.getLongitude(); //경도
                double latitude = location.getLatitude();   //위도
                tMapView.setCenterPoint(longitude, latitude, true);
                if(nodeCurrent==0){
                    tts.speak("안녕하세요 이제부터 안내를 시작할게요."+path.get(nodeCurrent)+"하세요.",TextToSpeech.QUEUE_FLUSH,null);
                    nodeCurrent++;
                    Log.d("debug","nodeCurrent: "+nodeCurrent);
                }
                Log.d("debug","경도오차"+Double.toString(path_coor.get(2*nodeCurrent)-longitude)+"// 위도오차"+Double.toString(path_coor.get(2*nodeCurrent+1)-latitude));
                if ((path_coor.get(2*nodeCurrent)>longitude-0.00008)&&(path_coor.get(2*nodeCurrent)<longitude+0.00008)&&((path_coor.get(2*nodeCurrent+1)>latitude-0.0017)&&(path_coor.get(2*nodeCurrent+1)<latitude+0.0017))){
                    tts.speak(path.get(nodeCurrent)+"하세요.",TextToSpeech.QUEUE_FLUSH,null);
                    Log.d("debug","Got it!");
                    Log.d("debug","nodeCurrent: "+nodeCurrent);
                    nodeCurrent++;
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

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        Location location = lm.getLastKnownLocation(locationProvider);

        tMapView.setCenterPoint(location.getLongitude(), location.getLatitude(), true);
        //tMapView.setCompassMode(true);
        tMapView.setIconVisibility(true);
        tMapView.setZoomLevel(20);
        tMapView.setMapType(TMapView.MAPTYPE_STANDARD);
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN);
        tMapView.setTrackingMode(true);
        tMapView.setSightVisible(true);

        toast("현재위치" + Double.toString(location.getLongitude()) + "/" + Double.toString(location.getLatitude()));

        linearLayoutTmap.addView(tMapView);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                tts.setPitch(1.0f); //1.5톤 올려서
                tts.setSpeechRate(1.2f); //1배속으로 읽기
                tts.setLanguage(Locale.KOREAN);
            }
        });
    }

    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(tts != null){
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

}
