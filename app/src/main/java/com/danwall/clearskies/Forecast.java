package com.danwall.clearskies;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Forecast {
    private DailyWeather[] mForecast;
    private double mLatitude;
    private double mLongitude;

    public Forecast(int daysToForecast, double latitude, double longitude) {
        mForecast = new DailyWeather[daysToForecast];
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public void getForecast(final Context context, final ForecastCallbackInterface callback) {
        String apiKey = "7fe32d1b7875b316afe9457fb173f6e9";

        String forecastUrl = "https://api.darksky.net/forecast/" +
                apiKey + "/" + mLatitude + "," + mLongitude;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(forecastUrl)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            Handler mainHandler = new Handler(context.getMainLooper());

            @Override
            public void onFailure(Request request, IOException e) {
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    if (response.isSuccessful()) {
                        setDetails(jsonData);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onForecastRetrieved(mForecast);
                            }
                        });
                    }
                } catch (Exception ex) {
                    Log.d("uh", "oh");
                }
            }
        });
    }

    private void setDetails(String jsonData) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONObject currently = jsonObject.getJSONObject("currently");
        JSONObject daily = jsonObject.getJSONObject("daily");
        JSONArray dailyData = daily.getJSONArray("data");

        DailyWeather firstDayWeather = new DailyWeather();

        firstDayWeather.setTemperature(currently.getDouble("temperature"));
        firstDayWeather.setIcon(currently.getString("icon"));

        mForecast[0] = firstDayWeather;

        for (int i = 0, n = mForecast.length; i < n; i++) {
            DailyWeather nextDayWeather = new DailyWeather();
            JSONObject nextJSONObject = (JSONObject) dailyData.get(i);

            nextDayWeather.setTemperature(nextJSONObject.getDouble("temperatureMax"));
            nextDayWeather.setIcon(nextJSONObject.getString("icon"));

            mForecast[i] = nextDayWeather;
        }
    }
}