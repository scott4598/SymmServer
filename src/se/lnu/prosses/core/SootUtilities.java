package se.lnu.prosses.core;

/* this class is a modified version of a class borrowed from
 * https://www.programcreek.com/java-api-examples/?code=petablox-project/petablox/petablox-master/src/petablox/analyses/provenance/kcfa/SimpleCtxtsAnalysis.java#*/

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import se.lnu.prosses.utils.Utils;
import soot.ArrayType;
import soot.Body;
import soot.Hierarchy;
import soot.Immediate;
import soot.Local;
import soot.PrimType;
import soot.RefLikeType;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DoubleConstant;
import soot.jimple.FieldRef;
import soot.jimple.FloatConstant;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.SwitchStmt;
import soot.jimple.ThrowStmt;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JCmpExpr;
import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNewMultiArrayExpr;
import soot.jimple.internal.JNopStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.shimple.PhiExpr;
import soot.util.Chain;

public class SootUtilities {
    private static HashMap<SootClass, LinkedList<SootMethod>> virtualMethodCache = new HashMap<>();
    private static HashMap<SootClass, HashSet<SootClass>> classCache = new HashMap<>();
    private static HashMap<SootClass, HashSet<SootClass>> interfaceCache = new HashMap<>();
    public static Hierarchy h = null;

    public static boolean extendsClass(SootClass c, SootClass sup) {
	HashSet<SootClass> sups = classCache.get(c);
	if (sups == null) // when c is a phantom class
	    return false;
	else
	    return sups.contains(sup);
    }

    public static String getClassName(SootClass declaringClass) {
	return escapType(declaringClass.getName().replaceAll("-", ""));// .replaceAll("dummyMainClass", "NNClass");
    }

    public static SootField getField(Value value) {
	if (value instanceof JInstanceFieldRef)
	    return ((JInstanceFieldRef) value).getField();
	else if (value instanceof StaticFieldRef)
	    return ((StaticFieldRef) value).getField();
	else if (value instanceof JInstanceFieldRef)
	    return ((JInstanceFieldRef) value).getField();
	return null;
    }

    public static int getID(Unit u) { // TODO
	return 0;
    }

    public static Value getInstanceInvkBase(Unit q) {
	assert (q instanceof JInvokeStmt || q instanceof JAssignStmt);
	InvokeExpr ie;
	if (q instanceof JInvokeStmt)
	    ie = ((JInvokeStmt) q).getInvokeExpr();
	else if (q instanceof JAssignStmt)
	    ie = ((InvokeExpr) (((JAssignStmt) q).getRightOpBox().getValue()));
	else
	    ie = null;
	if (ie != null && ie instanceof InstanceInvokeExpr)
	    return ((InstanceInvokeExpr) ie).getBase();
	return null;
    }

    public static List<Value> getInvokeArgs(Unit q) {
	assert (q instanceof JInvokeStmt || q instanceof JAssignStmt);
	if (q instanceof JInvokeStmt)
	    return ((JInvokeStmt) q).getInvokeExpr().getArgs();
	else if (q instanceof JAssignStmt)
	    if ((((JAssignStmt) q).getRightOpBox().getValue()) instanceof InvokeExpr)
		return ((InvokeExpr) (((JAssignStmt) q).getRightOpBox().getValue())).getArgs();
	    else if ((((JAssignStmt) q).getRightOpBox().getValue()) instanceof JNewExpr)
		return null;

	return null;
    }

    public static Value getInvokeBase(Unit unit) throws TransformationException {
	InvokeExpr invoke = getInvokeExpr(unit);
	if (invoke instanceof StaticInvokeExpr)
	    return null;
	else if (invoke instanceof SpecialInvokeExpr)
	    return ((SpecialInvokeExpr) invoke).getBase();
	else if (invoke instanceof JInterfaceInvokeExpr)
	    return ((JInterfaceInvokeExpr) invoke).getBase();
	else if (invoke instanceof JVirtualInvokeExpr)
	    return ((JVirtualInvokeExpr) invoke).getBase();
	else if (invoke instanceof JDynamicInvokeExpr) {
	    throw new TransformationException(
		    "Cannot get the base of the dynamic invoke expression of " + unit.toString());
	} else
	    throw new TransformationException("Cannot transform " + unit.toString());
    }

