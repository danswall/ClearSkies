package com.danwall.clearskies;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private final int MAX_MARKERS = 1;
    private final int DAYS_TO_FORECAST = 5;
    private final float DEFAULT_ZOOM = 8.0f;

    private GoogleMap mMap;
    private ArrayList<Marker> mMarkers;
    private LatLngBounds mBounds;
    private float mZoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMarkers = new ArrayList<>();
        mZoom = DEFAULT_ZOOM;

        // Move camera to Saint Louis by default
        LatLng saintLouis = new LatLng(38.627, -90.199);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(saintLouis, mZoom));
        mBounds = new LatLngBounds.Builder().include(saintLouis).build();

        // Set initial markers
        for (int i = 0; i < MAX_MARKERS; i++) {
            createMarker();
        }

        // create custom info window
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                // Get views
                View infoView = getLayoutInflater().inflate(R.layout.custom_infowindow, null);
                TextView[] tempViews = new TextView[5];
                tempViews[0] = (TextView) infoView.findViewById(R.id.temp1);
                tempViews[1] = (TextView) infoView.findViewById(R.id.temp2);
                tempViews[2] = (TextView) infoView.findViewById(R.id.temp3);
                tempViews[3] = (TextView) infoView.findViewById(R.id.temp4);
                tempViews[4] = (TextView) infoView.findViewById(R.id.temp5);
                ImageView[] iconViews = new ImageView[5];
                iconViews[0] = (ImageView) infoView.findViewById(R.id.icon1);
                iconViews[1] = (ImageView) infoView.findViewById(R.id.icon2);
                iconViews[2] = (ImageView) infoView.findViewById(R.id.icon3);
                iconViews[3] = (ImageView) infoView.findViewById(R.id.icon4);
                iconViews[4] = (ImageView) infoView.findViewById(R.id.icon5);

                // Get forecast data
                DailyWeather[] forecast = (DailyWeather[]) marker.getTag();

                // Set the temperatures and icons
                for (int i = 0; i < 5; i++) {
                    tempViews[i].setText(Integer.toString(forecast[i].getTemperature()));

                    Drawable iconDrawable = getResources().getDrawable(forecast[i].getIconId());
                    iconViews[i].setImageDrawable(iconDrawable);
                }

                return infoView;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                // get new bounds
                mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                float currentZoom = mMap.getCameraPosition().zoom;

                // check each marker
                for (int i = 0; i < MAX_MARKERS; i++) {
                    Marker marker = mMarkers.get(i);

                    // If zoom changed or marker is off screen..
                    if (currentZoom != mZoom || (!mBounds.contains(marker.getPosition()))) {
                        // ..remove the marker(s) and create new ones
                        marker.setVisible(false);
                        mMarkers.remove(i);
                        createMarker();
                        mZoom = currentZoom;
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    private void createMarker() {
        // Create random Lat/Lon from map bounds
        Double maxLat = mBounds.northeast.latitude;
        Double minLat = mBounds.southwest.latitude;
        Double maxLng = mBounds.northeast.longitude;
        Double minLng = mBounds.southwest.longitude;
        final Double newLat = Math.random() * (maxLat - minLat) + minLat;
        final Double newLng = Math.random() * (maxLng - minLng) + minLng;

        // Get forecast for this location
        Forecast forecast = new Forecast(DAYS_TO_FORECAST, newLat, newLng);
        forecast.getForecast(this, new ForecastCallbackInterface() {
            @Override
            public void onForecastRetrieved(DailyWeather[] forecast) {
                // Create the temperature icon
                int todaysTemperature = forecast[0].getTemperature();
                Bitmap temperatureIcon = createTemperatureIcon(todaysTemperature);

                // Create the custom marker
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(newLat, newLng))
                        .icon(BitmapDescriptorFactory.fromBitmap(temperatureIcon)));
                marker.setTag(forecast);

                // Add marker to map
                mMarkers.add(marker);
            }
        });
    }

    private Bitmap createTemperatureIcon(int temperature) {
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        Bitmap iconBitmap = Bitmap.createBitmap(250, 150, config);
        Canvas canvas = new Canvas(iconBitmap);

        Paint fillColor = new Paint();
        fillColor.setTextSize(140);
        fillColor.setColor(Color.DKGRAY);
        fillColor.setFakeBoldText(true);

        Paint outlineColor = new Paint();
        outlineColor.setTextSize(140);
        outlineColor.setColor(Color.WHITE);
        outlineColor.setStyle(Paint.Style.STROKE);
        outlineColor.setStrokeWidth(4);
        outlineColor.setFakeBoldText(true);

        canvas.drawText(temperature + "°", 20, 110, fillColor);
        canvas.drawText(temperature + "°", 20, 110, outlineColor);

        return iconBitmap;
    }
}

interface ForecastCallbackInterface {
    void onForecastRetrieved(DailyWeather[] forecast);
}