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

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.google.common.io.ByteStreams;
import com.google.common.io.LittleEndianDataInputStream;

public class CatBoostDataInput implements Closeable {

	private InputStream is = null;


	public CatBoostDataInput(InputStream is) throws IOException {
		byte[] header = new byte[4];

		ByteStreams.readFully(is, header);

		String headerString = new String(header, "US-ASCII");
		if(!("CBM1").equals(headerString)){
			throw new IllegalArgumentException(headerString);
		}

		this.is = new LittleEndianDataInputStream(is);
	}

	@Override
	public void close() throws IOException {
		this.is.close();
	}

	public int readSize() throws IOException {
		DataInput input = asDataInput();

		int oldVerSize = input.readInt();
		if(oldVerSize != 0xffffffff){
			return oldVerSize;
		}

		long newSize = input.readLong();

		return (int)newSize;
	}

	public ByteBuffer readByteBuffer() throws IOException {
		int size = readSize();

		byte[] bytes = new byte[size];

		ByteStreams.readFully(this.is, bytes);

		return ByteBuffer.wrap(bytes);
	}

	private DataInput asDataInput(){
		return (DataInput)this.is;
	}
}