    public static String getInvokeBaseType(Unit invoke) throws TransformationException {
	if (isStaticInvoke(invoke))// || isSpecialInvoke(invoke))
	    return getClassName(getInvokeExpr(invoke).getMethod().getDeclaringClass());
	Value base = getInvokeBase(invoke);
	return escapType(base.getType().toString());
    }

    private static String escapType(String type) {
	return type.replace(".checkpoint ", ".ccheckpoint ").replace(".checkpoint.", ".ccheckpoint.");

    }

    public static String getInvokeType(Unit unit) throws TransformationException {
	if (isStaticInvoke(unit))// || isSpecialInvoke(invoke))
	    return getClassName(getInvokeExpr(unit).getMethod().getDeclaringClass());
	InvokeExpr invoke = getInvokeExpr(unit);
	if (invoke instanceof StaticInvokeExpr)
	    return null;
	else if (invoke instanceof SpecialInvokeExpr)
	    return escapType(((SpecialInvokeExpr) invoke).getMethod().getDeclaringClass().toString());
	else if (invoke instanceof JInterfaceInvokeExpr)
	    return escapType(((JInterfaceInvokeExpr) invoke).getMethod().getDeclaringClass().toString());
	else if (invoke instanceof JVirtualInvokeExpr)
	    return escapType(((JVirtualInvokeExpr) invoke).getMethod().getDeclaringClass().toString());
	else if (invoke instanceof JDynamicInvokeExpr) {
	    throw new TransformationException(
		    "Cannot get the base of the dynamic invoke expression of " + unit.toString());
	} else
	    throw new TransformationException("Cannot transform " + unit.toString());
    }

    public static InvokeExpr getInvokeExpr(Unit q) {
	if (!(q instanceof JInvokeStmt || q instanceof JAssignStmt))
	    return null;
	InvokeExpr ie;
	if (q instanceof JInvokeStmt)
	    ie = ((JInvokeStmt) q).getInvokeExpr();
	else if (q instanceof JAssignStmt && (((JAssignStmt) q).getRightOpBox().getValue()) instanceof InvokeExpr)
	    ie = ((InvokeExpr) (((JAssignStmt) q).getRightOpBox().getValue()));
	else
	    ie = null;
	return ie;
    }

    public static List<Integer> getLineNumber(SootMethod m, int bci) { // TODO
	return null;
    }

    public static List<Integer> getLineNumber(SootMethod m, Local v) { // TODO
	return null;
    }

    /*
     * Returns the local variables of the method - arguments first, followed by
     * temporaries
     */
    public static List<Local> getLocals(SootMethod m) {
	if (m.isConcrete()) {
	    Body b = m.getActiveBody();
	    List<Local> regs = b.getParameterLocals();
	    if (!m.isStatic())
		regs.add(0, b.getThisLocal());

	    List<Local> temps = new ArrayList<>();
	    Chain<Local> allLocals = b.getLocals();
	    Iterator<Local> it = allLocals.iterator();
	    while (it.hasNext()) {
		Local l = it.next();
		if (!regs.contains(l))
		    temps.add(l);
	    }
	    regs.addAll(temps);
	    return regs;
	} else {
	    return Collections.emptyList();
	}
    }

    /*
     * Returns the local variables corresponding to the arguments of the method
     */
    public static Local[] getMethArgLocals(SootMethod m) {
	int numLocals = m.getParameterCount();
	List<Local> regs;
	try {
	    regs = m.getActiveBody().getParameterLocals();
	    if (!m.isStatic()) {
		numLocals++; // Done to consider the "this" parameter passed
		regs.add(0, m.getActiveBody().getThisLocal());
	    }
	    Local[] locals = new Local[numLocals];
	    for (int i = 0; i < regs.size(); i++) {
		locals[i] = regs.get(i);
	    }
	    return locals;
	} catch (RuntimeException e) {
	    Utils.log(SootUtilities.class, "No method body was found for the method: " + m.getSignature());
	}
	;
	return null;
    }

