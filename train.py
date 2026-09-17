import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix
import joblib


def train_model():
    df = pd.read_csv("water_quality_dataset1.csv")

    print("Dataset shape:", df.shape)
    print("\nLabel distribution:")
    print(df["Label"].value_counts())

    X = df[["pH", "TDS_ppm", "Turbidity_NTU"]]
    y = df["Label"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    model = RandomForestClassifier(
        n_estimators=100,
        class_weight="balanced",
        random_state=42,
        n_jobs=-1
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)

    print("\n=== Classification Report ===")
    print(classification_report(y_test, y_pred))

    print("=== Confusion Matrix ===")
    print(confusion_matrix(y_test, y_pred))

    joblib.dump(model, "water_quality_model.pkl")
    print("\nModel saved to water_quality_model.pkl")


if __name__ == "__main__":
    train_model()
