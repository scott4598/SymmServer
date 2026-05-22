package se.lnu.prosses.core;

import se.lnu.prosses.configs.Constants;
import soot.Type;

public class TypeCaster {
	Type type1;
	Type type2;
	
	public TypeCaster(Type type12, Type type22) {
		type1 = type12;
		type2 = type22;
	}

	public TypeCaster() {
	}

	public String callWith(String left, String right) {
		return  left + "=" + Constants.dummyClass + "." + getMethodName() + "(" + right + ")" ;
	}

	public String getMethodName() {
		return  escapeTypeName(type1)+"To"+escapeTypeName(type2);
	}
	
	private String escapeTypeName(Type type) {
		return type.toString().replaceAll("\\.", "").replaceAll("-", "");
	}

}
