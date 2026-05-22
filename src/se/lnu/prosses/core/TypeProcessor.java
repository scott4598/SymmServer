package se.lnu.prosses.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import se.lnu.prosses.configs.Constants;
import se.lnu.prosses.configs.TypeHandlingConfig;
import se.lnu.prosses.utils.Utils;
import soot.ArrayType;
import soot.PrimType;
import soot.SootClass;
import soot.SootField;
import soot.Type;
import soot.Value;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.JAddExpr;
import soot.jimple.internal.JAndExpr;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JCmpExpr;
import soot.jimple.internal.JCmpgExpr;
import soot.jimple.internal.JCmplExpr;
import soot.jimple.internal.JDivExpr;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JGeExpr;
import soot.jimple.internal.JGtExpr;
import soot.jimple.internal.JInstanceOfExpr;
import soot.jimple.internal.JLeExpr;
import soot.jimple.internal.JLengthExpr;
import soot.jimple.internal.JLtExpr;
import soot.jimple.internal.JMulExpr;
import soot.jimple.internal.JNeExpr;
import soot.jimple.internal.JNegExpr;
import soot.jimple.internal.JOrExpr;
import soot.jimple.internal.JRemExpr;
import soot.jimple.internal.JShlExpr;
import soot.jimple.internal.JShrExpr;
import soot.jimple.internal.JUshrExpr;
import soot.jimple.internal.JXorExpr;

public class TypeProcessor {

	public Hashtable<String, Set<String>> subtypeSupertypesMap = new Hashtable<String, Set<String>>();
	private List<Set<Type>> castableTypeGroups = new ArrayList<Set<Type>>();
	//private ClassHierarchy typeHierarchy = new ClassHierarchy(null);
	public List<SootField> allAccessedFields = new ArrayList<SootField>();

	public TypeHandlingConfig typeHandlingConfig;

	public TypeProcessor(TypeHandlingConfig typeHandlingConfig) {
		this.typeHandlingConfig = typeHandlingConfig;
	}

	private String addMissingTypesOfAccessedFields(List<SootField> accessedFieldTypes, List<String> leafTypes, ArrayList<String> missingTypes) {
		String typesDescriptions = "";
		for (SootField field : accessedFieldTypes)
			allAccessedFields.remove(field);
		for (SootField field : allAccessedFields)
			if (!missingTypes.contains(SootUtilities.getClassName(field.getDeclaringClass())))
				missingTypes.add(SootUtilities.getClassName(field.getDeclaringClass()));
		for (String type : missingTypes)
			typesDescriptions += getSingleTypeDescription(type, accessedFieldTypes, leafTypes);
		return typesDescriptions;
	}

	public void addNewType(final Type type2) {
		//	Utils.logErr(getClass(), type2.toString() );
		if(SootUtilities.isPrimType(type2)) {
			Utils.logErr(getClass(), type2 + " is a primitive type. Cannot add it to the type hierarchy.");
			return;
		}
		for (Set<Type> group : castableTypeGroups)
			if (group.contains(type2))
				return;
		this.castableTypeGroups.add(new HashSet<Type>() {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			{
				add(type2);
			}
		});
		// Utils.log(this.getClass(),"New type" + type2);
	}

