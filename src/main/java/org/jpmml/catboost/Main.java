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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import NCatBoostFbs.TModelCore;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.dmg.pmml.PMML;
import org.jpmml.model.MetroJAXBUtil;

public class Main {

	@Parameter (
		names = {"--help"},
		description = "Show the list of configuration options and exit",
		help = true
	)
	private boolean help = false;

	@Parameter (
		names = {"--cbm-input"},
		description = "CBM input file",
		required = true
	)
	private File input = null;

	@Parameter (
		names = {"--pmml-output"},
		description = "PMML output file",
		required = true
	)
	private File output = null;

	static
	public void main(String... args) throws Exception {
		Main main = new Main();

		JCommander commander = new JCommander(main);
		commander.setProgramName(Main.class.getName());

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			StringBuilder sb = new StringBuilder();

			sb.append(pe.toString());
			sb.append("\n");

			commander.usage(sb);

			System.err.println(sb.toString());

			System.exit(-1);
		}

		if(main.help){
			StringBuilder sb = new StringBuilder();

			commander.usage(sb);

			System.out.println(sb.toString());

			System.exit(0);
		}

		main.run();
	}

	private void run() throws Exception {
		TModelCore modelCore;

		try(InputStream is = new FileInputStream(this.input)){
			modelCore = CatBoostUtil.readTModelCore(is);
		}

		PMML pmml = CatBoostUtil.encodePMML(modelCore);

		try(OutputStream os = new FileOutputStream(this.output)){
			MetroJAXBUtil.marshalPMML(pmml, os);
		}
	}
}