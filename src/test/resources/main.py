from catboost import CatBoostClassifier, CatBoostError, CatBoostRegressor
from pandas import DataFrame

import pandas
import shutil

def load_csv(name, categorical_columns = []):
	df = pandas.read_csv("csv/" + name)
	for categorical_column in categorical_columns:
		df[categorical_column] = df[categorical_column].astype("category")
	return df

def store_csv(df, name):
	df.to_csv("csv/" + name, index = False)

def store_cbm(catboost, name, pool = None, debug = False):
	catboost.save_model("cbm/" + name, format = "cbm")
	if debug:
		try:
			catboost.save_model("cbm/" + name + ".json", format = "json", pool = pool)
		except CatBoostError:
			pass
		try:
			catboost.save_model("cbm/" + name + ".pmml", format = "pmml", pool = pool)
		except CatBoostError as cbe:
			print(cbe)
		try:
			catboost.save_model("cbm/" + name + ".cpp", format = "cpp", pool = pool)
			catboost.save_model("cbm/" + name + ".py", format = "python", pool = pool)
		except CatBoostError:
			pass

def build_audit():
	df = load_csv("Audit.csv", ["Employment", "Education", "Marital", "Occupation", "Gender"])
	X = df[["Age", "Employment", "Education", "Marital", "Occupation", "Income", "Gender", "Hours"]]
	y = df["Adjusted"]

	catboost = CatBoostClassifier(iterations = 31, depth = 5, random_state = 42)
	catboost.fit(X, y, cat_features = [1, 2, 3, 4, 6])

	store_cbm(catboost, "ClassificationAudit.cbm", pool = X)

	adjusted = DataFrame(catboost.predict(X), columns = ["_target"])
	adjusted_proba = DataFrame(catboost.predict_proba(X), columns = ["probability(0)", "probability(1)"])
	store_csv(pandas.concat((adjusted, adjusted_proba), axis = 1), "ClassificationAudit.csv")

build_audit()

def build_versicolor():
	df = load_csv("Versicolor.csv")
	X = df[df.columns.difference(["Species"])]
	y = df["Species"]

	catboost = CatBoostClassifier(iterations = 5, depth = 3, random_state = 42)
	catboost.fit(X, y)

	store_cbm(catboost, "ClassificationVersicolor.cbm")

	species = DataFrame(catboost.predict(X), columns = ["_target"])
	species_proba = DataFrame(catboost.predict_proba(X), columns = ["probability(0)", "probability(1)"])
	store_csv(pandas.concat((species, species_proba), axis = 1), "ClassificationVersicolor.csv")

build_versicolor()

def build_iris():
	df = load_csv("Iris.csv")
	X = df[df.columns.difference(["Species"])]
	y = df["Species"]

	y = y.replace("setosa", "0").replace("versicolor", "1").replace("virginica", "2")
	y = y.astype(int)

	catboost = CatBoostClassifier(iterations = 5, loss_function = "MultiClass", random_state = 42)
	catboost.fit(X, y)

	store_cbm(catboost, "ClassificationIris.cbm")

	species = DataFrame(catboost.predict(X), columns = ["_target"])
	species_proba = DataFrame(catboost.predict_proba(X), columns = ["probability(0)", "probability(1)", "probability(2)"])
	store_csv(pandas.concat((species, species_proba), axis = 1), "ClassificationIris.csv")

build_iris()

def build_auto():
	df = load_csv("Auto.csv", ["cylinders", "model_year", "origin"])
	X = df[["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"]]
	y = df["mpg"]

	catboost = CatBoostRegressor(iterations = 17, depth = 5, random_state = 42)
	#catboost.fit(X, y, cat_features = [0, 5, 6])
	catboost.fit(X, y)

	#store_cbm(catboost, "RegressionAuto.cbm", pool = X)
	store_cbm(catboost, "RegressionAuto.cbm")

	mpg = DataFrame(catboost.predict(X), columns = ["_target"])
	store_csv(mpg, "RegressionAuto.csv")

build_auto()

shutil.rmtree("catboost_info")
