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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import NCatBoostFbs.TFloatFeature;
import NCatBoostFbs.TKeyValue;
import NCatBoostFbs.TModelCore;
import NCatBoostFbs.TObliviousTrees;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.tree.BranchNode;
import org.dmg.pmml.tree.CountingLeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Label;
import org.jpmml.converter.LabelUtil;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class CatBoostUtil {

	private CatBoostUtil(){
	}

	static
	public TModelCore readTModelCore(InputStream is) throws IOException {
		CatBoostDataInput input = new CatBoostDataInput(is);

		try {
			ByteBuffer byteBuffer = input.readByteBuffer();

			return TModelCore.getRootAsTModelCore(byteBuffer);
		} finally {
			input.close();
		}
	}

	static
	public PMML encodePMML(TModelCore modelCore) throws Exception {
		CatBoostEncoder encoder = new CatBoostEncoder();

		TObliviousTrees obliviousTrees = modelCore.ObliviousTrees();

		TKeyValue params = modelCore.InfoMapByKey("params");

		Map<String, ?> paramsMap = parseMap(params.Value());

		Map<String, ?> lossFunction = (Map)paramsMap.get("loss_function");

		String lossFunctionType = (String)lossFunction.get("type");

		List<Split> splits = encodeFeatures(obliviousTrees, encoder);

		MiningModel miningModel = encodeModel(obliviousTrees, lossFunctionType, splits, encoder);

		return encoder.encodePMML(miningModel);
	}

	static
	private List<Split> encodeFeatures(TObliviousTrees obliviousTrees, CatBoostEncoder encoder){
		int catFeaturesLength = obliviousTrees.CatFeaturesLength();
		int floatFeaturesLength = obliviousTrees.FloatFeaturesLength();
		int oneHotFeaturesLength = obliviousTrees.OneHotFeaturesLength();
		int ctrFeaturesLength = obliviousTrees.CtrFeaturesLength();

		if(catFeaturesLength != 0 || oneHotFeaturesLength != 0 || ctrFeaturesLength != 0){
			throw new IllegalArgumentException();
		}

		List<List<Split>> featureSplits = new ArrayList<>();

		Split nullSplit = new Split(null){

			@Override
			public String toString(){
				return null;
			}
		};

		int featuresLength = (catFeaturesLength + floatFeaturesLength);
		for(int i = 0; i < featuresLength; i++){
			featureSplits.add(Collections.singletonList(nullSplit));
		}

		for(int i = 0; i < floatFeaturesLength; i++){
			TFloatFeature floatFeature = obliviousTrees.FloatFeatures(i);

			FieldName name = FieldName.create(floatFeature.FeatureId());

			DataField dataField = encoder.createDataField(name, OpType.CONTINUOUS, DataType.FLOAT);

			List<Split> floatSplits = new ArrayList<>();

			int borderLength = floatFeature.BordersLength();
			for(int j = 0; j < borderLength; j++){
				float border = floatFeature.Borders(j);

				floatSplits.add(new FloatSplit(name, border));
			}

			featureSplits.set(floatFeature.Index(), floatSplits);
		}

		List<Split> splits = featureSplits.stream()
			.flatMap(indexSplits -> indexSplits.stream())
			.collect(Collectors.toList());

		return splits;
	}

	static
	private MiningModel encodeModel(TObliviousTrees obliviousTrees, String lossFunctionType, List<Split> splits, CatBoostEncoder encoder){
		MiningFunction miningFunction;

		switch(lossFunctionType){
			case "Logloss":
			case "MultiClass":
				miningFunction = MiningFunction.CLASSIFICATION;
				break;
			case "RMSE":
				miningFunction = MiningFunction.REGRESSION;
				break;
			default:
				throw new IllegalArgumentException(lossFunctionType);
		}

		int numDimensions = obliviousTrees.ApproxDimension();

		Label label;

		switch(miningFunction){
			case CLASSIFICATION:
				{
					List<Integer> categories;

					if(numDimensions == 1){
						categories = Arrays.asList(0, 1);
					} else

					if(numDimensions > 1){
						categories = LabelUtil.createTargetCategories(numDimensions);
					} else

					{
						throw new IllegalArgumentException();
					}

					DataField dataField = encoder.createDataField(FieldName.create("_target"), OpType.CATEGORICAL, DataType.INTEGER, categories);

					label = new CategoricalLabel(dataField);
				}
				break;
			case REGRESSION:
				{
					DataField dataField = encoder.createDataField(FieldName.create("_target"), OpType.CONTINUOUS, DataType.DOUBLE);

					label = new ContinuousLabel(dataField);
				}
				break;
			default:
				throw new IllegalArgumentException();
		}

		int numTrees = obliviousTrees.TreeSizesLength();

		int[] treeSplits = new int[obliviousTrees.TreeSplitsLength()];

		IntBuffer treeSplitsBuf = (obliviousTrees.TreeSplitsAsByteBuffer()).asIntBuffer();

		treeSplitsBuf.get(treeSplits);

		DoubleBuffer leafValuesBuf = (obliviousTrees.LeafValuesAsByteBuffer()).asDoubleBuffer();
		DoubleBuffer leafWeightsBuf = (obliviousTrees.LeafWeightsAsByteBuffer()).asDoubleBuffer();

		double[] leafValues = new double[obliviousTrees.LeafValuesLength()];
		double[] leafWeights = new double[obliviousTrees.LeafWeightsLength()];

		leafValuesBuf.get(leafValues);
		leafWeightsBuf.get(leafWeights);

		Label segmentLabel;

		switch(miningFunction){
			case CLASSIFICATION:
				segmentLabel = label.toAnonymousLabel();
				break;
			case REGRESSION:
				segmentLabel = label;
				break;
			default:
				throw new IllegalArgumentException();
		}

		List<MiningModel> miningModels = new ArrayList<>();

		for(int dim = 0; dim < numDimensions; dim++){
			List<TreeModel> treeModels = new ArrayList<>();

			int treeSplitsPtr = 0;

			int treeLeafValuesPtr = 0;
			int treeLeafWeightsPtr = 0;

			for(int tree = 0; tree < numTrees; tree++){
				int numLevels = obliviousTrees.TreeSizes(tree);
				int numNodes = (1 << numLevels);

				int[] treeTreeSplits = new int[numLevels];

				System.arraycopy(treeSplits, treeSplitsPtr, treeTreeSplits, 0, numLevels);

				double[] treeLeafValues = new double[numNodes];
				double[] treeLeafWeights = new double[numNodes];

				for(int i = 0; i < numNodes; i++){
					treeLeafValues[i] = leafValues[treeLeafValuesPtr + (i * numDimensions) + dim];
				}

				System.arraycopy(leafWeights, treeLeafWeightsPtr, treeLeafWeights, 0, numNodes);

				Node root = encodeNode(new True(), 0, 0, numLevels, treeTreeSplits, treeLeafValues, treeLeafWeights, splits);

				TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, new MiningSchema(), root);

				treeModels.add(treeModel);

				treeSplitsPtr += numLevels;

				treeLeafValuesPtr += (numDimensions * numNodes);
				treeLeafWeightsPtr += numNodes;
			}

			MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(segmentLabel))
				.setSegmentation(MiningModelUtil.createSegmentation(Segmentation.MultipleModelMethod.SUM, treeModels));

			switch(miningFunction){
				case CLASSIFICATION:
					{
						FieldName name;

						if(numDimensions == 1){
							name = FieldName.create("cbValue");
						} else

						{
							name = FieldName.create("cbValue(" + dim + ")");
						}

						miningModel.setOutput(ModelUtil.createPredictedOutput(name, OpType.CONTINUOUS, DataType.DOUBLE));
					}
					break;
				default:
					break;
			}

			miningModels.add(miningModel);
		}

		MiningModel miningModel;

		switch(miningFunction){
			case CLASSIFICATION:
				{
					Schema schema = new Schema(label, Collections.emptyList());

					if(numDimensions == 1){
						miningModel = MiningModelUtil.createBinaryLogisticClassification(Iterables.getOnlyElement(miningModels), 1d, 0d, RegressionModel.NormalizationMethod.LOGIT, true, schema);
					} else

					if(numDimensions > 1){
						miningModel = MiningModelUtil.createClassification(miningModels, RegressionModel.NormalizationMethod.SOFTMAX, true, schema);
					} else

					{
						throw new IllegalArgumentException();
					}
				}
				break;
			case REGRESSION:
				{
					miningModel = Iterables.getOnlyElement(miningModels);
				}
				break;
			default:
				throw new IllegalArgumentException();
		}

		return miningModel;
	}

	static
	private Node encodeNode(Predicate predicate, int index, int depth, int numLevels, int[] treeSplits, double[] leafValues, double[] leafWeights, List<Split> splits){

		if(depth >= numLevels){
			double leafValue = leafValues[index];
			double leafWeight = leafWeights[index];

			Node result = new CountingLeafNode(leafValue, predicate)
				.setRecordCount(leafWeight);

			return result;
		}

		int treeSplit = treeSplits[depth];

		Split split = splits.get(treeSplit);

		Predicate leftPredicate;
		Predicate rightPredicate;

		if(split instanceof FloatSplit){
			FloatSplit floatSplit = (FloatSplit)splits.get(treeSplit);

			FieldName name = floatSplit.getName();
			Number value = floatSplit.getBorder();

			leftPredicate = new SimplePredicate(name, SimplePredicate.Operator.LESS_OR_EQUAL, value);
			rightPredicate = new SimplePredicate(name, SimplePredicate.Operator.GREATER_THAN, value);
		} else

		{
			throw new IllegalArgumentException();
		}

		int depthMask = (1 << depth);

		Node leftChild = encodeNode(leftPredicate, index, depth + 1, numLevels, treeSplits, leafValues, leafWeights, splits);
		Node rightChild = encodeNode(rightPredicate, index | depthMask, depth + 1, numLevels, treeSplits, leafValues, leafWeights, splits);

		Node result = new BranchNode(null, predicate)
			.addNodes(leftChild, rightChild);

		return result;
	}

	static
	private Map<String, ?> parseMap(String jsonString) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();

		TypeReference<HashMap<String, Object>> mapType = new TypeReference<HashMap<String, Object>>(){
		};

		Map<String, ?> result = (Map)objectMapper.readValue(jsonString, mapType);

		return result;
	}
}