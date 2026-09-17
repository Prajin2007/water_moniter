from flask import Flask, request, jsonify
import joblib
import pandas as pd

# =========================================================
# LOAD MODEL
# =========================================================

model = joblib.load("water_quality_model.pkl")

app = Flask(__name__)


# =========================================================
# PROJECT THRESHOLDS
# =========================================================

PH_THRESHOLD = 8.5
TDS_THRESHOLD = 500.0


# =========================================================
# LATEST DATA
# =========================================================

latest_data = {
    "pH": 0,
    "TDS_ppm": 0,
    "Turbidity_NTU": 0,
    "rain": "Unknown",

    "ml_prediction": "Waiting",
    "prediction": "Waiting",

    "risk_score": 0,

    "ph_threshold_exceeded": False,
    "tds_threshold_exceeded": False,

    "alert": "Waiting for sensor data"
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

    global latest_data

    try:

        data = request.get_json()

        if not data:
            return jsonify({
                "error": "No JSON data received"
            }), 400


        # -------------------------------------------------
        # SENSOR DATA
        # -------------------------------------------------

        pH = float(data["pH"])
        TDS = float(data["TDS_ppm"])
        turbidity = float(data["Turbidity_NTU"])

        rain = str(
            data.get("rain", "Unknown")
        )


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

        ml_prediction = str(
            model.predict(input_df)[0]
        )


        # -------------------------------------------------
        # ML RISK SCORE
        # Unsafe probability × 100
        # -------------------------------------------------

        probabilities = model.predict_proba(
            input_df
        )[0]

        classes = list(model.classes_)

        unsafe_index = classes.index("Unsafe")

        unsafe_probability = float(
            probabilities[unsafe_index]
        )

        risk_score = round(
            unsafe_probability * 100,
            2
        )


        # =================================================
        # PROJECT THRESHOLD CHECK
        # =================================================

        ph_high = pH > PH_THRESHOLD

        tds_high = TDS > TDS_THRESHOLD


        # =================================================
        # FINAL STATUS
        # =================================================

        if ph_high or tds_high:

            prediction = "Unsafe"

        else:

            prediction = "Safe"


        # =================================================
        # ALERT
        # =================================================

        if ph_high and tds_high:

            alert = (
                "CRITICAL ALERT: pH and TDS "
                "exceed project thresholds"
            )

        elif ph_high:

            alert = (
                "ALERT: pH exceeds 8.5"
            )

        elif tds_high:

            alert = (
                "ALERT: TDS exceeds 500 ppm"
            )

        else:

            alert = (
                "No active water-quality alert"
            )


        # =================================================
        # STORE RESULT
        # =================================================

        latest_data = {

            "pH": pH,

            "TDS_ppm": TDS,

            "Turbidity_NTU": turbidity,

            "rain": rain,

            "ml_prediction": ml_prediction,

            "prediction": prediction,

            "risk_score": risk_score,

            "ph_threshold_exceeded": ph_high,

            "tds_threshold_exceeded": tds_high,

            "alert": alert
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
        print(f"Rain            : {rain}")

        print("----------------------------------------")

        print(f"ML Prediction   : {ml_prediction}")
        print(f"ML Risk Score   : {risk_score:.2f}")

        print("----------------------------------------")

        print(f"pH > 8.5        : {ph_high}")
        print(f"TDS > 500 ppm   : {tds_high}")

        print("----------------------------------------")

        print(f"Final Status    : {prediction}")
        print(f"Alert           : {alert}")

        print("========================================")


        return jsonify(latest_data)


    except Exception as e:

        print("Backend Error:", str(e))

        return jsonify({
            "error": str(e)
        }), 400


# =========================================================
# LATEST
# =========================================================

@app.route("/latest")
def latest():

    return jsonify(latest_data)


# =========================================================
# START SERVER
# =========================================================

if __name__ == "__main__":

    print("========================================")
    print(" Water Quality ML Backend")
    print("========================================")

    print(
        f"pH threshold  : {PH_THRESHOLD}"
    )

    print(
        f"TDS threshold : {TDS_THRESHOLD}"
    )

    print(
        "Server        : http://0.0.0.0:5003"
    )

    print("========================================")

    app.run(
        host="0.0.0.0",
        port=5003,
        debug=True
    )
