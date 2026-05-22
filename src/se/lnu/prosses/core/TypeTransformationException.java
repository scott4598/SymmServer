package se.lnu.prosses.core;

import se.lnu.prosses.utils.Utils;
import soot.Type;
import soot.Value;

public class TypeTransformationException extends Exception {
	Type type1;
	Type type2;
	boolean skipMethodsWithIncompatibleTypes; 

	public TypeTransformationException(Type type12, Type type22, boolean skipMethodsWithIncompatibleTypes2) {
		type1 = type12;
		type2 = type22;
		skipMethodsWithIncompatibleTypes = skipMethodsWithIncompatibleTypes2;
	}

	public TypeTransformationException(TypeTransformationException e) {
		type1 = e.type1;
		type2= e.type2;
		skipMethodsWithIncompatibleTypes = e.skipMethodsWithIncompatibleTypes;
	}

	public void print() {
		Utils.logErr(getClass(), "Warning: Could not cast " + type2 + " to " + type1 + 
				"There will possibly be syntax errors in the compiled code.\n");
	}

	public TypeCaster createDummyCastMethod(Value value, Value renameToThisVariable) {
		return null;
	}

	public void handle() throws TransformationException {
		if(skipMethodsWithIncompatibleTypes)
			throw new TransformationException ("Skipped the method processing due to type incompatibilities");
		else
			print();		
	}
}
