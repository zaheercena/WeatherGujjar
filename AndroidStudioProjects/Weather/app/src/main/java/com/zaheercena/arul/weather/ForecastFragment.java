package com.zaheercena.arul.weather;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A fragment containing the weather forecast list view
 */
public class ForecastFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayAdapter<String> forecastAdapter;

    public ForecastFragment() {
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     //* {@link #onAttach(Activity)} and before
     * {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
     * <p/>
     * <p>Note that this can be called while the fragment's activity is
     * still in the process of being created.  As such, you can not rely
     * on things like the activity's content view hierarchy being initialized
     * at this point.  If you want to do work once the activity itself is
     * created, see {@link #onActivityCreated(android.os.Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.forecast_fragment, container, false);

        swipeRefreshLayout = (SwipeRefreshLayout) rootView
                .findViewById(R.id.swipe_refresh_layout);

        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_red_light,
                android.R.color.holo_green_light,
                android.R.color.holo_blue_bright,
                android.R.color.holo_orange_light);

        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refreshWeather();
                    }
                });

        String[] forecastArray = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };

        List<String> weekForecast = new ArrayList<String>(
                Arrays.asList(forecastArray)
        );

        ListView mListView = (ListView) rootView.findViewById(
                R.id.list_view_forecast
        );

        forecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weekForecast
        );

        mListView.setAdapter(forecastAdapter);

        return rootView;
    }

    private void refreshWeather() {
        FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
        fetchWeatherTask.execute("94043");
    }

    /**
     * This class performs an asynchronous fetch from the OpenWeatherMap
     * API as we cannot have network tasks on the main thread.
     */
    class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        /**
         * Used for logging
         */
        private String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p/>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected String[] doInBackground(String... params) {

            // This needs to be declared outside try-catch in order to
            // catch exceptions
            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;

            /**
             * Will contain forecast JSON string
             */
            String forecastJSONString = null;

            String format = "json";
            String units = "metric";
            int num_days = 7;

            try {

                final String URL_BASE = "http://api.openweathermap" +
                        ".org/data/2.5/forecast/daily";
                final String QUERY_PARAM = "q";
                final String UNITS_PARAM = "units";
                final String COUNT_PARAM = "cnt";
                final String MODES_PARAM = "mode";

                Uri builtUri = Uri.parse(URL_BASE).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(MODES_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(COUNT_PARAM,
                                Integer.toString(num_days))
                        .build();
                URL url = new URL(builtUri.toString());

                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();

                // Read the input from the API request here
                InputStream inputStream = httpURLConnection.getInputStream();
                StringBuffer stringBuffer = new StringBuffer();

                if (inputStream == null) {
                    // Nothing to do here, move along
                    return null;
                }
                bufferedReader = new BufferedReader(new InputStreamReader
                        (inputStream));

                // Temporary variable to concatenate results,
                // and add newlines for pretty printing. Note that while
                // this is not required, it makes debugging so much easier.
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line + "\n");
                }

                if (stringBuffer.length() == 0) {
                    // Did not receive any data. Move along.
                    return null;
                }

                // If data was received, copy it to forecastJSONString.
                forecastJSONString = stringBuffer.toString();
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Malformed URL Exception");
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "IO Exception");
                e.printStackTrace();
                return null;
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "IO Exception when closing " +
                                "bufferedReader");
                        e.printStackTrace();
                        return null;
                    }
                }
            }

            try {
                return WeatherDataParser.getWeatherDataFromJson(
                        forecastJSONString,
                        num_days);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error parsing JSON");
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Runs on the UI thread before {@link #doInBackground}. This
         * can be used to start the refresh indicator on the
         * SwipeRefreshLayout
         *
         * @see #onPostExecute
         * @see #doInBackground
         */
        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        /**
         * <p>Applications should preferably override {@link #onCancelled(Object)}.
         * This method is invoked by the default implementation of
         * {@link #onCancelled(Object)}.</p>
         * <p/>
         * <p>Runs on the UI thread after {@link #cancel(boolean)} is invoked and
         * {@link #doInBackground(Object[])} has finished.</p>
         *
         * Used to cancel refresh indicator on SwipeRefreshLayout if
         * the task was cancelled abruptly
         *
         * @see #onCancelled(Object)
         * @see #cancel(boolean)
         * @see #isCancelled()
         */
        @Override
        protected void onCancelled() {
            swipeRefreshLayout.setRefreshing(false);
            super.onCancelled();
        }

        /**
         * <p>Runs on the UI thread after {@link #doInBackground}. The
         * specified result is the value returned by {@link #doInBackground}.</p>
         * <p/>
         * <p>This method won't be invoked if the task was cancelled.</p>
         *
         * Used to update the forecastAdapter to stop indicating
         * refresh on the SwipeRefreshLayout
         *
         * @param forecastStrings The result of the operation computed by
         * {@link #doInBackground}.
         * @see #onPreExecute
         * @see #doInBackground
         * @see #onCancelled(Object)
         */
        @Override
        protected void onPostExecute(String[] forecastStrings) {
            if(forecastStrings != null) {
                forecastAdapter.setNotifyOnChange(false);
                forecastAdapter.clear();
                for(String dayForecastString:forecastStrings) {
                    forecastAdapter.add(dayForecastString);
                }
                forecastAdapter.setNotifyOnChange(true);
                forecastAdapter.notifyDataSetChanged();
            }
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}