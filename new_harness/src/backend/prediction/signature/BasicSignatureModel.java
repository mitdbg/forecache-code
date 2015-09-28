package backend.prediction.signature;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.OldScidbTileInterface;
import backend.disk.TileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.prediction.BasicModel;
import backend.prediction.TileHistoryQueue;
import backend.util.NiceTileBuffer;
import backend.util.SignatureMap;
import backend.util.TileKey;

// has uses special data structure to track signatures
public abstract class BasicSignatureModel extends BasicModel {
	protected SignatureMap sigMap;
	
	public BasicSignatureModel(TileHistoryQueue ref, NiceTileBuffer membuf, 
			NiceTileBuffer diskbuf,TileInterface api, int len,
			SignatureMap sigMap) {
		super(ref,membuf,diskbuf,api,len);
		this.sigMap = sigMap;
	}
	
	public abstract double[] getSignature(TileKey id);
	
	//public abstract double[] getSignature(NiceTile tile);
}