    public static List<String> getRegName(SootMethod m, Local v) { // TODO
	return null;
    }

    /*
     * Returns the local variable returned by method m Returns null if method does
     * not have a return statement or returns a constant
     */
    public static Local getReturnLocal(SootMethod m) {
	try {
	    Body body = m.retrieveActiveBody();
	    for (Unit unit : body.getUnits()) {
		Stmt s = (Stmt) unit;
		if (s instanceof ReturnStmt) {
		    Immediate retOp = (Immediate) ((ReturnStmt) s).getOp();
		    if (retOp instanceof Local)
			return (Local) retOp;
		}
	    }
	} catch (RuntimeException e) {
	    Utils.log(SootUtilities.class, "No method body was found for the method: " + m.getSignature());
	}
	;
	return null;
    }

    public static List<Unit> getReturnUnit(SootMethod m) {
	List<Unit> results = new ArrayList<>();
	try {
	    Body body = m.retrieveActiveBody();
	    for (Unit unit : body.getUnits()) {
		Stmt s = (Stmt) unit;
		if (s instanceof ReturnStmt) {
		    results.add(s);
		}
	    }
	} catch (RuntimeException e) {
	    Utils.log(SootUtilities.class, "No method body was found for the method: " + m.getSignature());
	}
	;
	return results;
    }

    public static SootMethod getVirtualMethod(SootClass c, SootMethod vm) {
	for (SootMethod m : virtualMethodCache.get(c)) {
	    if (m.getName().equals(vm.getName()) && m.getReturnType().equals(vm.getReturnType())
		    && m.getParameterTypes().equals(vm.getParameterTypes()))
		return m;
	}
	Utils.log(SootUtilities.class, "WARN: RTA method not found " + vm.getName());
	return null;
    }

    public static int hashCode(Unit u) { // TODO
	// if (DETERMINISTIC) return getID();
	// else return System.identityHashCode(this);
	return u.hashCode();
    }

    public static boolean implementsInterface(SootClass c, SootClass inter) {
	HashSet<SootClass> inters = interfaceCache.get(c);
	if (inters == null) // when c is a phantom class
	    return false;
	else
	    return inters.contains(inter);
    }

    public static boolean isAssign(Unit q) {
	if (q instanceof JAssignStmt)
	    return true;
	else
	    return false;
    }

    public static boolean isBranch(Unit u) {
	if (u instanceof IfStmt || u instanceof GotoStmt || u instanceof SwitchStmt || u instanceof ThrowStmt
		|| u instanceof ReturnStmt || u instanceof ReturnVoidStmt)
	    return true;
	return false;
    }

    public static boolean isCastExpr(Value value) {
	return value instanceof JCastExpr;
    }

    public static boolean isCmpExpr(Value value) {
	return value instanceof JCmpExpr;
    }

    public static boolean isConditional(Unit u) {
	if (u instanceof IfStmt)
	    return true;
	return false;
    }

    public static boolean isConstant(Value value) {
	return (value instanceof FloatConstant) || (value instanceof IntConstant) || (value instanceof DoubleConstant)
		|| (value instanceof LongConstant) || (value instanceof StringConstant);
    }

    public static boolean isConstructorInvokation(Unit lastUpdatingUnit) {
	return lastUpdatingUnit instanceof JAssignStmt
		&& ((JAssignStmt) lastUpdatingUnit).getRightOpBox().getValue() instanceof JNewExpr;
    }

    public static boolean isFieldLoad(JAssignStmt a) {
	Value right = a.getRightOpBox().getValue();
	if (a.containsFieldRef()) {
	    FieldRef fr = a.getFieldRef();
	    if (right.toString().equals(fr.toString()))
		return true;
	}
	return false;
    }

    public static boolean isFieldRef(Value value) {
	return value instanceof JInstanceFieldRef;
    }

