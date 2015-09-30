package backend.prediction.signature;

import backend.prediction.BasicModel;
import abstraction.prediction.DefinedTileView;
import abstraction.prediction.SessionMetadata;
import abstraction.util.NewTileKey;

// has uses special data structure to track signatures
public abstract class BasicSignatureModel extends BasicModel {
	public BasicSignatureModel(int len) {
		super(len);
	}
	
	public abstract double[] getSignature(SessionMetadata md, DefinedTileView dtv, NewTileKey id);
}