	public void addsubtypeSupertypeRelation(Type type1, Type type2) throws TransformationException, TypeTransformationException {
		//System.out.println(type1 + " : " + type2);
		if (type2 == null || type2.toString().equals("null_type"))
			return;
		while (type1 instanceof ArrayType)
			type1 = ((ArrayType) type1).baseType;
		while (type2 instanceof ArrayType)
			type2 = ((ArrayType) type2).baseType;

		if (SootUtilities.isPrimType(type1) && SootUtilities.isPrimType(type2))
			return;

		if (!SootUtilities.isPrimType(type1) && !SootUtilities.isPrimType(type2)) {
			constructTypeHierarchyFromClassDef (soot.Scene.v().getSootClass(type1.toString()));
			boolean compatbileTypes = false;
			if (!type1.equals(type2)) {
				constructTypeHierarchyFromClassDef (soot.Scene.v().getSootClass(type2.toString()));
				if (this.isSubTypeSuperType(soot.Scene.v().getSootClass(type1.toString()),
						soot.Scene.v().getSootClass(type2.toString()))) {
					if (subtypeSupertypesMap.get(type1.toString()) == null)
						this.subtypeSupertypesMap.put(type1.toString(), new HashSet<String>());
					this.subtypeSupertypesMap.get(type1.toString()).add(type2.toString());
					compatbileTypes = true;
				}
				if (this.isSubTypeSuperType(soot.Scene.v().getSootClass(type2.toString()),
						soot.Scene.v().getSootClass(type1.toString()))) {
					if(compatbileTypes) {
						Utils.logErr(this.getClass(), type1 + " and " + type2 + " cannot be a superclass and subclass of each other at the same time!");
						this.subtypeSupertypesMap.get(type1.toString()).add(type2.toString());
					}
					else {
						if (subtypeSupertypesMap.get(type2.toString()) == null)
							this.subtypeSupertypesMap.put(type2.toString(), new HashSet<String>());
						this.subtypeSupertypesMap.get(type2.toString()).add(type1.toString());
						compatbileTypes = true;
					}
				}
				if (!compatbileTypes && !type1.toString().equals("null_type")) {
					if(type1.toString().equals("java.lang.Object")) {
						if (subtypeSupertypesMap.get(type2.toString()) == null)
							this.subtypeSupertypesMap.put(type2.toString(), new HashSet<String>());
						this.subtypeSupertypesMap.get(type2.toString()).add(type1.toString());
					} else
						if(type2.toString().equals("java.lang.Object")) {
							if (subtypeSupertypesMap.get(type1.toString()) == null)
								this.subtypeSupertypesMap.put(type1.toString(), new HashSet<String>());
							this.subtypeSupertypesMap.get(type1.toString()).add(type2.toString());
						} else
							if (typeHandlingConfig.allowIncompatibleTypes){
								Utils.log(getClass(), "Added incompatible types: Type1: " + type1 + " and Type2:" + type2);	
								this.subtypeSupertypesMap.get(type1.toString()).add(type2.toString());
							}
							else{
								Utils.logErr(getClass(), type1 + " and " + type2 + " are considered incompatible!");
								throw new TypeTransformationException(type1 , type2 ,typeHandlingConfig.skipMethodsWithIncompatibleTypes  );
							}
				}
			} else if (!subtypeSupertypesMap.keySet().contains(type1.toString()))
				subtypeSupertypesMap.put(type1.toString(), new HashSet<String>());
		}
		else {
			if(!typeHandlingConfig.skipMethodsWithIncompatibleTypes && typeHandlingConfig.autoFixInconsistentTypes) {
				Utils.logErr(this.getClass(), "Warning: Not possible to cast " + type1 + " to " + type2  );
				//if(!SootUtilities.isPrimType(type2))
				{
					//String dummyMethod = createDummyTypeTranformerMethod(type1, type2);
					if(this.subtypeSupertypesMap.get(type1.toString())==null)
						this.subtypeSupertypesMap.put(type1.toString(), new HashSet<String>() );
					this.subtypeSupertypesMap.get(type1.toString()).add(type2.toString());
					return;
				}
			}
			throw new TypeTransformationException(type1, type2, typeHandlingConfig.skipMethodsWithIncompatibleTypes);
		}
	}

	String constructTypeDescriptions() {

		ArrayList<String> leafTypes = new ArrayList<String>();
		// typeHierarchy.buildTypesHierarchy(castableTypeGroups);

		// mapTypesToSuperTypes();
		List<SootField> accessedFieldTypes = new ArrayList<SootField>();
		String typesDescriptions = "";
		for (String type : subtypeSupertypesMap.keySet()) {
			typesDescriptions += getSingleTypeDescription(type, accessedFieldTypes, leafTypes);
		}
		if (!subtypeSupertypesMap.keySet().contains("android.os.IBinder")
				&& subtypeSupertypesMap.keySet().contains("android.content.Intent"))
			typesDescriptions += "android.os.IBinder {}\n";

		ArrayList<String> missingTypes = new ArrayList<String>();
		typesDescriptions += addMissingTypesOfAccessedFields(accessedFieldTypes, leafTypes,missingTypes );
		for (String type : leafTypes)
			if(!missingTypes.contains(type))
				typesDescriptions += type + "{}\n";

		return typesDescriptions;
	}

