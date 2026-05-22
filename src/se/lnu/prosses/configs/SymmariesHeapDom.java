package se.lnu.prosses.configs;

import se.lnu.prosses.frontend.JSymCompiler;
import se.lnu.prosses.utils.Utils;

public enum SymmariesHeapDom {
	DEEPALIAS, SHALLOWALIAS, DUMBALIAS, CONNECT, SHARE, FIELDSHARE;

	public String toString () {
		switch (this) {
		default:
		case DEEPALIAS: return "deepalias";
		case SHALLOWALIAS: return "shallowalias";
		case DUMBALIAS: return "dumbalias";
		case CONNECT: return "connect";
		case SHARE: return "share";
		case FIELDSHARE: return "fieldshare";
		}
	}

	public static final class UnknownHeapDomError extends Error {
		private UnknownHeapDomError (String s) { super (s); }
	}

	public static SymmariesHeapDom ofString (String s) throws UnknownHeapDomError {
		switch (s) {
		case "deep": case "deepalias": return DEEPALIAS;
		case "shallow": case "shallowalias": return SHALLOWALIAS;
		case "dumb": case "dumbalias": return DUMBALIAS;
		case "connect": return CONNECT;
		case "share": return SHARE;
		case "fieldshare": case "ashare": return FIELDSHARE;
		default: throw new UnknownHeapDomError ("Unknown Heap Domain: " + s);
		}
	}

	public String getTransFlags () {
		return "heapdom=" + this.toString ();
	}

	public CharSequence getvarHeapDomain() {
		// TODO Auto-generated method stub
		//return "heapdom=${heapDomain}";
		Utils.log(JSymCompiler.class, "heapDom: " + this.toString());
		return "heapdom=" + this.toString(); //Sets the text to write the heapdomain flag in the .command file
	}
}
