import numpy as np
import pandas as pd
from matplotlib import pyplot as plt
from keras.models import Sequential
from keras.layers import Dense
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import cross_val_score
import pickle
from os.path import exists
from flask import Flask, request, json, jsonify

def swap_columns(df, col1, col2):
    col_list = list(df.columns)
    x, y = col_list.index(col1), col_list.index(col2)
    col_list[y], col_list[x] = col_list[x], col_list[y]
    df = df[col_list]
    return df

def load_data(file_path):
    df = pd.read_csv(file_path, header=9)
    df.columns = ['x', 'temp', 'res', 'h', 'wspeed', 'x2']
    df = df.drop(columns=['x', 'x2'])
    df = swap_columns(df, 'res', 'wspeed')
    df['res'] = pd.cut(df['res'],bins=[-1, 0, 100], labels=[0, 1]).astype(int)
    return df

def init():
    global df_train, df_test, X_train, y_train, X_test, y_test
    df_train = load_data('train.csv')
    df_test = load_data('test.csv')
    X_train = df_train.iloc[:, :-1]
    y_train = df_train.iloc[:, -1]
    X_test = df_test.iloc[:, :-1]
    y_test = df_test.iloc[:, -1]

def evaluate(prd):
    global y_test
    plt.scatter(y_test, prd)
    plt.xlabel("Actual")
    plt.ylabel("Predicted")
    plt.title("Actual vs Predicted")
    print("Confusion matrix:")
    print(pd.crosstab(prd, y_test))

def train_model_dl():
    global X_train, y_train, X_test
    from sklearn.preprocessing import StandardScaler 
    sc = StandardScaler()
    X_train = sc.fit_transform(X_train)
    X_test = sc.transform(X_test)
    model = Sequential()
    model.add(Dense(90, input_dim=3, activation='relu'))
    model.add(Dense(60, activation='relu'))
    model.add(Dense(30, activation='relu'))
    model.add(Dense(1, activation='sigmoid'))

    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])
    model.fit(X_train, y_train, epochs=50, batch_size=50)
    prd = (model.predict(X_test) > 0.5).astype(int).flatten()
    evaluate(prd)
    return model

def train_model_ml():
    global X_train, y_train, X_test
    log_regress = LogisticRegression()
    log_regress.fit(X_train, y_train)
    prd = log_regress.predict(X_test)
    evaluate( prd)
    return log_regress

app = Flask(__name__)

def main():
    if not exists('model.sav'):
        init()
        model = train_model_ml()
        pickle.dump(model, open('model.sav', 'wb'))
    global loaded_model, app
    loaded_model = pickle.load(open('model.sav', 'rb'))
    app.run(host='0.0.0.0', port=5000)

@app.route('/iot/weather/forecast', methods=['POST'])
def predict():
    global loaded_model
    print(request.data)
    features = request.json
    features_list = [features["temperature"],
      features["humidity"],
      features["wind_speed"]]
    prediction = loaded_model.predict([features_list])
    print(prediction)
    response = {}
    response['prediction'] = int(prediction[0])
    return jsonify(response)

if __name__ == '__main__':
    main()