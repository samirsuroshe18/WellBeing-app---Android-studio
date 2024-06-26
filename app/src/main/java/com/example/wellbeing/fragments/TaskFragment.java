package com.example.wellbeing.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.wellbeing.PostActivity;
import com.example.wellbeing.R;
import com.example.wellbeing.TaskCompletedActivity;
import com.example.wellbeing.TaskIncompletedActivity;
import com.example.wellbeing.UtilsServices.SharedPreferenceClass;
import com.example.wellbeing.models.TaskModel;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class TaskFragment extends Fragment {
    public static final int TIMEOUT_MS = 10000;
    public static final int MAX_RETRIES = 2;
    public static final float BACKOFF_MULT = 2.0f;
    TextView describeTV, timeLeftTV, taskTitleTV, taskCreatedUserName, timelineTV;
    ImageView taskImage;
    VideoView taskVideo;
    CircleImageView taskCreatedProfile;
    AppCompatButton nextBtn,acceptBtn, acceptedBtn, postBtn;
    SharedPreferenceClass sharedPreferenceClass;
    String acceptFlag, statusFlag, accessToken, taskId;
    LinearLayout timer;
    ArrayList<TaskModel> tasks;
    ConstraintLayout container;
    LottieAnimationView lottieAnimationView;
    private static TaskFragment instance;
    public TaskFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup containe, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_task, containe, false);

        container = view.findViewById(R.id.container);
        lottieAnimationView = view.findViewById(R.id.loadingAnim);
        describeTV = view.findViewById(R.id.describeTV);
        taskTitleTV = view.findViewById(R.id.taskTitleTV);
        timelineTV = view.findViewById(R.id.timelineTV);
        taskImage = view.findViewById(R.id.taskImage);
        taskVideo = view.findViewById(R.id.taskVideo);
        taskCreatedProfile = view.findViewById(R.id.taskCreatedProfile);
        taskCreatedUserName = view.findViewById(R.id.taskCreatedUserName);
        nextBtn = view.findViewById(R.id.nextBtn);
        acceptBtn = view.findViewById(R.id.acceptBtn);
        acceptedBtn = view.findViewById(R.id.acceptedBtn);
        postBtn = view.findViewById(R.id.postBtn);
        timeLeftTV = view.findViewById(R.id.timeLeftTV);
        timer = view.findViewById(R.id.timer);

        sharedPreferenceClass = new SharedPreferenceClass(requireContext());
        tasks = new ArrayList<>();

        acceptFlag = sharedPreferenceClass.getValue_string("acceptFlag");
        statusFlag = sharedPreferenceClass.getValue_string("statusFlag");
        accessToken = sharedPreferenceClass.getValue_string("accessToken");

        if (acceptFlag.equals("true")){
            nextBtn.setVisibility(View.INVISIBLE);
            acceptBtn.setVisibility(View.INVISIBLE);
            acceptedBtn.setVisibility(View.VISIBLE);
            postBtn.setVisibility(View.VISIBLE);
            timer.setVisibility(View.VISIBLE);
            getTaskCurrentStatus();
        }else {
            getTask();
        }

        if (statusFlag.equals("completed")){
            startActivity(new Intent(getContext(), TaskCompletedActivity.class));
        }

        if (statusFlag.equals("incompleted")){
            startActivity(new Intent(getContext(), TaskIncompletedActivity.class));
        }

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPreferenceClass.setValue_string("acceptFlag", "true");
                taskId = tasks.get(tasks.size()-1).get_id();
                sharedPreferenceClass.setValue_string("taskId", taskId);
                nextBtn.setVisibility(View.INVISIBLE);
                acceptBtn.setVisibility(View.INVISIBLE);
                acceptedBtn.setVisibility(View.VISIBLE);
                timer.setVisibility(View.VISIBLE);
                postBtn.setVisibility(View.VISIBLE);
                taskAccepted(taskId);
            }
        });

        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getContext(), PostActivity.class));
            }
        });

        describeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int lines = describeTV.getMaxLines() + 2;
                describeTV.setMaxLines(lines);
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tasks.clear();
                getTask();
            }
        });

        taskVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskVideo.start();
            }
        });

        return  view;

    }

    public void getTask(){
        container.setVisibility(View.INVISIBLE);
        lottieAnimationView.setVisibility(View.VISIBLE);

        String apiKey = "https://wellbeing-backend-5f8e.onrender.com/api/v1/usertaskinfo/get-task";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiKey, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response != null) {
                        JSONObject dataObject = (JSONObject) response.getJSONArray("data").get(0);
                        TaskModel taskModel = new TaskModel();

                        taskModel.set_id(dataObject.getString("_id"));
                        taskModel.setTitle(dataObject.getString("title"));
                        taskModel.setDescription(dataObject.getString("description"));
                        taskModel.setMediaType(dataObject.getString("mediaType"));
                        taskModel.setTaskReference(dataObject.getString("taskReference"));
                        taskModel.setTimeToComplete(String.valueOf(dataObject.getInt("timeToComplete")));
                        taskModel.setUserId(dataObject.getJSONObject("createdBy").getString("_id"));
                        taskModel.setUserName(dataObject.getJSONObject("createdBy").getString("userName"));
                        taskModel.setPofilePicture(dataObject.getJSONObject("createdBy").getString("profilePicture"));

                        tasks.add(taskModel);
                        sharedPreferenceClass.setValue_string("taskId", dataObject.getString("_id"));
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);

                        taskTitleTV.setText(tasks.get(tasks.size()-1).getTitle());
                        describeTV.setText(tasks.get(tasks.size()-1).getDescription());
                        if (tasks.get(tasks.size()-1).getMediaType().equals("video")){
                            taskImage.setVisibility(View.INVISIBLE);
                            taskVideo.setVisibility(View.VISIBLE);
                            taskVideo.setVideoURI(Uri.parse(tasks.get(tasks.size()-1).getTaskReference()));
                        }else {
                            taskImage.setVisibility(View.VISIBLE);
                            taskVideo.setVisibility(View.INVISIBLE);
                            Picasso.get().load(tasks.get(tasks.size()-1).getTaskReference()).into(taskImage);
                        }
                        Picasso.get().load(tasks.get(tasks.size()-1).getPofilePicture()).into(taskCreatedProfile);
                        taskCreatedUserName.setText(tasks.get(tasks.size()-1).getUserName());
                        timelineTV.setText(tasks.get(tasks.size()-1).getTimeToComplete());

                        sharedPreferenceClass.setValue_string("List", String.valueOf(tasks.get(tasks.size()-1)));
                        Log.d("getTaskData : ", String.valueOf(dataObject));

                    } else {
                        // Handle the case where "accessToken" key is not present in the JSON response
                        Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    container.setVisibility(View.VISIBLE);
                    lottieAnimationView.setVisibility(View.INVISIBLE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    }
                } else {
                    String result = null;
                    try {
                        result = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                        Log.d("Error : ", result);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                    try {
                        JSONObject response = new JSONObject(result);
                        String status = response.getString("status");
                        String message = response.getString("message");

                        Log.e("Error Status", status);
                        Log.e("Error Message", message);

                        if (networkResponse.statusCode == 404) {
                            errorMessage = "Resource not found";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        } else if (networkResponse.statusCode == 401) {
                            errorMessage = message+" Unauthorized";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        } else if (networkResponse.statusCode == 400) {
                            errorMessage = message+ "Bad request";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        } else if (networkResponse.statusCode == 500) {
                            errorMessage = message+" Something is getting wrong";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    }
                    container.setVisibility(View.VISIBLE);
                    lottieAnimationView.setVisibility(View.INVISIBLE);
                }
                Log.i("Error", errorMessage);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                error.printStackTrace();
                container.setVisibility(View.VISIBLE);
                lottieAnimationView.setVisibility(View.INVISIBLE);

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer "+accessToken);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonObjectRequest);

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                MAX_RETRIES,
                BACKOFF_MULT
        ));
    }

    public void taskAccepted(String taskId){
        container.setVisibility(View.INVISIBLE);
        lottieAnimationView.setVisibility(View.VISIBLE);

        String apiKey = "https://wellbeing-backend-5f8e.onrender.com/api/v1/usertaskinfo/accept-task";

        final HashMap<String, String> params = new HashMap<>();
        params.put("taskInfo", taskId);
        params.put("status", "pending");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiKey, new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response != null) {

                        JSONObject dataObject = response.getJSONObject("data");
                        sharedPreferenceClass.setValue_string("acceptedTaskId", dataObject.getString("_id"));
                        String remainingTime = dataObject.getString("remainingTime");

                        timeLeftTV.setText(remainingTime);
                        Log.d("acceptedTaskData : ", String.valueOf(dataObject));
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    } else {
                        // Handle the case where "accessToken" key is not present in the JSON response
                        Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    container.setVisibility(View.VISIBLE);
                    lottieAnimationView.setVisibility(View.INVISIBLE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    }
                } else {
                    String result = null;
                    try {
                        result = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                        Log.d("Error : ", result);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                    try {
                        JSONObject response = new JSONObject(result);
                        String status = response.getString("status");
                        String message = response.getString("message");

                        Log.e("Error Status", status);
                        Log.e("Error Message", message);

                        if (networkResponse.statusCode == 404) {
                            errorMessage = "Resource not found";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        } else if (networkResponse.statusCode == 401) {
                            errorMessage = message+" Unauthorized";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        } else if (networkResponse.statusCode == 400) {
                            errorMessage = message+ "Bad request";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        } else if (networkResponse.statusCode == 500) {
                            errorMessage = message+" Something is getting wrong";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    }
                    container.setVisibility(View.VISIBLE);
                    lottieAnimationView.setVisibility(View.INVISIBLE);
                }
                Log.i("Error", errorMessage);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                error.printStackTrace();
                container.setVisibility(View.VISIBLE);
                lottieAnimationView.setVisibility(View.INVISIBLE);

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer "+accessToken);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonObjectRequest);

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                MAX_RETRIES,
                BACKOFF_MULT
        ));
    }

    public void getTaskCurrentStatus(){
        container.setVisibility(View.INVISIBLE);
        lottieAnimationView.setVisibility(View.VISIBLE);
        String apiKey = "https://wellbeing-backend-5f8e.onrender.com/api/v1/usertaskinfo/get-status";

        String _id = sharedPreferenceClass.getValue_string("acceptedTaskId");
        final HashMap<String, String> params = new HashMap<>();
        params.put("_id", _id);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiKey, new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response != null) {
                        JSONObject dataObject = response.getJSONObject("data");
                        String status = dataObject.getString("status");
                        Log.d("currentTaskStatusData : ", String.valueOf(dataObject));
                        if (status.equals("incompleted")){
                            sharedPreferenceClass.setValue_string("statusFlag", "incompleted");
                        }

                        String time = dataObject.getString("remainingTime");
                        if (time.contains("-")){
                            sharedPreferenceClass.setValue_string("statusFlag", "incompleted");
                        }

                        timeLeftTV.setText(dataObject.getString("remainingTime"));
                        taskTitleTV.setText(dataObject.getJSONObject("taskInfo").getString("title"));
                        describeTV.setText(dataObject.getJSONObject("taskInfo").getString("description"));

                        String mediaType = dataObject.getJSONObject("taskInfo").getString("mediaType");
                        if (mediaType.equals("video")){
                            taskImage.setVisibility(View.INVISIBLE);
                            taskVideo.setVisibility(View.VISIBLE);
                            taskVideo.setVideoURI(Uri.parse(dataObject.getJSONObject("taskInfo").getString("taskReference")));
                        }else {
                            taskImage.setVisibility(View.VISIBLE);
                            taskVideo.setVisibility(View.INVISIBLE);
                            Picasso.get().load(dataObject.getJSONObject("taskInfo").getString("taskReference")).into(taskImage);
                        }

                        Picasso.get().load(dataObject.getJSONObject("taskInfo").getJSONObject("createdBy").getString("profilePicture")).into(taskCreatedProfile);
                        taskCreatedUserName.setText(dataObject.getJSONObject("taskInfo").getJSONObject("createdBy").getString("userName"));
                        timelineTV.setText(String.valueOf(dataObject.getJSONObject("taskInfo").getInt("timeToComplete")));


                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    } else {
                        // Handle the case where "accessToken" key is not present in the JSON response
                        Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    container.setVisibility(View.VISIBLE);
                    lottieAnimationView.setVisibility(View.INVISIBLE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    }
                } else {
                    String result = null;
                    try {
                        result = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                        Log.d("Error : ", result);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                    try {
                        JSONObject response = new JSONObject(result);
                        String status = response.getString("status");
                        String message = response.getString("message");

                        Log.e("Error Status", status);
                        Log.e("Error Message", message);

                        if (networkResponse.statusCode == 404) {
                            errorMessage = "Resource not found";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        } else if (networkResponse.statusCode == 401) {
                            errorMessage = message+" Unauthorized";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        } else if (networkResponse.statusCode == 400) {
                            errorMessage = message+ "Bad request";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        } else if (networkResponse.statusCode == 500) {
                            errorMessage = message+" Something is getting wrong";
                            container.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.INVISIBLE);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        container.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    }
                    container.setVisibility(View.VISIBLE);
                    lottieAnimationView.setVisibility(View.INVISIBLE);
                }
                Log.i("Error", errorMessage);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                error.printStackTrace();
                container.setVisibility(View.VISIBLE);
                lottieAnimationView.setVisibility(View.INVISIBLE);

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer "+accessToken);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonObjectRequest);

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                MAX_RETRIES,
                BACKOFF_MULT
        ));
    }
}