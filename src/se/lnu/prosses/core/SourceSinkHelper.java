package se.lnu.prosses.core;

import java.util.HashMap;
import java.util.List;

import se.lnu.prosses.configs.Constants;
import se.lnu.prosses.utils.Utils;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.ClassConstant;

public class SourceSinkHelper {

    public HashMap<String, List<String[]>> highSources = new HashMap<String, List<String[]>>();
    public HashMap<String, List<String[]>> lowSources = new HashMap<String, List<String[]>>();
    public HashMap<String, List<String[]>> highSinks = new HashMap<String, List<String[]>>();
    public HashMap<String, List<String[]>> lowSinks = new HashMap<String, List<String[]>>();
    // public List<String> sinkMethods = new ArrayList<String>();
    // public List<String> sourceMethods = new ArrayList<String>();

    public SourceSinkHelper() {
	// TODO Auto-generated constructor stub
    }

    boolean isSink(SootMethod method, boolean isHigh) throws TransformationException {
	if (// !isHigh && sinkMethods.contains(new
	    // JimpleProjectHelper().getMethodUniqueNameV2(method)) ||
	this.isSourceSinkMethod(method, isHigh, false) || returnsSourceSink(method, isHigh, false))
	    return true;
	for (int index = 0; index < method.getParameterCount(); index++)
	    if (hasSourceSinkParameter(method, index, isHigh, false))
		return true;

	return false;
    }

    /*
     * boolean isSource(SootField field, boolean isHigh) { if
     * (getSources(isHigh).get(Utils.XMLField) != null) for (String[] source :
     * getSources(isHigh).get(Utils.XMLField)) if
     * (field.getDeclaringClass().getName().equals(ClassConstant.v(source[0]).
     * toSootType().toString()) &&
     * field.getDeclaringClass().declaresFieldByName(source[1]) &&
     * field.getName().equals(source[1])) return true; return false; }
     */

    boolean isSinkOrSourceField(SootField field, boolean isHigh, boolean isSource) {
	if (getSourcesOrSinks(isHigh, isSource).get(Utils.XMLField) != null)
	    for (String[] source : getSourcesOrSinks(isHigh, isSource).get(Utils.XMLField))
		if (field.getDeclaringClass().getName().equals(ClassConstant.v(source[0]).toSootType().toString())
			&& field.getDeclaringClass().declaresFieldByName(source[1])
			&& field.getName().equals(source[1]))
		    return true;
	return false;
    }

    boolean isSourceMethod(SootMethod method, boolean isHigh) throws TransformationException {
	if (returnsSourceSink(method, isHigh, true) || isSourceSinkMethod(method, isHigh, true))
	    return true;

	for (int index = 0; index < method.getParameterCount(); index++)
	    if (hasSourceSinkParameter(method, index, isHigh, false))
		return true;
	return false;
    }

    private boolean isSourceSinkMethod(SootMethod method, boolean isHigh, boolean isSource) {
	if (getSourcesOrSinks(isHigh, isSource).get(Utils.XMlMethod) != null)
	    for (String[] source : getSourcesOrSinks(isHigh, isSource).get(Utils.XMlMethod)) {
		if (method.getDeclaringClass().getName().equals(ClassConstant.v(source[0]).toSootType().toString())
			&& method.getName().equals(source[1].split("\\(")[0]))
		    return true;
	    }
	return false;
    }

    private HashMap<String, List<String[]>> getSources(boolean isHigh) {
	return isHigh ? highSources : lowSources;
    }

    private HashMap<String, List<String[]>> getSourcesOrSinks(boolean isHigh, boolean isSource) {
	return (isSource ? (isHigh ? highSources : lowSources) : (isHigh ? highSinks : lowSinks));
    }

    boolean hasSourceSinkParameter(SootMethod method, int parameterIndex, boolean isHigh, boolean isSource)
	    throws TransformationException {
	try {
	    if (getSourcesOrSinks(isHigh, isSource).get(Utils.XMLParameter) != null)
		for (String[] source : getSourcesOrSinks(isHigh, isSource).get(Utils.XMLParameter))
		    if (method.getDeclaringClass().getName().equals(ClassConstant.v(source[0]).toSootType().toString())
			    && method.getName().equals(source[1].split("\\(")[0])
			    && Integer.parseInt(source[2]) == parameterIndex + 1)
			return true;
	} catch (RuntimeException e) {
	    throw new TransformationException("The source/sink xml file is not well-formatted");
	}
	return false;

    }

