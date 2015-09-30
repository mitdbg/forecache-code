package backend.prediction.directional;

import java.util.List;
import java.util.Random;

import backend.prediction.BasicModel;
import backend.prediction.DirectionPrediction;
import abstraction.prediction.DefinedTileView;
import abstraction.prediction.SessionMetadata;
import abstraction.util.Direction;
import abstraction.util.NewTileKey;

public class RandomDirectionalModel extends BasicModel {
	private Random generator;
	//public static final int seed = 7;
	public static final int seed = 425752111;

	public RandomDirectionalModel(int len) {
		super(len);
		this.generator = new Random(seed); // use seed for consistency
		this.useDistanceCorrection = false;
	}
	
	@Override
	public List<DirectionPrediction> predictOrder(SessionMetadata md, DefinedTileView dtv, List<NewTileKey> htrace) {
		return super.predictOrder(md,dtv,htrace,true);
	}
	
	@Override
	public double computeConfidence(SessionMetadata md, DefinedTileView dtv,
			Direction d, List<NewTileKey> trace) {
		return generator.nextDouble();
	}
}

