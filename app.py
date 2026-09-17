from flask import Flask, request, jsonify
import joblib
import pandas as pd
import json

# =========================================================
# LOAD MODEL
# =========================================================

model = joblib.load("water_quality_model.pkl")

app = Flask(__name__)

# =========================================================
# LOAD THRESHOLDS
# =========================================================

def load_thresholds():
    with open("thresholds.json", "r") as f:
        return json.load(f)

THRESHOLDS = load_thresholds()

# =========================================================
# RECENT HISTORY (for trend detection)
# =========================================================

history = []
MAX_HISTORY = 10


# =========================================================
# LATEST DATA
# =========================================================

latest_data = {
    "pH": 0,
    "TDS_ppm": 0,
    "Turbidity_NTU": 0,
    "Temperature_C": 0,
    "rain": "Unknown",

    "ml_prediction": "Waiting",
    "prediction": "Waiting",

    "risk_score": 0,

    "ph_threshold_exceeded": False,
    "tds_threshold_exceeded": False,
    "turbidity_threshold_exceeded": False,
    "temperature_threshold_exceeded": False,

    "alert": "Waiting for sensor data",

    "trends": {
        "pH": "stable",
        "TDS": "stable",
        "Turbidity": "stable",
        "Temperature": "stable"
    },

    "thresholds": THRESHOLDS
}


# =========================================================
# HOME
# =========================================================

@app.route("/")
def home():
    return app.send_static_file("dashboard.html")


# =========================================================
# RECEIVE ESP32 DATA + ML + THRESHOLDS
# =========================================================