    public static boolean isFieldStore(JAssignStmt a) {
	Value left = a.getLeftOpBox().getValue();
	if (a.containsFieldRef()) {
	    FieldRef fr = a.getFieldRef();
	    if (left.toString().equals(fr.toString()))
		return true;
	}
	return false;
    }

    public static boolean isGotoStatment(Unit unit) {
	return unit instanceof JGotoStmt;
    }

    // public static boolean isLoadInst(JAssignStmt a) {
    // return false;
    // }

    public static boolean isIdentityStmt(Unit unit) {
	return (unit instanceof JIdentityStmt);
    }

    public static boolean isInitInvokation(Unit unit) throws TransformationException {
	try {
	    InvokeExpr ie;
	    if (unit instanceof JInvokeStmt || unit instanceof JAssignStmt)
		ie = getInvokeExpr(unit);
	    else
		ie = null;
	    return ie != null && ie.getMethod().getName().equals("<init>");
	} catch (RuntimeException ex) {
	    throw new TransformationException("An error occuerd in resolving the method in " + unit.toString());
	}
    }

    public static boolean isInstanceInvoke(Unit q) throws TransformationException {
	try {
	    InvokeExpr ie = getInvokeExpr(q);
	    if (ie != null && ie instanceof InstanceInvokeExpr)
		return true;
	    return false;
	} catch (RuntimeException ex) {
	    throw new TransformationException("An error occuerd in resolving the method in " + q.toString());
	}
    }

    public static boolean isInterfaceInvoke(Unit q) {
	InvokeExpr ie = getInvokeExpr(q);
	return ie != null && ie instanceof JInterfaceInvokeExpr;
    }

    public static boolean isInvoke(Unit q) {
	return (getInvokeExpr(q) != null);
    }

    static boolean isLibraryClass(SootClass classs) {
	return classs.isJavaLibraryClass() || classs.isPhantomClass() || classs.isLibraryClass();
    }

    static boolean isLibraryMethodCall(SootMethod method, boolean loadFromAPK, List<String> thirdPartyMethods,
	    String methodName) {
	return // method.isJavaLibraryMethod() ||
	method.isPhantom() // || isLibraryClass(method.getDeclaringClass())
		|| (loadFromAPK ? !method.hasActiveBody() && !method.getDeclaringClass().isInterface() : false)
		|| thirdPartyMethods.contains(methodName);
    }

    public static boolean isLoadInst1(JAssignStmt a) {
	Value right = a.getRightOpBox().getValue();
	if (right instanceof JArrayRef)
	    return true;
	return false;
    }

    public static boolean isLocalVariable(Value value) {
	return (value instanceof Local);
    }

    public static boolean isMonitorStmt(Unit unit) {
	return (unit instanceof soot.jimple.internal.JEnterMonitorStmt)
		|| (unit instanceof soot.jimple.internal.JExitMonitorStmt);
    }

    /*
     * public static String printUnit(Unit u){ SootMethod m =
     * SootUtilities.getMethod(u); Body b=m.retrieveActiveBody(); UnitPrinter up
     * =new NormalUnitPrinter(b); u.toString(up); return up.toString(); }
     */

    /*
     * public static Block getBasicBlock(Unit u){ Block b = unitToBlockMap.get(u);
     * if (b == null) { SootMethod m = getMethod(u); ICFG cfg = getCFG(m);
     * makeUnitToBlockMap(cfg); } return unitToBlockMap.get(u); }
     */

    public static boolean isMoveInst(JAssignStmt a) {
	Value left = a.getLeftOpBox().getValue();
	Value right = a.getRightOpBox().getValue();
	if (left instanceof Local && right instanceof Local)
	    return true;
	return false;
    }

    public static boolean isNewArrayStmt(JAssignStmt a) {
	Value right = a.getRightOpBox().getValue();
	if (right instanceof JNewArrayExpr)
	    return true;
	return false;
    }

    public static boolean isNewMultiArrayStmt(JAssignStmt a) {
	Value right = a.getRightOpBox().getValue();
	if (right instanceof JNewMultiArrayExpr)
	    return true;
	return false;
    }

