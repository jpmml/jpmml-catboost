JPMML-CatBoost
==============

Java library and command-line application for converting [CatBoost](https://github.com/catboost/catboost) models to PMML.

# Prerequisites #

* CatBoost 0.15.2 or newer.
* Java 1.8 or newer.

# Installation #

Enter the project root directory and build using [Apache Maven](https://maven.apache.org/):
```
mvn clean install
```

The build produces an executable uber-JAR file `target/jpmml-catboost-executable-1.0-SNAPSHOT.jar`.

# Usage #

A typical workflow can be summarized as follows:

1. Use CatBoost to train a model.
2. Save the model to a CBM file in a local filesystem.
3. Use the JPMML-CatBoost command-line converter application to turn this CBM file to a PMML file.

### The CatBoost side of operations

Using the [`catboost`](https://github.com/catboost/catboost/tree/master/catboost/python-package) package to train a regression model for the example Boston housing dataset:

```python
from pandas import DataFrame, Series
from sklearn.datasets import load_boston

boston = load_boston()

X = DataFrame(boston.data, columns = boston.feature_names)
y = Series(boston.target)

from catboost import CatBoostRegressor

cb = CatBoostRegressor(iterations = 131)
cb.fit(X, y)

cb.save_model("catboost.cbm", format = "cbm")
```

### The JPMML-CatBoost side of operations

Converting the CBM file `catboost.cbm` to a PMML file `catboost.pmml`:
```
java -jar target/jpmml-catboost-executable-1.0-SNAPSHOT.jar --cbm-input catboost.cbm --pmml-output catboost.pmml
```

Getting help:
```
java -jar target/jpmml-catboost-executable-1.0-SNAPSHOT.jar --help
```

# License #

JPMML-CatBoost is dual-licensed under the [GNU Affero General Public License (AGPL) version 3.0](https://www.gnu.org/licenses/agpl-3.0.html), and a commercial license.

# Additional information #

JPMML-CatBoost is developed and maintained by Openscoring Ltd, Estonia.

Interested in using JPMML software in your application? Please contact [info@openscoring.io](mailto:info@openscoring.io)
