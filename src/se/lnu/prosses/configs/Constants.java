package se.lnu.prosses.configs;

import java.text.DecimalFormat;
import java.text.Format;

import soot.RefLikeType;
import soot.RefType;

public class Constants {
    public static final String NIL = "Nil";
    public static final String dummyClass = "dummyClass";
    public static final String lowSinkMethodName = "lowSink";
    public static final String highSourceMethodName = "highSourceMut";
    public static final String highSinkMethodName = "highSink";
    public static final String lowSourceMethodName = "lowSource";
    public static final String This = "this";
    public static final RefLikeType throwableType = RefType.v("java.lang.Throwable");
    public static final Object OBJECT = "java.lang.Object";
    public static final long serialVersionUID = 1L;
    public static final String statsFilePrefix = "results";
    public static final String class_statsFileExtension = ".class_stats";
    public static final String meth_statsFileExtension = ".meth_stats";
    public static final String proc_statsFileExtension = ".proc_stats";

    public static final String secsumsFileExtension = ".secsums";

    public static final String analysis_proc_statsFileExtension = "_analysis.proc_stats";
    public static final String secsumFileExtension = ".secsum";

    public static final String syrsVersionFileExtension = ".syrs-version";

    public static final String secstubsFileExtension = ".secstubs";
    public static final String GroundTruthFile = "ground-truth.txt";
    public static final String errLogFile = "generation-errors.log";
    public static final String methExtension = "meth";
    public static final String srcsExtension = "srcs";
    // public static final String symmariesPath = " ulimit -d $(( 8 * 1024 * 1024 *
    // 1024 )); OCAMLRUNPARAM=\"h=3G\" syrs.opt "; //
    // System.getProperty("user.home") +
    // "/.opam/ocaml-base-compiler.4.07.1/bin/scgs.opt";
    public static final String symmariesEnv = "OCAMLRUNPARAM=\"h=3G\"";
    // public static final String symmariesPath = " syrs.opt ";

    // public static final String timeCmd = " /usr/bin/time -f '%e; %S; %U; %D; %K;
    // %M; %x' ";
    public static final String timeCmd = "/usr/bin/time -f '%e; %S; %U; %M; %x'";
    public static final String[] timeFmtDescr = new String[] { // all times are in seconds, all mem in KiB
	    "Wall clock time", // e
	    "System time", // S
	    "User time", // U
	    // "Average data mem", // D (unshared data segment)
	    // "Average total memory", // K (data + stack + text/code)
	    "Max memory", // M
	    "Exit status" // x
    };
    public static final DecimalFormat secFmt = new DecimalFormat("0.00");
    public static final DecimalFormat memFmt = new DecimalFormat("0");
    public static final Format statusFmt = new DecimalFormat("0");
    public static final Format[] timeFmt = new Format[] { secFmt, // e
	    secFmt, // S
	    secFmt, // u
	    memFmt, // M
	    statusFmt // x
    };
    // public static final String[] timeFmt = new String[]
    // {
    // "%.2f", // e
    // "%.2f", // S
    // "%.2f", // u
    // "%.0f", // M
    // "%.0f" // x
    // };
    // public static final Function<Float, String> floatAsSeconds =
    // new Function<> {
    // };
    public static final String elapsed_timeFileExtension = ".elapsed-time";

};
