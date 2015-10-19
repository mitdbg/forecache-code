package abstraction.prediction;

import abstraction.enums.Model;

public class DistanceModifiers {
	protected static double normalModifier = 2;
	protected static double siftModifier = 1.5;
	protected static double defaultModifier = 2;
	
	public static double getBase(Model m) {
		double returnval = 1;
		switch(m) {
		case NORMAL: returnval = normalModifier;
		break;
		case SIFT: returnval = siftModifier;
		break;
		default: returnval = defaultModifier;
		}
		return 1.0 / returnval;
	}
}