	void constructTypeHierarchyFromClassDef(SootClass declaringClass) {
		if (!subtypeSupertypesMap.containsKey(SootUtilities.getClassName(declaringClass))) {
			Set<String> out = new HashSet<String>();
			subtypeSupertypesMap.put(SootUtilities.getClassName(declaringClass), out);
			// for (SootField field : declaringClass.getFields()) {
			// 	constructTypeHierarchyFromTypeDef(field.getType(), declaringClass);
			// }
			// if (!declaringClass.isInterface()
			//     && soot.Scene.v().getActiveHierarchy().getSuperclassesOf(declaringClass).isEmpty()
			//     || declaringClass.isInterface()
			//     && soot.Scene.v().getActiveHierarchy().getSuperinterfacesOf(declaringClass).isEmpty()
			//     && soot.Scene.v().getActiveHierarchy().getImplementersOf(declaringClass).isEmpty())
			// 	;
			// else {
			// 	Set<SootClass> superClassesOrInterfaces = getSuperInterfaces(declaringClass);
			// 	for (SootClass superClasss : superClassesOrInterfaces) {
			// 		constructTypeHierarchyFromClassDef(superClasss);
			// 		out.add(SootUtilities.getClassName(superClasss));
			// 	}
			// }
			for (SootClass superClasss: getSuperInterfaces(declaringClass)) {
				constructTypeHierarchyFromClassDef(superClasss);
				out.add(SootUtilities.getClassName(superClasss));
			}
			for (SootField field : declaringClass.getFields()) {
				constructTypeHierarchyFromTypeDef(field.getType(), declaringClass);
			}
		}
	}

	void constructTypeHierarchyFromTypeDef(Type type, SootClass declaringClass) {
		while (type instanceof ArrayType)
			type = ((ArrayType) type).baseType;
		if (soot.Scene.v().getSootClass(type.toString()) != declaringClass && !(type instanceof PrimType)) {
			constructTypeHierarchyFromClassDef(soot.Scene.v().getSootClass(type.toString()));
			if (!SootUtilities.isPrimType(declaringClass.getType()))
				constructTypeHierarchyFromClassDef(declaringClass);
		}
	}

	public String escapeTypeName(String type) {
		// TODO Auto-generated method stub
		return type.replaceAll("-", "");
	}

	String getSingleTypeDescription(String type, List<SootField> missingFieldTypes, List<String> leafTypes) {
		SootClass declaringClass = soot.Scene.v().getSootClass(type);
		String fields = " {";
		/*for (SootField field : allAccessedFields)
			if (SootUtilities.getClassName(field.getDeclaringClass())
					.equals(SootUtilities.getClassName(declaringClass))) {
				fields += escapeTypeName(field.getType().toString()) + "  " + field.getName() + "; ";
				missingFieldTypes.add(field);
			}*/
		for (SootField field : declaringClass.getFields()) {
			if (field.isStatic ())
				fields += "static ";
			fields += escapeTypeName(field.getType().toString()) + "  " + field.getName() + "; ";
			missingFieldTypes.add(field);
		}
		fields += "}";
		String superClasses = "", separator = "";
		for (SootClass sup: getSuperInterfaces (declaringClass)) {
			String str = SootUtilities.getClassName(sup);
			superClasses += separator + str;
			separator = ",";
			// NB: ???
			if (!subtypeSupertypesMap.keySet().contains(str) && !leafTypes.contains(str))
				leafTypes.add(str);
		}
		if (type.trim().equals("android.content.Intent")) {
			superClasses += separator + "android.os.IBinder";

		}
		return SootUtilities.getClassName(declaringClass) + (superClasses.equals("") ? "" : " <: " + superClasses)
				+ fields + "\n";
	}

	Set<SootClass> getSuperInterfaces(SootClass declaringClass) {
 		Set<SootClass> res = new HashSet<SootClass>();
 		if (!declaringClass.isInterface() && declaringClass.hasSuperclass ())
 			res.add (declaringClass.getSuperclass ());
 		res.addAll(declaringClass.getInterfaces ());
 		return res;
 	}


