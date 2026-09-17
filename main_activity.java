package com.example.waterqualitymonitor;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // =========================================================
    // TEXT VIEWS
    // =========================================================

    TextView tvConnection;
    TextView tvUpdated;

    TextView tvPrediction;
    TextView tvFinalStatus;
    TextView tvRisk;
    TextView tvRiskLevel;

    TextView tvPh;
    TextView tvTds;
    TextView tvTurbidity;
    TextView tvRain;

    TextView tvPhStatus;
    TextView tvTdsStatus;
    TextView tvTurbidityStatus;
    TextView tvRainStatus;

    TextView tvMlPrediction;
    TextView tvInputSummary;
    TextView tvReason;

    TextView tvAlert;
    TextView tvAlertDetail;

    TextView tvHistory;


    // =========================================================
    // PROGRESS
    // =========================================================

    ProgressBar riskProgressBar;


    // =========================================================
    // BUTTONS
    // =========================================================

    Button btnDemoSafe;
    Button btnDemoUnsafe;
    Button btnClearHistory;


    // =========================================================
    // COLLAPSIBLE CONTENT
    // =========================================================

    LinearLayout contentQuality;
    LinearLayout contentSensors;
    LinearLayout contentML;
    LinearLayout contentAlert;
    LinearLayout contentTrends;
    LinearLayout contentHistory;

    TextView arrowQuality;
    TextView arrowSensors;
    TextView arrowML;
    TextView arrowAlert;
    TextView arrowTrends;
    TextView arrowHistory;


    // =========================================================
    // CONNECTION CHIP
    // =========================================================

    LinearLayout connectionChip;


    // =========================================================
    // CUSTOM GRAPHS
    // =========================================================

    GraphView phGraph;
    GraphView tdsGraph;
    GraphView turbidityGraph;
    GraphView riskGraph;


    // =========================================================
    // HISTORY / GRAPH DATA
    // =========================================================

    private final ArrayList<Reading> readings =
            new ArrayList<>();

    private static final int MAX_HISTORY = 20;

    private long lastHistoryTime = 0;

    private static final long HISTORY_INTERVAL = 3000;


    // =========================================================
    // SERVER
    // =========================================================

    private static final String SERVER_URL =
            "http://10.216.92.252:5003/latest";

    private static final long UPDATE_INTERVAL = 1000;


    // =========================================================
    // HANDLER
    // =========================================================

    private final Handler handler =
            new Handler();

    private boolean running = true;


    // =========================================================
    // DEMO MODE
    // =========================================================

    private boolean demoMode = false;

    private static final long DEMO_DURATION = 8000;

    private final Runnable endDemoRunnable = () -> {

        demoMode = false;

        styleConnectionChip(true);

        tvConnection.setText(
                "●  Live Mode"
        );
    };


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_main
        );

        initializeViews();

        setupExpandableBlocks();

        setupButtons();

        startLiveUpdates();
    }


    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initializeViews() {

        tvConnection =
                findViewById(R.id.tvConnection);

        tvUpdated =
                findViewById(R.id.tvUpdated);

        tvPrediction =
                findViewById(R.id.tvPrediction);

        tvFinalStatus =
                findViewById(R.id.tvFinalStatus);

        tvRisk =
                findViewById(R.id.tvRisk);

        tvRiskLevel =
                findViewById(R.id.tvRiskLevel);


        tvPh =
                findViewById(R.id.tvPh);

        tvTds =
                findViewById(R.id.tvTds);

        tvTurbidity =
                findViewById(R.id.tvTurbidity);

        tvRain =
                findViewById(R.id.tvRain);


        tvPhStatus =
                findViewById(R.id.tvPhStatus);

        tvTdsStatus =
                findViewById(R.id.tvTdsStatus);

        tvTurbidityStatus =
                findViewById(R.id.tvTurbidityStatus);

        tvRainStatus =
                findViewById(R.id.tvRainStatus);


        tvMlPrediction =
                findViewById(R.id.tvMlPrediction);

        tvInputSummary =
                findViewById(R.id.tvInputSummary);

        tvReason =
                findViewById(R.id.tvReason);


        tvAlert =
                findViewById(R.id.tvAlert);

        tvAlertDetail =
                findViewById(R.id.tvAlertDetail);


        tvHistory =
                findViewById(R.id.tvHistory);


        riskProgressBar =
                findViewById(
                        R.id.riskProgressBar
                );


        btnDemoSafe =
                findViewById(
                        R.id.btnDemoSafe
                );

        btnDemoUnsafe =
                findViewById(
                        R.id.btnDemoUnsafe
                );

        btnClearHistory =
                findViewById(
                        R.id.btnClearHistory
                );


        contentQuality =
                findViewById(
                        R.id.contentQuality
                );

        contentSensors =
                findViewById(
                        R.id.contentSensors
                );

        contentML =
                findViewById(
                        R.id.contentML
                );

        contentAlert =
                findViewById(
                        R.id.contentAlert
                );

        contentTrends =
                findViewById(
                        R.id.contentTrends
                );

        contentHistory =
                findViewById(
                        R.id.contentHistory
                );


        arrowQuality =
                findViewById(
                        R.id.arrowQuality
                );

        arrowSensors =
                findViewById(
                        R.id.arrowSensors
                );

        arrowML =
                findViewById(
                        R.id.arrowML
                );

        arrowAlert =
                findViewById(
                        R.id.arrowAlert
                );

        arrowTrends =
                findViewById(
                        R.id.arrowTrends
                );

        arrowHistory =
                findViewById(
                        R.id.arrowHistory
                );


        connectionChip =
                findViewById(
                        R.id.connectionChip
                );


        phGraph =
                findViewById(
                        R.id.phGraph
                );

        tdsGraph =
                findViewById(
                        R.id.tdsGraph
                );

        turbidityGraph =
                findViewById(
                        R.id.turbidityGraph
                );

        riskGraph =
                findViewById(
                        R.id.riskGraph
                );


        styleConnectionChip(false);

        tvUpdated.setText(
                "Waiting for live data..."
        );
    }


    // =========================================================
    // COLLAPSIBLE BLOCKS
    // =========================================================

    private void setupExpandableBlocks() {

        findViewById(
                R.id.headerQuality
        ).setOnClickListener(
                v -> toggleBlock(
                        contentQuality,
                        arrowQuality
                )
        );


        findViewById(
                R.id.headerSensors
        ).setOnClickListener(
                v -> toggleBlock(
                        contentSensors,
                        arrowSensors
                )
        );


        findViewById(
                R.id.headerML
        ).setOnClickListener(
                v -> toggleBlock(
                        contentML,
                        arrowML
                )
        );


        findViewById(
                R.id.headerAlert
        ).setOnClickListener(
                v -> toggleBlock(
                        contentAlert,
                        arrowAlert
                )
        );


        findViewById(
                R.id.headerTrends
        ).setOnClickListener(
                v -> toggleBlock(
                        contentTrends,
                        arrowTrends
                )
        );


        findViewById(
                R.id.headerHistory
        ).setOnClickListener(
                v -> toggleBlock(
                        contentHistory,
                        arrowHistory
                )
        );
    }


    private void toggleBlock(
            LinearLayout content,
            TextView arrow
    ) {

        if (content.getVisibility()
                == View.VISIBLE) {

            content.setVisibility(
                    View.GONE
            );

            arrow.setText("▶");

        } else {

            content.setVisibility(
                    View.VISIBLE
            );

            arrow.setText("▼");
        }
    }


    // =========================================================
    // BUTTONS
    // =========================================================

    private void setupButtons() {

        btnDemoSafe.setOnClickListener(
                v -> {

                    demoMode = true;

                    handler.removeCallbacks(
                            endDemoRunnable
                    );

                    handler.postDelayed(
                            endDemoRunnable,
                            DEMO_DURATION
                    );

                    tvConnection.setText(
                            "●  Demo Mode"
                    );

                    styleConnectionChip(true);

                    showData(
                            7.20,
                            120.0,
                            1.0,
                            "No Rain",
                            "Safe",
                            "Safe",
                            5.0,
                            false,
                            false,
                            "No active water-quality alert"
                    );
                }
        );


        btnDemoUnsafe.setOnClickListener(
                v -> {

                    demoMode = true;

                    handler.removeCallbacks(
                            endDemoRunnable
                    );

                    handler.postDelayed(
                            endDemoRunnable,
                            DEMO_DURATION
                    );

                    tvConnection.setText(
                            "●  Demo Mode"
                    );

                    styleConnectionChip(true);

                    showData(
                            8.80,
                            900.0,
                            1.0,
                            "Rain Detected",
                            "Unsafe",
                            "Unsafe",
                            100.0,
                            true,
                            true,
                            "CRITICAL ALERT: pH and TDS exceed project thresholds"
                    );
                }
        );


        btnClearHistory.setOnClickListener(
                v -> {

                    readings.clear();

                    updateGraphs();

                    tvHistory.setText(
                            "No readings yet"
                    );
                }
        );
    }


    // =========================================================
    // LIVE UPDATES
    // =========================================================

    private void startLiveUpdates() {

        handler.post(
                new Runnable() {

                    @Override
                    public void run() {

                        if (!running) {
                            return;
                        }

                        getLatestData();

                        handler.postDelayed(
                                this,
                                UPDATE_INTERVAL
                        );
                    }
                }
        );
    }


    // =========================================================
    // GET /latest
    // =========================================================

    private void getLatestData() {

        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    try {

                        URL url =
                                new URL(
                                        SERVER_URL
                                );

                        connection =
                                (HttpURLConnection)
                                        url.openConnection();

                        connection.setRequestMethod(
                                "GET"
                        );

                        connection.setConnectTimeout(
                                5000
                        );

                        connection.setReadTimeout(
                                5000
                        );


                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                connection.getInputStream()
                                        )
                                );


                        StringBuilder response =
                                new StringBuilder();

                        String line;

                        while (
                                (line =
                                        reader.readLine())
                                        != null
                        ) {

                            response.append(
                                    line
                            );
                        }

                        reader.close();


                        JSONObject data =
                                new JSONObject(
                                        response.toString()
                                );


                        double pH =
                                data.getDouble(
                                        "pH"
                                );

                        double tds =
                                data.getDouble(
                                        "TDS_ppm"
                                );

                        double turbidity =
                                data.getDouble(
                                        "Turbidity_NTU"
                                );

                        String rain =
                                data.optString(
                                        "rain",
                                        "Unknown"
                                );


                        String prediction =
                                data.optString(
                                        "prediction",
                                        "Waiting"
                                );


                        String mlPrediction =
                                data.optString(
                                        "ml_prediction",
                                        prediction
                                );


                        double risk =
                                data.optDouble(
                                        "risk_score",
                                        0
                                );


                        boolean phHigh =
                                data.optBoolean(
                                        "ph_threshold_exceeded",
                                        pH > 8.5
                                );


                        boolean tdsHigh =
                                data.optBoolean(
                                        "tds_threshold_exceeded",
                                        tds > 500
                                );


                        String alert =
                                data.optString(
                                        "alert",
                                        "No active water-quality alert"
                                );


                        runOnUiThread(
                                () -> {

                                    if (!demoMode) {

                                        styleConnectionChip(
                                                true
                                        );

                                        showData(
                                                pH,
                                                tds,
                                                turbidity,
                                                rain,
                                                prediction,
                                                mlPrediction,
                                                risk,
                                                phHigh,
                                                tdsHigh,
                                                alert
                                        );
                                    }
                                }
                        );


                    } catch (Exception e) {

                        runOnUiThread(
                                () -> {

                                    if (!demoMode) {

                                        styleConnectionChip(
                                                false
                                        );

                                        tvReason.setText(
                                                "Unable to receive data from the Flask backend."
                                        );
                                    }
                                }
                        );


                    } finally {

                        if (connection != null) {

                            connection.disconnect();
                        }
                    }

                }
        ).start();
    }


    // =========================================================
    // DISPLAY DATA
    // =========================================================

    private void showData(
            double pH,
            double tds,
            double turbidity,
            String rain,
            String prediction,
            String mlPrediction,
            double risk,
            boolean phHigh,
            boolean tdsHigh,
            String alert
    ) {

        // -----------------------------------------------------
        // SAFE RISK RANGE
        // -----------------------------------------------------

        risk = Math.max(
                0,
                Math.min(100, risk)
        );


        // -----------------------------------------------------
        // ML PREDICTION
        // -----------------------------------------------------

        tvPrediction.setText(
                mlPrediction.toUpperCase(
                        Locale.US
                )
        );

        tvMlPrediction.setText(
                "Random Forest: " +
                        mlPrediction
        );


        // -----------------------------------------------------
        // FINAL STATUS
        // -----------------------------------------------------

        tvFinalStatus.setText(
                prediction.toUpperCase(
                        Locale.US
                )
        );


        if (
                prediction.equalsIgnoreCase(
                        "Safe"
                )
        ) {

            tvFinalStatus.setTextColor(
                    Color.rgb(
                            22,
                            163,
                            74
                    )
            );

        } else {

            tvFinalStatus.setTextColor(
                    Color.rgb(
                            220,
                            38,
                            38
                    )
            );
        }


        // -----------------------------------------------------
        // RISK SCORE
        // -----------------------------------------------------

        tvRisk.setText(
                String.format(
                        Locale.US,
                        "ML Risk Score  •  %.1f / 100",
                        risk
                )
        );


        int riskColor;


        if (risk < 30) {

            tvRiskLevel.setText(
                    "LOW RISK"
            );

            riskColor =
                    Color.rgb(
                            22,
                            163,
                            74
                    );

        } else if (risk < 70) {

            tvRiskLevel.setText(
                    "MODERATE RISK"
            );

            riskColor =
                    Color.rgb(
                            217,
                            119,
                            6
                    );

        } else {

            tvRiskLevel.setText(
                    "HIGH RISK"
            );

            riskColor =
                    Color.rgb(
                            220,
                            38,
                            38
                    );
        }


        tvRiskLevel.setTextColor(
                riskColor
        );


        riskProgressBar.setProgress(
                (int) risk
        );

        riskProgressBar
                .getProgressDrawable()
                .setTint(
                        riskColor
                );


        // -----------------------------------------------------
        // SENSORS
        // -----------------------------------------------------

        tvPh.setText(
                String.format(
                        Locale.US,
                        "%.2f",
                        pH
                )
        );


        tvTds.setText(
                String.format(
                        Locale.US,
                        "%.1f",
                        tds
                )
        );


        tvTurbidity.setText(
                String.format(
                        Locale.US,
                        "%.1f",
                        turbidity
                )
        );


        tvRain.setText(
                rain
        );


        // -----------------------------------------------------
        // SENSOR STATUS
        // -----------------------------------------------------

        if (phHigh) {

            tvPhStatus.setText(
                    "⚠ Above 8.5"
            );

            tvPhStatus.setTextColor(
                    Color.rgb(
                            220,
                            38,
                            38
                    )
            );

        } else {

            tvPhStatus.setText(
                    "Within project threshold"
            );

            tvPhStatus.setTextColor(
                    Color.rgb(
                            22,
                            163,
                            74
                    )
            );
        }


        if (tdsHigh) {

            tvTdsStatus.setText(
                    "⚠ Above 500 ppm"
            );

            tvTdsStatus.setTextColor(
                    Color.rgb(
                            220,
                            38,
                            38
                    )
            );

        } else {

            tvTdsStatus.setText(
                    "Within project threshold"
            );

            tvTdsStatus.setTextColor(
                    Color.rgb(
                            22,
                            163,
                            74
                    )
            );
        }


        tvTurbidityStatus.setText(
                "Prototype value"
        );

        tvTurbidityStatus.setTextColor(
                Color.rgb(
                        217,
                        119,
                        6
                )
        );


        if (
                rain.equalsIgnoreCase(
                        "Rain Detected"
                )
        ) {

            tvRainStatus.setText(
                    "Rain detected"
            );

            tvRainStatus.setTextColor(
                    Color.rgb(
                            37,
                            99,
                            235
                    )
            );

        } else {

            tvRainStatus.setText(
                    "No rain"
            );

            tvRainStatus.setTextColor(
                    Color.rgb(
                            100,
                            116,
                            139
                    )
            );
        }


        // -----------------------------------------------------
        // ML INPUTS
        // -----------------------------------------------------

        tvInputSummary.setText(
                String.format(
                        Locale.US,
                        "pH          %.2f\n" +
                                "TDS         %.1f ppm\n" +
                                "Turbidity   %.1f NTU",
                        pH,
                        tds,
                        turbidity
                )
        );


        // -----------------------------------------------------
        // ML EXPLANATION
        // -----------------------------------------------------

        tvReason.setText(
                "The Random Forest model uses pH, TDS and turbidity as its input features. "
                        + "The ML prediction and ML Risk Score are shown separately from the project's threshold alert layer."
        );


        // -----------------------------------------------------
        // ALERT
        // -----------------------------------------------------

        tvAlert.setText(
                alert
        );


        if (
                phHigh &&
                        tdsHigh
        ) {

            tvAlert.setTextColor(
                    Color.rgb(
                            153,
                            27,
                            27
                    )
            );

            tvAlertDetail.setText(
                    "CRITICAL • Both project thresholds exceeded"
            );

            tvAlertDetail.setTextColor(
                    Color.rgb(
                            220,
                            38,
                            38
                    )
            );

        } else if (phHigh) {

            tvAlert.setTextColor(
                    Color.rgb(
                            220,
                            38,
                            38
                    )
            );

            tvAlertDetail.setText(
                    "pH threshold exceeded"
            );

            tvAlertDetail.setTextColor(
                    Color.rgb(
                            220,
                            38,
                            38
                    )
            );

        } else if (tdsHigh) {

            tvAlert.setTextColor(
                    Color.rgb(
                            220,
                            38,
                            38
                    )
            );

            tvAlertDetail.setText(
                    "TDS threshold exceeded"
            );

            tvAlertDetail.setTextColor(
                    Color.rgb(
                            220,
                            38,
                            38
                    )
            );

        } else {

            tvAlert.setTextColor(
                    Color.rgb(
                            22,
                            163,
                            74
                    )
            );

            tvAlertDetail.setText(
                    "No threshold alert"
            );

            tvAlertDetail.setTextColor(
                    Color.rgb(
                            22,
                            163,
                            74
                    )
            );
        }


        // -----------------------------------------------------
        // UPDATED TIME
        // -----------------------------------------------------

        tvUpdated.setText(
                "Last updated  •  "
                        + java.text.DateFormat
                        .getTimeInstance()
                        .format(
                                new java.util.Date()
                        )
        );


        // -----------------------------------------------------
        // HISTORY
        // -----------------------------------------------------

        long now =
                System.currentTimeMillis();


        if (
                now - lastHistoryTime
                        >= HISTORY_INTERVAL
        ) {

            lastHistoryTime = now;

            Reading reading =
                    new Reading(
                            pH,
                            tds,
                            turbidity,
                            risk,
                            prediction
                    );


            readings.add(
                    reading
            );


            if (
                    readings.size()
                            > MAX_HISTORY
            ) {

                readings.remove(
                        0
                );
            }


            updateHistory();

            updateGraphs();
        }
    }


    // =========================================================
    // HISTORY TEXT
    // =========================================================

    private void updateHistory() {

        if (readings.isEmpty()) {

            tvHistory.setText(
                    "No readings yet"
            );

            return;
        }


        StringBuilder builder =
                new StringBuilder();


        for (
                int i = readings.size() - 1;
                i >= 0;
                i--
        ) {

            Reading r =
                    readings.get(i);


            builder.append(
                    String.format(
                            Locale.US,
                            "pH %.2f  •  TDS %.1f  •  "
                                    + "Turb %.1f  •  Risk %.1f  •  %s",
                            r.ph,
                            r.tds,
                            r.turbidity,
                            r.risk,
                            r.prediction
                    )
            );


            builder.append(
                    "\n\n"
            );
        }


        tvHistory.setText(
                builder.toString()
        );
    }


    // =========================================================
    // GRAPH UPDATE
    // =========================================================

    private void updateGraphs() {

        int count =
                readings.size();


        if (count == 0) {
            return;
        }


        float[] ph =
                new float[count];

        float[] tds =
                new float[count];

        float[] turbidity =
                new float[count];

        float[] risk =
                new float[count];


        for (
                int i = 0;
                i < count;
                i++
        ) {

            Reading r =
                    readings.get(i);

            ph[i] =
                    (float) r.ph;

            tds[i] =
                    (float) r.tds;

            turbidity[i] =
                    (float) r.turbidity;

            risk[i] =
                    (float) r.risk;
        }


        phGraph.setData(
                ph,
                0,
                14
        );


        tdsGraph.setData(
                tds,
                0,
                1500
        );


        turbidityGraph.setData(
                turbidity,
                0,
                30
        );


        riskGraph.setData(
                risk,
                0,
                100
        );
    }


    // =========================================================
    // CONNECTION CHIP
    // =========================================================

    private void styleConnectionChip(
            boolean connected
    ) {

        GradientDrawable background =
                new GradientDrawable();

        background.setCornerRadius(
                100
        );


        if (connected) {

            background.setColor(
                    Color.rgb(
                            220,
                            252,
                            231
                    )
            );

            tvConnection.setTextColor(
                    Color.rgb(
                            22,
                            101,
                            52
                    )
            );

        } else {

            background.setColor(
                    Color.rgb(
                            254,
                            226,
                            226
                    )
            );

            tvConnection.setTextColor(
                    Color.rgb(
                            153,
                            27,
                            27
                    )
            );
        }


        connectionChip.setBackground(
                background
        );
    }


    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        running = false;

        handler.removeCallbacksAndMessages(
                null
        );

        super.onDestroy();
    }


    // =========================================================
    // READING MODEL
    // =========================================================

    static class Reading {

        double ph;
        double tds;
        double turbidity;
        double risk;
        String prediction;


        Reading(
                double ph,
                double tds,
                double turbidity,
                double risk,
                String prediction
        ) {

            this.ph = ph;
            this.tds = tds;
            this.turbidity = turbidity;
            this.risk = risk;
            this.prediction = prediction;
        }
    }


    // =========================================================
    // CUSTOM GRAPH VIEW
    // =========================================================

    public static class GraphView
            extends View {

        private final Paint gridPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint linePaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint pointPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint textPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private float[] values =
                new float[0];

        private float minValue = 0;

        private float maxValue = 100;


        public GraphView(
                android.content.Context context
        ) {

            super(context);

            init();
        }


        public GraphView(
                android.content.Context context,
                android.util.AttributeSet attrs
        ) {

            super(
                    context,
                    attrs
            );

            init();
        }


        private void init() {

            gridPaint.setColor(
                    Color.rgb(
                            226,
                            232,
                            240
                    )
            );

            gridPaint.setStrokeWidth(
                    1
            );


            linePaint.setColor(
                    Color.rgb(
                            18,
                            103,
                            130
                    )
            );

            linePaint.setStrokeWidth(
                    5
            );

            linePaint.setStyle(
                    Paint.Style.STROKE
            );

            linePaint.setStrokeCap(
                    Paint.Cap.ROUND
            );

            linePaint.setStrokeJoin(
                    Paint.Join.ROUND
            );


            pointPaint.setColor(
                    Color.rgb(
                            18,
                            103,
                            130
                    )
            );


            textPaint.setColor(
                    Color.rgb(
                            100,
                            116,
                            139
                    )
            );

            textPaint.setTextSize(
                    22
            );
        }


        public void setData(
                float[] values,
                float minValue,
                float maxValue
        ) {

            this.values =
                    values;

            this.minValue =
                    minValue;

            this.maxValue =
                    maxValue;

            invalidate();
        }


        @Override
        protected void onDraw(
                Canvas canvas
        ) {

            super.onDraw(
                    canvas
            );


            float width =
                    getWidth();

            float height =
                    getHeight();


            canvas.drawColor(
                    Color.rgb(
                            248,
                            250,
                            252
                    )
            );


            float left =
                    55;

            float right =
                    width - 18;

            float top =
                    18;

            float bottom =
                    height - 32;


            // -------------------------------------------------
            // GRID
            // -------------------------------------------------

            for (
                    int i = 0;
                    i <= 4;
                    i++
            ) {

                float y =
                        top
                                + (
                                (bottom - top)
                                        * i
                                        / 4f
                        );

                canvas.drawLine(
                        left,
                        y,
                        right,
                        y,
                        gridPaint
                );
            }


            // -------------------------------------------------
            // Y LABELS
            // -------------------------------------------------

            textPaint.setTextSize(
                    20
            );


            canvas.drawText(
                    formatAxis(
                            maxValue
                    ),
                    5,
                    top + 7,
                    textPaint
            );


            canvas.drawText(
                    formatAxis(
                            minValue
                    ),
                    5,
                    bottom,
                    textPaint
            );


            if (
                    values == null
                            || values.length == 0
            ) {

                textPaint.setTextSize(
                        22
                );

                canvas.drawText(
                        "Waiting for data...",
                        left,
                        height / 2,
                        textPaint
                );

                return;
            }


            // -------------------------------------------------
            // LINE
            // -------------------------------------------------

            Path path =
                    new Path();


            int count =
                    values.length;


            for (
                    int i = 0;
                    i < count;
                    i++
            ) {

                float x;

                if (count == 1) {

                    x =
                            (left + right)
                                    / 2f;

                } else {

                    x =
                            left
                                    + (
                                    (right - left)
                                            * i
                                            / (count - 1f)
                            );
                }


                float normalized =
                        (values[i] - minValue)
                                /
                                (
                                        maxValue
                                                - minValue
                                );


                normalized =
                        Math.max(
                                0,
                                Math.min(
                                        1,
                                        normalized
                                )
                        );


                float y =
                        bottom
                                - normalized
                                * (
                                bottom - top
                        );


                if (i == 0) {

                    path.moveTo(
                            x,
                            y
                    );

                } else {

                    path.lineTo(
                            x,
                            y
                    );
                }
            }


            canvas.drawPath(
                    path,
                    linePaint
            );


            // -------------------------------------------------
            // POINTS
            // -------------------------------------------------

            for (
                    int i = 0;
                    i < count;
                    i++
            ) {

                float x;

                if (count == 1) {

                    x =
                            (left + right)
                                    / 2f;

                } else {

                    x =
                            left
                                    + (
                                    (right - left)
                                            * i
                                            / (count - 1f)
                            );
                }


                float normalized =
                        (values[i] - minValue)
                                /
                                (
                                        maxValue
                                                - minValue
                                );


                normalized =
                        Math.max(
                                0,
                                Math.min(
                                        1,
                                        normalized
                                )
                        );


                float y =
                        bottom
                                - normalized
                                * (
                                bottom - top
                        );


                canvas.drawCircle(
                        x,
                        y,
                        5,
                        pointPaint
                );
            }
        }


        private String formatAxis(
                float value
        ) {

            if (
                    value >= 1000
            ) {

                return String.format(
                        Locale.US,
                        "%.0fk",
                        value / 1000
                );
            }


            if (
                    value == (int) value
            ) {

                return String.format(
                        Locale.US,
                        "%.0f",
                        value
                );
            }
11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111444444444444444444444444444444444444444444444444444ckkkkkkk011111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
                   

            return String.format(
                    Locale.US,
                    "%.1f",
                    value
            );
        }
    }
}
