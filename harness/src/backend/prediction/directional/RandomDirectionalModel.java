package backend.prediction.directional;

import java.util.List;
import java.util.Random;

import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryTileBuffer;
import backend.prediction.BasicModel;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;

import utils.UserRequest;

public class RandomDirectionalModel extends BasicModel {
	private Random generator;
	//public static final int seed = 7;
	public static final int seed = 425752111;

	public RandomDirectionalModel(TileHistoryQueue ref, MemoryTileBuffer membuf, DiskTileBuffer diskbuf,ScidbTileInterface api, int len) {
		super(ref,membuf,diskbuf,api,len);
		this.generator = new Random(seed); // use seed for consistency
	}
	
	@Override
	public List<DirectionPrediction> predictOrder(List<UserRequest> htrace) throws Exception {
		return super.predictOrder(htrace,true);
	}
	
	@Override
	public double computeConfidence(Direction d, List<UserRequest> trace) {
		return generator.nextDouble();
	}
}