	boolean isSubTypeSuperType(SootClass subClass, SootClass superClasss) throws TransformationException {
		try {
			if (subClass.isInterface())
				if (superClasss.isInterface())
					return soot.Scene.v().getActiveHierarchy().isInterfaceSubinterfaceOf(subClass, superClasss);
				else {
					if (soot.Scene.v().getActiveHierarchy().getImplementersOf(subClass) != null
							&& soot.Scene.v().getActiveHierarchy().getImplementersOf(subClass).contains(superClasss))
						return false;
					for (SootClass implementingClass : soot.Scene.v().getActiveHierarchy().getImplementersOf(subClass)) {
						if (isSubTypeSuperType(implementingClass, superClasss))
							return true;
					}
					return false;
				}
			else if (superClasss.isInterface())
				return soot.Scene.v().getActiveHierarchy().getImplementersOf(superClasss).contains(subClass);
			else if (superClasss.getName().equals("java.lang.Object"))
				return true;
			else
				return soot.Scene.v().getActiveHierarchy().isClassSubclassOf(subClass, superClasss);
		}
		catch(java.lang.RuntimeException ex) {
			throw new TransformationException("Runtime error with checking the inheritance relations between " + subClass.getName() + " and " + superClasss.getName());
		}
	}
	/*public void joinTypeGroupsOld(Type type1, Type type2) {
		if (type2 == null || type2.toString().equals("null_type"))
			return;
		while (type1 instanceof ArrayType)
			type1 = ((ArrayType) type1).baseType;
		while (type2 instanceof ArrayType)
			type2 = ((ArrayType) type2).baseType;

		if (SootUtilities.isPrimType(type1) || SootUtilities.isPrimType(type2))
			return;

		if (type1.equals(type2) && SootUtilities.isLibraryClass(soot.Scene.v().getSootClass(type2.toString()))) {
			addNewType(type2);
			return;
		}
		Set<Type> type1Group = null;
		Set<Type> type2Group = null;
		for (Set<Type> group : castableTypeGroups) {
			if (group.contains(type1))
				type1Group = group;
			if (group.contains(type2))
				type2Group = group;
			if (type1Group != null && type2Group != null)
				break;
		}
		if (type1Group != null)
			if (type2Group != null) {
				if (type1Group == type2Group)
					return;
				type2Group.addAll(type1Group);
				this.castableTypeGroups.remove(type1Group);
			} else
				type2Group = type1Group;
		if (type2Group == null) {
			type2Group = new HashSet<Type>();
			this.castableTypeGroups.add(type2Group);

		}
		type2Group.add(type1);
		type2Group.add(type2);
	}*/

	// void mapTypesToSuperTypes() {
	// 	Stack<ClassHierarchy> stack = new Stack<ClassHierarchy>();
	// 	for (ClassHierarchy node : typeHierarchy.getLeaves()) {
	// 		stack.add(node);
	// 		Utils.log(this.getClass(), "Leaf: " + node.data);
	// 	}
	// 	List<String> visited = new ArrayList<String>();
	// 	Utils.log(this.getClass(), "Building subtype/supertype relations");
	// 	if (stack.peek().data != null)
	// 		while (!stack.isEmpty()) {
	// 			ClassHierarchy node = stack.pop();
	// 			// Utils.log(this.getClass(),"Processing Type " + node.data);
	// 			String curType = SootUtilities.getClassName(node.data);
	// 			visited.add(curType);
	// 			if (this.subtypeSupertypesMap.get(curType) == null)
	// 				this.subtypeSupertypesMap.put(curType, new HashSet<String>());
	// 			for (ClassHierarchy parent : node.parents) {
	// 				Utils.log(this.getClass(), "parent.data " + parent.data);
	// 				if (parent.data != null) {
	// 					this.subtypeSupertypesMap.get(curType).add(SootUtilities.getClassName(parent.data));
	// 					if (!visited.contains(SootUtilities.getClassName(parent.data)))
	// 						stack.add(parent);
	// 				}
	// 			}
	// 		}
	// }

