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

import java.io.InputStream;
import java.util.function.Predicate;

import NCatBoostFbs.TModelCore;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.ArchiveBatch;
import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.IntegrationTestBatch;
import org.jpmml.evaluator.RealNumberEquivalence;

public class CatBoostTest extends IntegrationTest {

	public CatBoostTest(){
		super(new RealNumberEquivalence(2));
	}

	@Override
	protected ArchiveBatch createBatch(String name, String dataset, Predicate<FieldName> predicate){
		ArchiveBatch result = new IntegrationTestBatch(name, dataset, predicate){

			@Override
			public IntegrationTest getIntegrationTest(){
				return CatBoostTest.this;
			}

			@Override
			public PMML getPMML() throws Exception {
				TModelCore modelCore;

				try(InputStream is = open("/cbm/" + getName() + getDataset() + ".cbm")){
					modelCore = CatBoostUtil.readTModelCore(is);
				}

				PMML pmml = CatBoostUtil.encodePMML(modelCore);

				ensureValidity(pmml);

				return pmml;
			}
		};

		return result;
	}
}