    public static boolean isNewStmt(Unit unit) {

	return (unit instanceof JNewExpr) || ((unit instanceof JAssignStmt
		&& ((JAssignStmt) unit).getRightOpBox().getValue() instanceof JNewExpr));
    }

    public static boolean isNOP(Unit unit) {
	return unit instanceof JNopStmt;
    }

    public static boolean isNullConstant(Value value) {

	return value instanceof NullConstant;
    }

    public static boolean isParameterRef(Value value) {
	return (value instanceof ParameterRef);
    }

    public static boolean isPhiInst(JAssignStmt a) {
	Value right = a.getRightOpBox().getValue();
	if (right instanceof PhiExpr)
	    return true;
	return false;
    }

    public static boolean isPrimType(Type type) {
	return (type instanceof PrimType);
	// ||
	// (type instanceof ArrayType);
    }

    public static boolean isReturn(Unit unit) {
	return unit instanceof JReturnStmt;
    }

    public static boolean isSootVariable(Value value) {
	return value.toString().matches("(\\$)stack(\\d+)") || value.toString().matches("tmp(\\$)(\\d+)");
    }

    public static boolean isSpecialInvoke(Unit q) {
	InvokeExpr ie;
	if (q instanceof JInvokeStmt || q instanceof JAssignStmt)
	    ie = getInvokeExpr(q);
	else
	    ie = null;
	return ie != null && ie instanceof JSpecialInvokeExpr;
    }

    public static boolean isStaticField(Value value) {
	return value instanceof StaticFieldRef;
    }

    public static boolean isStaticGet(JAssignStmt a) {
	if (a.containsFieldRef()) {
	    FieldRef fr = a.getFieldRef();
	    ValueBox vb = a.getRightOpBox();
	    vb.getValue();
	    if (fr.getField().isStatic()) {
		if (vb.getValue().toString().equals(fr.toString())) {
		    return true;
		}
	    }
	}
	return false;
    }

    public static boolean isStaticInvoke(Unit q) {
	InvokeExpr ie = getInvokeExpr(q);
	if (ie != null && ie instanceof StaticInvokeExpr)
	    return true;
	return false;
    }

    public static boolean isStaticPut(JAssignStmt a) {
	if (a.containsFieldRef()) {
	    FieldRef fr = a.getFieldRef();
	    ValueBox vb = a.getLeftOpBox();
	    vb.getValue();
	    if (fr.getField().isStatic())
		if (vb.getValue().toString().equals(fr.toString()))
		    return true;
	}
	return false;
    }

    public static boolean isStoreInst(JAssignStmt a) {
	Value left = a.getLeftOpBox().getValue();
	if (left instanceof JArrayRef)
	    return true;
	return false;
    }

    public static boolean isSubtypeOf(RefLikeType i, RefLikeType j) {
	if (i instanceof ArrayType && j instanceof ArrayType) {
	    ArrayType ia = (ArrayType) i;
	    ArrayType ja = (ArrayType) j;
	    if (ia.numDimensions == ja.numDimensions) {
		Type basei = ia.baseType;
		Type basej = ja.baseType;
		if (basei == basej)
		    return true;
		else if (basei instanceof RefType && basej instanceof RefType) {
		    RefType baseir = (RefType) basei;
		    RefType basejr = (RefType) basej;
		    return isSubtypeOf(baseir.getSootClass(), basejr.getSootClass());
		}
	    } else if (ia.numDimensions > ja.numDimensions) {
		Type basej = ja.baseType;
		if (basej instanceof RefType) {
		    SootClass c = ((RefType) basej).getSootClass();
		    if (c.getName().equals("java.lang.Object"))
			return true;
		}
		return false;
	    } else {
		return false;
	    }
	} else if (i instanceof ArrayType && j instanceof RefType) {
	    RefType jr = (RefType) j;
	    String cName = jr.getSootClass().getName();
	    return cName.equals("java.lang.Object") || cName.equals("java.lang.Cloneable")
		    || cName.equals("java.io.Serializable");
	} else if (i instanceof RefType && j instanceof ArrayType) {
	    return false;
	} else if (i instanceof RefType && j instanceof RefType) {
	    return isSubtypeOf(((RefType) i).getSootClass(), ((RefType) j).getSootClass());
	}
	return false;
    }