	public void processExpressionTypes(Value value) throws TransformationException, TypeTransformationException {
		// Value value = expression.getValue();
		if (SootUtilities.isLocalVariable(value) || SootUtilities.isParameterRef(value)
				|| SootUtilities.isConstant(value) || SootUtilities.isStaticField(value)
				|| SootUtilities.isFieldRef(value))
			return;

		if (value instanceof JCmpExpr || value instanceof JAddExpr || value instanceof JNeExpr
				|| value instanceof JEqExpr || (value instanceof JMulExpr) || value instanceof JAndExpr
				|| value instanceof JOrExpr || value instanceof JDivExpr
				|| value instanceof soot.jimple.internal.JSubExpr || value instanceof JCmplExpr
				|| value instanceof JCmpgExpr || value instanceof JXorExpr || value instanceof JGtExpr
				|| value instanceof JUshrExpr || value instanceof JLtExpr || value instanceof JShlExpr
				|| value instanceof JNeExpr || value instanceof JShrExpr || value instanceof JGeExpr
				|| value instanceof JLeExpr) {

			if (((soot.jimple.internal.AbstractBinopExpr) value).getOp1()
					.getType() != ((soot.jimple.internal.AbstractBinopExpr) value).getOp2().getType()) {
				this.processTypes(((soot.jimple.internal.AbstractBinopExpr) value).getOp1().getType(),
						((soot.jimple.internal.AbstractBinopExpr) value).getOp2());
				// this.processTypes(((soot.jimple.internal.AbstractBinopExpr)value).getOp2().getType(),
				// ((soot.jimple.internal.AbstractBinopExpr)value).getOp1());
			}
			if (((soot.jimple.internal.AbstractBinopExpr) value).getOp1().getUseBoxes().size() > 1)
				processExpressionTypes(((soot.jimple.internal.AbstractBinopExpr) value).getOp1());
			if (((soot.jimple.internal.AbstractBinopExpr) value).getOp2().getUseBoxes().size() > 1)
				processExpressionTypes(((soot.jimple.internal.AbstractBinopExpr) value).getOp2());
			return;
		}
		if (value instanceof JNegExpr || value instanceof JRemExpr || value instanceof JLengthExpr
				|| value instanceof JInstanceOfExpr) {
			if (((soot.jimple.internal.AbstractUnopExpr) value).getOp().getUseBoxes().size() > 1)
				processExpressionTypes(((soot.jimple.internal.AbstractUnopExpr) value).getOp());
			return;
		}
		if (SootUtilities.isCastExpr(value)) {
			processExpressionTypes(((JCastExpr) value).getOp());
			return;
		}

	}

	public void processTypes(Type lhsType, Value rhsValue) throws TransformationException, TypeTransformationException {
		while (lhsType instanceof ArrayType)
			lhsType = ((ArrayType) lhsType).baseType;

		if (SootUtilities.isCastExpr(rhsValue)) {
			addsubtypeSupertypeRelation(((JCastExpr) rhsValue).getOp().getType(), ((JCastExpr) rhsValue).getCastType());
			addsubtypeSupertypeRelation(((JCastExpr) rhsValue).getOp().getType(), rhsValue.getType());
		}
		if (SootUtilities.isStaticField(rhsValue)) {
			SootClass rhsClass = ((StaticFieldRef) rhsValue).getField().getDeclaringClass();
			addNewType(rhsClass.getType());
			//addsubtypeSupertypeRelation(rhsClass.getType(), null);
			constructTypeHierarchyFromClassDef (rhsClass);
		}
		addsubtypeSupertypeRelation(lhsType, rhsValue.getType());
	}

	public void writeTypeHierarchyDotFile(String targetPath) {

		String typesGraph = "";
		String nodes = ""; int index = 0;
		for (String type : this.subtypeSupertypesMap.keySet()) {
			if(subtypeSupertypesMap.get(type).size()==0)
				nodes  += "node" + index++ + "[shape=diamond, label = " + "\"" + type + "\"];\n";
			else
				for (String parentType : subtypeSupertypesMap.get(type))
					typesGraph += "\"" + parentType + "\" -> \"" + type + "\" ;\n		";
		}
		typesGraph ="digraph \"typesDiagram\" {\n" + nodes + typesGraph + "}";
		Utils.writeTextFile(targetPath + "/types.dot", typesGraph);
	}

	public void writeTypesClassesFile(String targetPath) {
		String types =  Constants.dummyClass + "  <: java.lang.Object{}\n";
		types +=	constructTypeDescriptions();
		Utils.writeTextFile(targetPath + "/types.classes", types);
	}

}
