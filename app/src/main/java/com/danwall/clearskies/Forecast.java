package com.danwall.clearskies;

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

    public Forecast(int daysToForecast, double latitude, double longitude) {
        mForecast = new DailyWeather[daysToForecast];
        getForecastData(latitude, longitude);
    }

    public DailyWeather[] getForecast() {
        return mForecast;
    }

    private void getForecastData(double latitude, double longitude) {
        String apiKey = "bdad704d77fcf083ea7036a6ebfd3c4a";

        String forecastUrl = "https://api.forecast.io/forecast/" +
                apiKey + "/" + latitude + "," + longitude;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(forecastUrl)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    if (response.isSuccessful()) {
                        setDetails(jsonData);
                    }
                } catch (Exception ex) {
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