@app.route("/predict", methods=["POST"])
def predict():
    global latest_data, history

    try:
        data = request.get_json()

        if not data:
            return jsonify({"error": "No JSON data received"}), 400

        # -------------------------------------------------
        # SENSOR DATA
        # -------------------------------------------------

        pH = float(data["pH"])
        TDS = float(data["TDS_ppm"])
        turbidity = float(data["Turbidity_NTU"])
        temperature = float(data.get("Temperature_C", 25.0))
        rain = str(data.get("rain", "Unknown"))

        # -------------------------------------------------
        # ML INPUT
        # -------------------------------------------------

        input_df = pd.DataFrame([{
            "pH": pH,
            "TDS_ppm": TDS,
            "Turbidity_NTU": turbidity
        }])

        # =================================================
        # RANDOM FOREST
        # =================================================

        ml_prediction = str(model.predict(input_df)[0])

        # -------------------------------------------------
        # ML RISK SCORE
        # -------------------------------------------------

        probabilities = model.predict_proba(input_df)[0]
        classes = list(model.classes_)
        unsafe_index = classes.index("Unsafe")
        unsafe_probability = float(probabilities[unsafe_index])
        risk_score = round(unsafe_probability * 100, 2)

        # =================================================
        # THRESHOLD CHECK
        # =================================================

        ph_high = pH > THRESHOLDS["pH"]
        tds_high = TDS > THRESHOLDS["TDS"]
        turb_high = turbidity > THRESHOLDS["Turbidity"]
        temp_high = temperature > THRESHOLDS["Temperature"]

        # =================================================
        # FINAL STATUS
        # =================================================

        if ph_high or tds_high or turb_high or temp_high:
            prediction = "Unsafe"
        else:
            prediction = "Safe"

        # =================================================
        # BUILD ALERT LIST
        # =================================================

        alerts = []
        if ph_high:
            alerts.append(f"pH exceeds {THRESHOLDS['pH']}")
        if tds_high:
            alerts.append(f"TDS exceeds {THRESHOLDS['TDS']} ppm")
        if turb_high:
            alerts.append(f"Turbidity exceeds {THRESHOLDS['Turbidity']} NTU")
        if temp_high:
            alerts.append(f"Temperature exceeds {THRESHOLDS['Temperature']}°C")

        if len(alerts) > 1:
            alert = "CRITICAL: " + " & ".join(alerts)
        elif len(alerts) == 1:
            alert = "ALERT: " + alerts[0]
        else:
            alert = "No active water-quality alert"

        # =================================================
        # TREND DETECTION
        # =================================================

        history.append({
            "pH": pH,
            "TDS": TDS,
            "Turbidity": turbidity,
            "Temperature": temperature
        })
        if len(history) > MAX_HISTORY:
            history.pop(0)

        def detect_trend(values):
            if len(values) < 3:
                return "stable"
            recent = values[-3:]
            avg_change = (recent[-1] - recent[0]) / 2
            if avg_change > 0.3:
                return "rising"
            elif avg_change < -0.3:
                return "falling"
            return "stable"

        trends = {
            "pH": detect_trend([h["pH"] for h in history]),
            "TDS": detect_trend([h["TDS"] for h in history]),
            "Turbidity": detect_trend([h["Turbidity"] for h in history]),
            "Temperature": detect_trend([h["Temperature"] for h in history])
        }

        # =================================================
        # STORE RESULT
        # =================================================

        latest_data = {
            "pH": pH,
            "TDS_ppm": TDS,
            "Turbidity_NTU": turbidity,
            "Temperature_C": temperature,
            "rain": rain,
            "ml_prediction": ml_prediction,
            "prediction": prediction,
            "risk_score": risk_score,
            "ph_threshold_exceeded": ph_high,
            "tds_threshold_exceeded": tds_high,
            "turbidity_threshold_exceeded": turb_high,
            "temperature_threshold_exceeded": temp_high,
            "alert": alert,
            "trends": trends,
            "thresholds": THRESHOLDS
        }

        # =================================================
        # TERMINAL OUTPUT
        # =================================================

        print()
        print("========================================")
        print(" WATER QUALITY MONITORING")
        print("========================================")
        print(f"pH              : {pH:.2f}")
        print(f"TDS             : {TDS:.1f} ppm")
        print(f"Turbidity       : {turbidity:.1f} NTU")
        print(f"Temperature     : {temperature:.1f}°C")
        print(f"Rain            : {rain}")
        print("----------------------------------------")
        print(f"ML Prediction   : {ml_prediction}")
        print(f"ML Risk Score   : {risk_score:.2f}")
        print("----------------------------------------")
        print(f"pH > {THRESHOLDS['pH']}        : {ph_high}")
        print(f"TDS > {THRESHOLDS['TDS']}   : {tds_high}")
        print(f"Turb > {THRESHOLDS['Turbidity']}    : {turb_high}")
        print(f"Temp > {THRESHOLDS['Temperature']}   : {temp_high}")
        print("----------------------------------------")
        print(f"Final Status    : {prediction}")
        print(f"Alert           : {alert}")
        print("========================================")

        return jsonify(latest_data)

    except Exception as e:
        print("Backend Error:", str(e))
        return jsonify({"error": str(e)}), 400


# =========================================================
# LATEST
# =========================================================

@app.route("/latest")
def latest():
    return jsonify(latest_data)


# =========================================================
# HISTORY
# =========================================================

@app.route("/history")
def get_history():
    return jsonify(history)


# =========================================================
# THRESHOLDS
# =========================================================

@app.route("/thresholds", methods=["GET"])
def get_thresholds():
    return jsonify(THRESHOLDS)


@app.route("/thresholds", methods=["POST"])
def update_thresholds():
    global THRESHOLDS
    try:
        new_thresholds = request.get_json()
        THRESHOLDS.update(new_thresholds)
        with open("thresholds.json", "w") as f:
            json.dump(THRESHOLDS, f, indent=4)
        return jsonify({"status": "ok", "thresholds": THRESHOLDS})
    except Exception as e:
        return jsonify({"error": str(e)}), 400


# =========================================================
# START SERVER
# =========================================================

if __name__ == "__main__":
    print("========================================")
    print(" Water Quality ML Backend")
    print("========================================")
    print(f"pH threshold      : {THRESHOLDS['pH']}")
    print(f"TDS threshold     : {THRESHOLDS['TDS']} ppm")
    print(f"Turbidity threshold: {THRESHOLDS['Turbidity']} NTU")
    print(f"Temperature threshold: {THRESHOLDS['Temperature']}°C")
    print("Server            : http://0.0.0.0:5003")
    print("========================================")

    app.run(host="0.0.0.0", port=5003, debug=True)
