package org.torproject.android.ui.onboarding;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.R;
import org.torproject.android.service.util.Prefs;

/**
 Implements the MOAT protocol: Fetches OBFS4 bridges via Meek Azure.

 The bare minimum of the communication is implemented. E.g. no check, if OBFS4 is possible or which
 protocol version the server wants to speak. The first should be always good, as OBFS4 is the most widely
 supported bridge type, the latter should be the same as we requested (0.1.0) anyway.

 API description:
 https://github.com/NullHypothesis/bridgedb#accessing-the-moat-interface
 */
public class MoatActivity extends AppCompatActivity implements View.OnClickListener {

    private static String moatBaseUrl = "https://bridges.torproject.org/moat";

    private ImageView mCaptchaIv;
    private EditText mSolutionEt;

    private String mChallenge;

    private RequestQueue mQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setTitle(getString(R.string.request_bridges));

        mCaptchaIv = findViewById(R.id.captchaIv);
        mSolutionEt = findViewById(R.id.solutionEt);

        findViewById(R.id.requestBt).setOnClickListener(this);

        mQueue = Volley.newRequestQueue(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.moat, menu);

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set to Meek bridge.
        Prefs.setBridgesList("meek");
        Prefs.putBridgesEnabled(true);

        fetchCaptcha();
    }

    @Override
    public void onClick(View view) {
        Log.d(MoatActivity.class.toString(), "Request Bridge!");

        requestBridges(mSolutionEt.getText().toString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            fetchCaptcha();
        }

        return super.onOptionsItemSelected(item);
    }

    private void fetchCaptcha() {
        JsonObjectRequest request = buildRequest("fetch",
                "\"type\": \"client-transports\", \"supported\": [\"obfs4\"]",
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject data = response.getJSONArray("data").getJSONObject(0);
                            mChallenge = data.getString("challenge");

                            byte[] image = Base64.decode(data.getString("image"), Base64.DEFAULT);
                            mCaptchaIv.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));

                        } catch (JSONException e) {
                            Log.d(MoatActivity.class.toString(), "Error decoding answer.");

                            new AlertDialog.Builder(MoatActivity.this)
                                    .setTitle(R.string.error)
                                    .setMessage(e.getLocalizedMessage())
                                    .setNegativeButton(R.string.btn_cancel, new Dialog.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            //Do nothing.
                                        }
                                    })
                                    .show();
                        }
                    }
                });

        if (request != null) {
            mQueue.add(request);
        }
    }

    private void requestBridges(String solution) {
        JsonObjectRequest request = buildRequest("check",
                "\"id\": \"2\", \"type\": \"moat-solution\", \"transport\": \"obfs4\", \"challenge\": \""
                        + mChallenge + "\", \"solution\": \"" + solution + "\", \"qrcode\": \"false\"",
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray bridges = response.getJSONArray("data").getJSONObject(0).getJSONArray("bridges");

                            Log.d(MoatActivity.class.toString(), "Bridges: " + bridges.toString());

                            StringBuilder sb = new StringBuilder();

                            for (int i = 0; i < bridges.length(); i++) {
                                sb.append(bridges.getString(i)).append("\n");
                            }

                            Prefs.setBridgesList(sb.toString());
                            Prefs.putBridgesEnabled(true);

                            MoatActivity.this.finish();

                        } catch (JSONException e) {
                            Log.d(MoatActivity.class.toString(), "Error decoding answer: " + response.toString());

                            new AlertDialog.Builder(MoatActivity.this)
                                    .setTitle(R.string.error)
                                    .setMessage(e.getLocalizedMessage())
                                    .setNegativeButton(R.string.btn_cancel, new Dialog.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            //Do nothing.
                                        }
                                    })
                                    .show();
                        }
                    }
                });

        if (request != null) {
            mQueue.add(request);
        }
    }

    private JsonObjectRequest buildRequest(String endpoint, String payload, Response.Listener<JSONObject> listener) {
        JSONObject requestBody;

        try {
            requestBody = new JSONObject("{\"data\": [{\"version\": \"0.1.0\", " + payload + "}]}");
        } catch (JSONException e) {
            return null;
        }

        Log.d(MoatActivity.class.toString(), "Request: " + requestBody.toString());

        return new JsonObjectRequest(
                Request.Method.POST,
                moatBaseUrl + "/" + endpoint,
                requestBody,
                listener,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(MoatActivity.class.toString(), "Error response.");

                        new AlertDialog.Builder(MoatActivity.this)
                                .setTitle(R.string.error)
                                .setMessage(error.getLocalizedMessage())
                                .setNegativeButton(R.string.btn_cancel, new Dialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //Do nothing.
                                    }
                                })
                                .show();
                    }
                }
        ) {
            public String getBodyContentType() {
                return "application/vnd.api+json";
            }
        };
    }
}
