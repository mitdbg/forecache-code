package abstraction.prediction.signature;

import abstraction.prediction.BasicModel;
import abstraction.structures.DefinedTileView;
import abstraction.structures.NewTileKey;
import abstraction.structures.SessionMetadata;

// has uses special data structure to track signatures
public abstract class BasicSignatureModel extends BasicModel {
	public BasicSignatureModel(int len) {
		super(len);
	}
	
	public abstract double[] getSignature(SessionMetadata md, DefinedTileView dtv, NewTileKey id);
}
