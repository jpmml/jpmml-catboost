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

import org.dmg.pmml.FieldName;

abstract
class Split {

	private FieldName name = null;


	Split(FieldName name){
		setName(name);
	}

	public FieldName getName(){
		return this.name;
	}

	private void setName(FieldName name){
		this.name = name;
	}
}