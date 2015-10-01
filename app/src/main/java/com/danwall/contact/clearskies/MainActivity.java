package com.danwall.contact.clearskies;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends FragmentActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static final int MAX_MARKERS = 10;
    private ArrayList<Marker> mMarkers;
    private LatLngBounds mBounds;
    private float mZoom;

    private CurrentWeather mCurrentWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMapIfNeeded();
        mZoom = mMap.getCameraPosition().zoom;
        mMarkers = new ArrayList<Marker>();
        for (int i = 0; i < MAX_MARKERS; i++) {
            // TODO replace with createMarker();
            mMarkers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(38.627 + i, -90.199))));
        }

        // create custom info window
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                View infoView = getLayoutInflater().inflate(R.layout.custom_infowindow, null);

                TextView temp1 = (TextView) infoView.findViewById(R.id.temp1);
                temp1.setText(Integer.toString(mCurrentWeather.getTemperature()));

                ImageView icon1 = (ImageView) infoView.findViewById(R.id.icon1);
                Drawable iconDrawable1 = getResources().getDrawable(mCurrentWeather.getIconId());
                icon1.setImageDrawable(iconDrawable1);

                /* TODO use after implementing dailyWeather
                TextView temp2 = (TextView) infoView.findViewById(R.id.temp2);

                temp2.setText(Integer.toString(mCurrentWeather.getDailyWeather()[1].getTemperature()));

                ImageView icon2 = (ImageView) infoView.findViewById(R.id.icon2);
                Drawable iconDrawable2 = getResources().getDrawable(mCurrentWeather.getDailyWeather()[1].getIconId());
                icon2.setImageDrawable(iconDrawable2);
                */

                return infoView;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                // get new bounds
                mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                float currentZoom = mMap.getCameraPosition().zoom;

                // check each marker
                for (int i = 0; i < MAX_MARKERS; i++) {
                    // if zoom has not changed
                    if (currentZoom == mZoom) {
                        // if marker is not in bounds, create new one
                        if (!mBounds.contains(mMarkers.get(i).getPosition())) {
                            mMarkers.get(i).setVisible(false);
                            mMarkers.remove(i);
                            createMarker();
                        }
                    } else { // zoom changed
                        mMarkers.get(i).setVisible(false);
                        mMarkers.remove(i);
                        createMarker();
                        mZoom = currentZoom;
                    }
                }
            }
        });
    }

    private void createMarker() {
        Double maxLat = mBounds.northeast.latitude;
        Double minLat = mBounds.southwest.latitude;
        Double maxLng = mBounds.northeast.longitude;
        Double minLng = mBounds.southwest.longitude;
        Double newLat = Math.random() * (maxLat - minLat) + minLat;
        Double newLng = Math.random() * (maxLng - minLng) + minLng;

        getForecast(newLat, newLng);
        // TODO can access mBounds just fine, but cannot access mCurrentWeather from here
        Log.v(TAG, mBounds.toString());

        // TODO update info window with temp
        // TODO update marker with icon
        mMarkers.add(mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(newLat, newLng))
                        .title("Unable to get forecast")
                        .snippet("Please try again")
        ));


    }

    private void getForecast(double latitude, double longitude) {
        String apiKey = "bdad704d77fcf083ea7036a6ebfd3c4a";

        String forecastUrl = "https://api.forecast.io/forecast/" +
                apiKey + "/" + latitude + "," + longitude;

        if (isNetworkAvailable()) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Toast.makeText(MainActivity.this, "Unable to connect to forecast.io",
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to retrieve data.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        } else {
            // no connection so inform user
            Toast.makeText(this, R.string.network_unavailable_message,
                    Toast.LENGTH_LONG).show();
        }
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG, "From JSON: " + timezone);

        // get current weather
        JSONObject currently = forecast.getJSONObject("currently");
        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);

        Log.d(TAG, currentWeather.getFormattedTime());

        /* TODO get daily weather
        JSONArray daily = forecast.getJSONObject("daily").getJSONArray("data");
        CurrentWeather[] dailyWeather = new CurrentWeather[4];
        for (int i = 0; i < dailyWeather.length; i++) {
            dailyWeather[i] = new CurrentWeather();
            dailyWeather[i].setTemperature(daily.getJSONObject(i).getDouble("temperature"));
        }

        currentWeather.setDailyWeather(dailyWeather);
        */

        return currentWeather;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manageer = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manageer.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }

        return isAvailable;
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        // start camera over Saint Louis
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(38.627, -90.199), 9));
    }
}