    public static boolean isSubtypeOf(SootClass j, SootClass k) {
	if (k.getName().equals("java.lang.Object"))
	    return true;
	if (j.getName().equals(k.getName()))
	    return true;
	if (j.isInterface() && k.isInterface()) {
	    return h.isInterfaceSubinterfaceOf(j, k);
	} else if (j.isInterface() && !(k.isInterface()))
	    return false;
	else if (!(j.isInterface()) && k.isInterface()) {
	    Iterator<SootClass> inters = j.getInterfaces().iterator();
	    while (inters.hasNext()) {
		SootClass c = inters.next();
		if (c.getName().equals(k.getName()))
		    return true;
		else {
		    boolean temp = false;
		    temp = h.isInterfaceSubinterfaceOf(c, k);
		    if (temp)
			return temp;
		}
	    }
	    if (j.hasSuperclass())
		return isSubtypeOf(j.getSuperclass(), k);
	} else {
	    // Both j and k are concrete classes
	    if (!j.hasSuperclass())
		return false;
	    return SootUtilities.isSubtypeOf(j.getSuperclass(), k);
	}
	return false;
    }

    public static boolean isVirtualInvoke(Unit q) {
	assert (q instanceof JInvokeStmt || q instanceof JAssignStmt);
	InvokeExpr ie;
	if (q instanceof JInvokeStmt)
	    ie = ((JInvokeStmt) q).getInvokeExpr();
	else if (q instanceof JAssignStmt)
	    ie = ((InvokeExpr) (((JAssignStmt) q).getRightOpBox().getValue()));
	else
	    ie = null;
	return ie != null && ie instanceof JVirtualInvokeExpr;
    }

    public static boolean isVoidReturn(Unit unit) {
	return unit instanceof JReturnVoidStmt;
    }

    public static boolean varUsed(Unit unit, String local) {
	if (SootUtilities.isAssign(unit)) {
	    if (SootUtilities.isCastExpr(((JAssignStmt) unit).getRightOpBox().getValue()))
		return ((JCastExpr) (((JAssignStmt) unit).getRightOpBox().getValue())).getOp().toString().equals(local);
	    if (SootUtilities.isInvoke(unit)) {
		List<Value> args = ((InvokeExpr) (((JAssignStmt) unit).getRightOpBox().getValue())).getArgs();
		for (Value argName : args)
		    if (argName.toString().equals(local))
			return true;
	    } else
		return true;
	} else if (SootUtilities.isInvoke(unit)) {
	    List<Value> args = ((JInvokeStmt) unit).getInvokeExpr().getArgs();
	    for (Value argName : args)
		if (argName.toString().equals(local))
		    return true;
	} else if (SootUtilities.isConditional(unit))
	    return true;
	return false;
    }

    /*
     * public static Map<String,List<Pair<String,String>>>
     * parseVisibilityAnnotationTag(VisibilityAnnotationTag v){
     * Map<String,List<Pair<String,String>>> result = new
     * HashMap<String,List<Pair<String,String>>>(); List<AnnotationTag> aTags =
     * v.getAnnotations(); for(AnnotationTag a : aTags){ String annotationName =
     * a.getType(); List<Pair<String,String>> elems = null;
     * if(!result.containsKey(annotationName)){ elems = new
     * ArrayList<Pair<String,String>>(); result.put(annotationName, elems); }else
     * elems = result.get(annotationName); for(AnnotationElem ae : a.getElems()){
     * if(ae.getKind() == 's'){ AnnotationStringElem ase = (AnnotationStringElem)ae;
     * Pair<String,String> keyValue = new
     * Pair<String,String>(ase.getName(),ase.getValue()); elems.add(keyValue); } } }
     * }
     */
}
