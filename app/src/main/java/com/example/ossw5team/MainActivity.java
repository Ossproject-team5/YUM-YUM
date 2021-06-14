package com.example.ossw5team;// 패키지 이름

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;//String 값부터 다양한 Pracelable type까지 mapping 해줌.
                         //Parcelable type : 기본 type(int, bool, long, string)이 아닌 class객체를 activity 간에 주고 받기 위한 type
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;//view를 상속받아 화면에 배치하여 구현하기 위함.
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;// 구형 Android기기에서 일부 최신 플랫폼 기능을 사용하려는 활동의 기본 클래스.
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.annotations.NotNull;
import com.google.firebase.firestore.FirebaseFirestore;

import net.daum.mf.map.api.MapCircle;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;//Kakao API의 MapView import

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity<currentUser> extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapView.MapViewEventListener, MapView.POIItemEventListener, MapView.OpenAPIKeyAuthenticationResultListener, View.OnClickListener {
    final static String TAG = "MapTAG";
    private FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    //현재 로그인 된 유저 정보를 담을 변수
    private FirebaseUser currentUser;
    private long backBtnTime = 0;
    Intent intent;

    //전역변수
    Button btnSearch;
    EditText mSearchEdit;

    String userEmail;
    String fStoreEmail;
    String fStoreNickname;

    //맵 뷰
    private static final String LOG_TAG = "MainActivity";
    MapView mapView;
    ViewGroup mapViewContainer;

    //현재위치정보
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION};
    boolean isTrackingMode = false; //트래킹 모드

    //value
    MapPoint currentMapPoint;
    private double mCurrentLng; //Long = X, Lat = Y
    private double mCurrentLat;
    private double mSearchLng = -1;
    private double mSearchLat = -1;
    private String mSearchName;

    ArrayList<Document> cafeList = new ArrayList<>(); //카페 CE7
    ArrayList<Document> restaurantList = new ArrayList<>(); //음식점 FD6

    ArrayList<Document> documentArrayList = new ArrayList<>(); //지역명 검색 결과 리스트

    MapPOIItem searchMarker = new MapPOIItem();


    ArrayList<Fragment> array;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getHashKey();

        //액션바 안보이게 지정
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        currentUser = fAuth.getCurrentUser();
        userEmail = currentUser.getEmail();
        System.out.println(userEmail);

        array=new ArrayList<>();

        initView();

    }

    private void initView(){
        mSearchEdit = findViewById(R.id.map_et_search);
        btnSearch = findViewById(R.id.button_search);

        //맵 띄우기
        mapView = new MapView(this);//MapView 선언
        mapView.setMapViewEventListener(this);
        mapView.setPOIItemEventListener(this);
        mapView.setOpenAPIKeyAuthenticationResultListener(this);

        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);//MapView 띄울 레이아웃의 아이디 지정
        mapViewContainer.addView(mapView);//View 에 MapView 추기

        btnSearch.setOnClickListener(this);

        //위치정보 모으기
        mapView.setCurrentLocationEventListener(this);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }

        mapView.removeAllPOIItems();
        mapView.removeAllCircles();
        requestSearchLocal(mCurrentLng, mCurrentLat);

        //검색

    }

    private void requestSearchLocal(double x, double y) {
        documentArrayList.clear();
        restaurantList.clear();
        cafeList.clear();
        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        Call<CategoryResult> call = apiInterface.getSearchCategory(getString(R.string.restapi_key), "FD6", x + "", y + "", 500);
        call.enqueue(new Callback<CategoryResult>() {
            @Override
            public void onResponse(@NotNull Call<CategoryResult> call, @NotNull Response<CategoryResult> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    if (response.body().getDocuments() != null) {
                        Log.d(TAG, "restaurantList Success");
                        restaurantList.addAll(response.body().getDocuments());
                    }
                    call = apiInterface.getSearchCategory(getString(R.string.restapi_key), "CE7", x + "", y + "", 500);
                    call.enqueue(new Callback<CategoryResult>() {
                        @Override
                        public void onResponse(@NotNull Call<CategoryResult> call, @NotNull Response<CategoryResult> response) {
                            if (response.isSuccessful()) {
                                assert response.body() != null;
                                Log.d(TAG, "cafeList Success");
                                cafeList.addAll(response.body().getDocuments());
                                //모두 통신 성공 시 circle 생성
                                MapCircle circle1 = new MapCircle(
                                        MapPoint.mapPointWithGeoCoord(x, y), // center
                                        1000, // radius
                                        Color.argb(128, 255, 0, 0), // strokeColor
                                        Color.argb(128, 0, 255, 0) // fillColor
                                );
                                circle1.setTag(5678);
                                mapView.addCircle(circle1);
                                Log.d("SIZE1", restaurantList.size() + "");
                                Log.d("SIZE2", cafeList.size() + "");
                                //마커 생성
                                int tagNum = 30;
                                for (Document document : restaurantList) {
                                    MapPOIItem marker = new MapPOIItem();
                                    marker.setItemName(document.getPlaceName());
                                    marker.setTag(tagNum++);
                                    double x = Double.parseDouble(document.getY());
                                    double y = Double.parseDouble(document.getX());
                                    //카카오맵은 참고로 new MapPoint()로  생성못함. 좌표기준이 여러개라 이렇게 메소드로 생성해야함
                                    MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(x, y);
                                    marker.setMapPoint(mapPoint);
                                    marker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 마커타입을 커스텀 마커로 지정.
                                    marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin);
//                                    marker.setCustomImageResourceId(R.drawable.ic_food); // 마커 이미지.
//                                    marker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
//                                    marker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.
                                    mapView.addPOIItem(marker);
                                }
                                for (Document document : cafeList) {
                                    MapPOIItem marker = new MapPOIItem();
                                    marker.setItemName(document.getPlaceName());
                                    marker.setTag(tagNum++);
                                    double x = Double.parseDouble(document.getY());
                                    double y = Double.parseDouble(document.getX());
                                    //카카오맵은 참고로 new MapPoint()로  생성못함. 좌표기준이 여러개라 이렇게 메소드로 생성해야함
                                    MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(x, y);
                                    marker.setMapPoint(mapPoint);
                                    marker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 마커타입을 커스텀 마커로 지정.
                                    marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin);
//                                    marker.setCustomImageResourceId(R.drawable.ic_food); // 마커 이미지.
//                                    marker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
//                                    marker.setCustomImageAnchor(0.5f, 1.0f);
                                    mapView.addPOIItem(marker);
                                }
                            }
                        }


                        @Override
                        public void onFailure(@NotNull Call<CategoryResult> call, @NotNull Throwable t) {

                        }
                    });
                }
            }

            @Override
            public void onFailure(@NotNull Call<CategoryResult> call, @NotNull Throwable t) {
                Log.d(TAG, "FAIL");
            }
        });
    }

    public void onToggleClicked(View view){
        boolean on = ((ToggleButton) view).isChecked();

        if (on){
            isTrackingMode = true;
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
        }
        else {
            isTrackingMode = false;
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        }
    }

    public void onToggleClicked2(View view){
        boolean on = ((ToggleButton) view).isChecked();

        if (on){
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        }
        else {
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapViewContainer.removeAllViews();
    }

    //현재위치 업데이트
    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float accuracyInMeters) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        Log.i(LOG_TAG, String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, accuracyInMeters));
        currentMapPoint = MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude);
        //이 좌표로 지도 중심 이동
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude), true);
        //전역변수로 현재 좌표 저장
        mCurrentLat = mapPointGeo.latitude;
        mCurrentLng = mapPointGeo.longitude;
        Log.d(TAG, "현재위치 => " + mCurrentLat + "  " + mCurrentLng);

    }
    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {
    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {
        Log.i(TAG, "onCurrentLocationUpdateFailed");
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {
        Log.i(TAG, "onCurrentLocationUpdateCancelled");
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
    }


    private void onFinishReverseGeoCoding(String result) {
//        Toast.makeText(LocationDemoActivity.this, "Reverse Geo-coding : " + result, Toast.LENGTH_SHORT).show();
    }

    // ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {


        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            boolean check_result = true;

            // 모든 퍼미션을 허용했는지 체크합니다.
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if (check_result) {
                Log.d("@@@", "start");
                //위치 값을 가져올 수 있음

            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있다
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    void checkRunTimePermission(){

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED ) {
            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)
            // 3.  위치 값을 가져올 수 있음

        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.
            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {
                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하시겠습니까?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onMapViewInitialized(MapView mapView) {

    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {

    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onDaumMapOpenAPIKeyAuthenticationResult(MapView mapView, int i, String s) {

    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {

    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {

    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {


    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }



    //로그인 되어있으면 currentUser 변수에 유저정보 할당. 아닌경우 login 페이지로 이동!
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        currentUser = fAuth.getCurrentUser();
        if(currentUser == null){
            intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }
public void UserInfo() {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    if (currentUser != null) {
        // User is signed in
        // Name, email address, and profile
        String name = currentUser.getDisplayName();
        String email = currentUser.getEmail();
        boolean emailVerified = currentUser.isEmailVerified();
        String uid = currentUser.getUid();
    } else {
        // No user is signed in
    }
}
    public void onBackPressed() {
        long curTime = System.currentTimeMillis();
        long gapTime = curTime - backBtnTime;

        if(0 <= gapTime && 2000 >= gapTime) {
            super.onBackPressed();
        }
        else {
            backBtnTime = curTime;
            Toast.makeText(this, "한번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT).show();
        }
    }

    private void getHashKey(){
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo("com.example.ossw5team", PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null)
            Log.e("KeyHash", "KeyHash:null");

        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            } catch (NoSuchAlgorithmException e) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=" + signature, e);
            }
        }
    }

    @Override
    public void onClick(View view) {

    }


//    class PageAdapter extends FragmentStatePagerAdapter {
//
//        public PageAdapter(@NonNull FragmentManager fm) {
//            super(fm);
//        }
//
//        @NonNull
//        @Override
//        public Fragment getItem(int position) {
//            return array.get(position);
//        }
//
//        @Override
//        public int getCount() {
//            return array.size();
//        }
//    }
}