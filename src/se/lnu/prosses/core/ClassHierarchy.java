package se.lnu.prosses.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import se.lnu.prosses.utils.Utils;
import soot.SootClass;
import soot.Type;

public class ClassHierarchy implements Iterable<ClassHierarchy> {

	class subTypeRelation implements Comparator {
		@Override
		public int compare(Object o1, Object o2) {
			// TODO Auto-generated method stub
			return (isSubTypeSuperType(soot.Scene.v().getSootClass(o1.toString()),
					soot.Scene.v().getSootClass(o2.toString())) ? 0 : 1);
		}

	}

	SootClass data;
	List<ClassHierarchy> parents;

	List<ClassHierarchy> children;

	HashMap<String, ClassHierarchy> processedTypesToClassHierarchyMap = new HashMap<String, ClassHierarchy>();

	public ClassHierarchy(SootClass data) {
		this.data = data;
		this.children = new LinkedList<ClassHierarchy>();
		this.parents = new LinkedList<ClassHierarchy>();
	}

	public ClassHierarchy addChild(SootClass child) {
		ClassHierarchy childNode = new ClassHierarchy(child);
		childNode.parents.add(this);
		this.children.add(childNode);
		return childNode;
	}

	public ClassHierarchy addChildNode(ClassHierarchy childNode) {
		childNode.parents.add(this);
		this.children.add(childNode);
		return childNode;
	}

	ClassHierarchy buildSingleTypeHierarchy(SootClass declaringClass, Set<Type> group) {
		Utils.log(this.getClass(), "Processing the type " + declaringClass.getName());
		// typeDesc += "\n Processing the type " + declaringClass.getName();
		ClassHierarchy newNode = processedTypesToClassHierarchyMap.get(declaringClass.getName().toString());// findNode(declaringClass);
		if (newNode != null) {
			// Utils.log(this.getClass(),newNode.data.getName() + " is retrieved");
			return newNode;
		}
		newNode = new ClassHierarchy(declaringClass);
		if (!hasSubTypeInList(declaringClass, group)) {
			addChildNode(newNode);
			// this.typeDesc +=newNode.data.getName() + " is a leaf\n";
		}
		processedTypesToClassHierarchyMap.put(declaringClass.getName().toString(), newNode);
		for (Type type : group) {
			if (!type.equals(declaringClass.getType())) {
				SootClass classs = soot.Scene.v().getSootClass(type.toString());
				if (isSubTypeSuperType(declaringClass, classs)) {
					Utils.log(this.getClass(), declaringClass + "<:" + classs.getName());
					ClassHierarchy parent = buildSingleTypeHierarchy(classs, group);
					parent.addChildNode(newNode);
				}
			}
			// Utils.log(this.getClass(),"CP3 " + type);
		}
		return newNode;
	}

	public void buildTypesHierarchy(List<Set<Type>> castableTypeGroups) {
		for (int index = 0; index < castableTypeGroups.size(); index++) {
			Comparator<Type> c = new subTypeRelation();
			Collections.sort(new ArrayList<Type>(castableTypeGroups.get(index)), c);
			System.out.print("\n\nNew Types Group: \n");
			// typeDesc += "\n\nNew Types Group: ";
			for (Type type : castableTypeGroups.get(index)) {
				Utils.log(this.getClass(), "Building the type hierarchy of " + type.toQuotedString());
				// typeDesc += "\n Building the type hierarchy of " + type.toQuotedString();
				buildSingleTypeHierarchy(soot.Scene.v().getSootClass(type.toString()), castableTypeGroups.get(index));
			}
		}

	}

	public ClassHierarchy findNode(SootClass node) {
		if (data == node)
			return this;
		else {
			for (ClassHierarchy child : this.children) {
				if (!(child.data).isInterface() && !node.isInterface()
						&& soot.Scene.v().getActiveHierarchy().getSuperclassesOf(node).contains(child.data)
						|| child.data.isInterface() && node.isInterface()
								&& soot.Scene.v().getActiveHierarchy().getSuperinterfacesOf(node).contains(child.data)
						|| node == child.data)
					return child.findNode(node);
			}
		}
		return null;
	}

	public ArrayList<ClassHierarchy> getLeaves() {
		ArrayList<ClassHierarchy> res = new ArrayList<ClassHierarchy>();
		if (this.children.isEmpty())
			res.add(this);
		else
			for (ClassHierarchy node : this.children)
				res.addAll(node.getLeaves());
		return res;
	}

	boolean hasSubTypeInList(SootClass classs, Set<Type> group) {
		for (Type type : group) {
			SootClass subClasss = soot.Scene.v().getSootClass(type.toString());
			if (subClasss != classs && isSubTypeSuperType(subClasss, classs))
				return true;
		}
		return false;
	}

	boolean isSubTypeSuperType(SootClass subClass, SootClass superClasss) {
		/*
		 * if(TypeProcessor.castedTypes.get(subClass.getName())!=null &&
		 * TypeProcessor.castedTypes.get(subClass.getName()).contains(superClasss.
		 * getName())) { return true; }
		 */
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

	@Override
	public Iterator<ClassHierarchy> iterator() {
		return null;
	}

}
