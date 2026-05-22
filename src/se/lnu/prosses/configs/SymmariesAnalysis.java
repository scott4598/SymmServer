package se.lnu.prosses.configs;

public enum SymmariesAnalysis {
	HEAP_ONLY, TAINT, EXPLICIT_CONF, IMPLICIT_CONF;

	public String toString () {
		switch (this) {
		default:
		case HEAP_ONLY: return "HeapOnly";
		case TAINT: return "TaintExpr";
		case EXPLICIT_CONF: return "ExplicitConf";
		case IMPLICIT_CONF: return "ImplicitConf";
		}
	}

	public static final class UnknownAnalysisError extends Error {
		private UnknownAnalysisError (String s) { super (s); }
	}

	public static SymmariesAnalysis ofString (String s) throws UnknownAnalysisError {
		switch (s) {
		case "HeapOnly": case "Heap": case "HA": return HEAP_ONLY;
		case "TaintExpr": case "Taint": case "TA": return TAINT;
		case "ExplicitConf": case "Explicit": case "EFA": return EXPLICIT_CONF;
		case "ImplicitConf": case "Implicit": case "IFA": return IMPLICIT_CONF;
		default: throw new UnknownAnalysisError ("Unknown Symmaries Analysis: " + s);
		}
	}

	public String getTransFlags () {
		switch (this) {
		default:
		case HEAP_ONLY: return "-taints,-levels";
		case TAINT: return "+enforce_integrity-levels";
		case EXPLICIT_CONF: return "-taints,levels=explicit";
		case IMPLICIT_CONF: return "-taints,levels=all";
		}
	}

	public boolean taintCheckingEnabled () {
		return this == TAINT;
	}

	public boolean confidentialityCheckingEnabled () {
		return this == EXPLICIT_CONF || this == IMPLICIT_CONF;
	}

	public boolean implicitConfEnabeled () {
		return this == IMPLICIT_CONF;
	}

	public boolean explicitConfEnabeled () {
		return this == EXPLICIT_CONF || this == IMPLICIT_CONF;
	}

	public boolean isAlwaysSafe() {
		return this == HEAP_ONLY;
	}
}
