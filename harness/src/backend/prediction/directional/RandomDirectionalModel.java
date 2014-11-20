package backend.prediction.directional;

import java.util.List;
import java.util.Random;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.MemoryTileBuffer;
import backend.prediction.BasicModel;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.TileKey;

import utils.UserRequest;

public class RandomDirectionalModel extends BasicModel {
	private Random generator;
	//public static final int seed = 7;
	public static final int seed = 425752111;

	public RandomDirectionalModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, DiskNiceTileBuffer diskbuf,ScidbTileInterface api, int len) {
		super(ref,membuf,diskbuf,api,len);
		this.generator = new Random(seed); // use seed for consistency
	}
	
	@Override
	public List<DirectionPrediction> predictOrder(List<TileKey> htrace) {
		return super.predictOrder(htrace,true);
	}
	
	@Override
	public double computeConfidence(Direction d, List<TileKey> trace) {
		return generator.nextDouble();
	}
}

