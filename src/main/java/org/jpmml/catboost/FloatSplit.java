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

class FloatSplit extends Split {

	private Float border = null;


	FloatSplit(FieldName name, Float border){
		super(name);

		setBorder(border);
	}

	@Override
	public String toString(){
		return getName() + "@" + getBorder();
	}

	public Float getBorder(){
		return this.border;
	}

	private void setBorder(Float border){
		this.border = border;
	}
}