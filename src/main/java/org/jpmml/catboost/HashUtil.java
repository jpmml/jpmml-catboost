/*
 * Copyright (c) 2019 Villu Ruusmann
 *
 * This file is part of JPMML-CatBoost
 *
 * JPMML-CatBoost is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-CatBoost is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-CatBoost.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.catboost;

import java.lang.reflect.Method;

public class HashUtil {

	private HashUtil(){
	}

	static
	public int hashCatFeature(String value) throws ReflectiveOperationException {
		Class<?> nativeLibClass = Class.forName("ai.catboost.NativeLib");

		Method handleMethod = nativeLibClass.getDeclaredMethod("handle");
		handleMethod.setAccessible(true);

		Object catboostJNI = handleMethod.invoke(null);

		Method catBoostHashCatFeatureMethod = catboostJNI.getClass().getDeclaredMethod("catBoostHashCatFeature", String.class, int[].class);
		catBoostHashCatFeatureMethod.setAccessible(true);

		int[] hash = new int[1];

		catBoostHashCatFeatureMethod.invoke(catboostJNI, value, hash);

		return hash[0];
	}
}