    /*
     * boolean hasSinkParameter(SootMethod method, int parameterIndex, boolean
     * isHigh) { //System.out.println(method.getName()); if
     * (sinkMethods.contains(new
     * JimpleProjectHelper().getMethodUniqueNameV2(method))) return true; if
     * (getSinks(isHigh).get(Utils.XMLParameter) != null) for (String[] source :
     * getSinks(isHigh).get(Utils.XMLParameter)) if
     * (method.getDeclaringClass().getName().equals(ClassConstant.v(source[0]).
     * toSootType().toString()) &&
     * method.getName().equals(source[1].split("\\(")[0]) &&
     * Integer.parseInt(source[2]) == parameterIndex + 1) return true;
     * 
     * return false; }
     */

    boolean returnsSourceSink(SootMethod method, boolean isHigh, boolean isSource) {
	if (getSourcesOrSinks(isHigh, isSource).get(Utils.XMlReturnValue) != null)
	    for (String[] source : getSourcesOrSinks(isHigh, isSource).get(Utils.XMlReturnValue))
		if (method.getDeclaringClass().getName().equals(ClassConstant.v(source[0]).toSootType().toString())
			&& method.getName().equals(source[1].split("\\(")[0]))
		    return true;
	return false;

    }

    boolean hasSourceSinkBase(SootMethod method, boolean isHigh, boolean isSource) {
	if (getSourcesOrSinks(isHigh, isSource).get(Utils.XMLReference) != null)
	    for (String[] source : getSourcesOrSinks(isHigh, isSource).get(Utils.XMLReference))
		if (method.getDeclaringClass().getName().equals(ClassConstant.v(source[0]).toSootType().toString())
			&& method.getName().equals(source[1].split("\\(")[0]))
		    return true;
	return false;

    }

    public boolean hasLowSinkBase(SootMethod method) {
	return hasSourceSinkBase(method, false, false);
    }

    public boolean hasHighSinkBase(SootMethod method) {
	return hasSourceSinkBase(method, true, false);
    }

    public boolean hasLowSourceBase(SootMethod method) {
	return hasSourceSinkBase(method, false, true);
    }

    public boolean hasHighSourceBase(SootMethod method) {
	return hasSourceSinkBase(method, true, true);
    }

    public boolean hasLowSinkParameter(SootMethod method, int index) throws TransformationException {
	return this.hasSourceSinkParameter(method, index, false, false);
    }

    public boolean hasHighSourceParameter(SootMethod method, int index) throws TransformationException {
	return this.hasSourceSinkParameter(method, index, true, true);
    }

    public boolean hasHighSinkParameter(SootMethod method, int index) throws TransformationException {
	return this.hasSourceSinkParameter(method, index, true, false);
    }

    public boolean hasLowSourceParameter(SootMethod method, int index) throws TransformationException {
	return this.hasSourceSinkParameter(method, index, false, true);
    }

    public boolean isHighSource(SootField field) {
	return this.isSinkOrSourceField(field, true, true);
    }

    public boolean isLowSource(SootField field) {
	return this.isSinkOrSourceField(field, false, true);
    }

    public boolean isLowSink(SootField field) {
	return this.isSinkOrSourceField(field, false, false);
    }

    public boolean isHighSink(SootField field) {
	return this.isSinkOrSourceField(field, true, false);
    }

    public boolean returnsHighSource(SootMethod method) {
	return this.returnsSourceSink(method, true, true);
    }

    public boolean returnsLowSource(SootMethod method) {
	return returnsSourceSink(method, false, true);
    }

    public boolean returnsHighSink(SootMethod method) {
	return this.returnsSourceSink(method, true, false);
    }

    public boolean returnsLowSink(SootMethod method) {
	return returnsSourceSink(method, false, false);
    }

    public boolean isHighSource(SootMethod method) throws TransformationException {
	return isSourceSinkMethod(method, true, true);
    }

    public boolean isLowSource(SootMethod method) throws TransformationException {
	return isSourceSinkMethod(method, false, true);
    }

    public boolean isLowSink(SootMethod method) throws TransformationException {
	return isSourceSinkMethod(method, false, false);
    }

    public boolean isHighSink(SootMethod method) throws TransformationException {
	return isSourceSinkMethod(method, true, false);
    }

    public static final String dummyLowSourceMethodName(soot.Type t) {
	return Constants.dummyClass + "." + Constants.lowSourceMethodName + t.toString().replace('.', '_');
    }

    public static final String dummyHighSourceMethodName(soot.Type t) {
	return Constants.dummyClass + "." + Constants.highSourceMethodName
		+ t.toString().replace('.', '_').replace("[]", "Arr");
    }

    public static final String dummyHighSourceMethodNameForSig(soot.Type t) {
	return Constants.dummyClass + ":" + Constants.highSourceMethodName
		+ t.toString().replace('.', '_').replace("[]", "Arr");
    }

}
