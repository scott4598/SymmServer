package se.lnu.prosses.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.lnu.prosses.configs.Configuration;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

/**
 * Scan an rt.jar (Java 8 style) and dump all methods + Jimple bodies for
 * selected API packages.
 *
 * Usage: java com.example.sootdump.RtJarJimplePrinter <path-to-rt.jar> [--all]
 * [--pkg=p1,p2,...] [--include-inner]
 *
 * Examples: java ... RtJarJimplePrinter $JAVA_HOME/jre/lib/rt.jar java ...
 * RtJarJimplePrinter rt.jar --all > out.jimple java ... RtJarJimplePrinter
 * rt.jar --pkg=java.lang,java.util
 */
public class SootSetup {

    /*
     * public static void main(String[] args) throws IOException { if (args.length <
     * 1) { System.err.println("Usage: java " + SootSetup.class.getName() +
     * " <path-to-rt.jar> [--all] [--pkg=p1,p2,...] [--include-inner]");
     * System.exit(1); }
     * 
     * String rtJarPath = args[0]; boolean includeAll = false; boolean includeInner
     * = false; List<String> pkgPrefixes = new ArrayList<>();
     * 
     * // defaults = standard Java SE API top-levels pkgPrefixes.add("java."); //
     * pkgPrefixes.add("javax."); // pkgPrefixes.add("org.w3c."); //
     * pkgPrefixes.add("org.xml."); // pkgPrefixes.add("org.ietf."); //
     * pkgPrefixes.add("org.omg.");
     * 
     * // parse flags for (int i = 1; i < args.length; i++) { String a = args[i]; if
     * ("--all".equals(a)) { includeAll = true; } else if (a.startsWith("--pkg=")) {
     * pkgPrefixes.clear(); String csv = a.substring("--pkg=".length()).trim(); if
     * (!csv.isEmpty()) { for (String p : csv.split(",")) { p = p.trim(); if
     * (!p.isEmpty()) pkgPrefixes.add(p.endsWith(".") ? p : p + "."); } } } else if
     * ("--include-inner".equals(a)) { includeInner = true; } else {
     * System.err.println("[warn] Unknown arg: " + a); } }
     * 
     * if (includeAll) { pkgPrefixes.clear(); // we'll include everything below }
     * 
     * System.out.println("[info] Runtime JAR: " + rtJarPath);
     * System.out.println("[info] Include all packages: " + includeAll);
     * System.out.println("[info] Include inner classes: " + includeInner); if
     * (!includeAll) { System.out.println("[info] Package prefixes: " +
     * pkgPrefixes); }
     * 
     * // Scan jar -> candidate classes List<String> allClasses =
     * getClassesFromJar(rtJarPath, pkgPrefixes, includeAll, includeInner);
     * System.out.println("[info] Classes to process: " + allClasses.size());
     * 
     * // Configure Soot setupSoot(rtJarPath);
     * 
     * // Actually load & dump dumpClasses(allClasses);
     * 
     * }
     */

    /** Configure Soot to use the provided rt.jar as the classpath root. */
    /*
     * private static void setupSoot(String rtJarPath) { G.reset();
     * 
     * // Basic options Options.v().set_prepend_classpath(true);
     * Options.v().set_allow_phantom_refs(true); // rt.jar partial loads produce
     * lots of phantoms Options.v().set_whole_program(true);
     * Options.v().set_keep_line_number(true); Options.v().set_keep_offset(true);
     * 
     * // Tell Soot where the classes live String cp = rtJarPath +
     * File.pathSeparator + "."; // include CWD so we can find our own classes if
     * needed Options.v().set_soot_classpath(cp); Scene.v().setSootClassPath(cp);
     * 
     * // We are analyzing compiled .class in jars
     * Options.v().set_src_prec(Options.src_prec_only_class);
     * 
     * // Output to Jimple (we are printing ourselves; this controls file output if
     * // used) Options.v().set_output_format(Options.output_format_jimple); }
     */

    // ------------------------------------------------------------------
    // SOOT SETUP
    // ------------------------------------------------------------------
    public static void setupSoot(Configuration cfg) {
	G.reset();
	Options opt = Options.v();

	if (cfg.isAPK) {
	    opt.set_src_prec(Options.src_prec_apk);
	    opt.set_process_dir(Collections.singletonList(cfg.inputArtifactPath));
	    if (!cfg.androidPlatforms.isEmpty()) {
		opt.set_android_jars(cfg.androidPlatforms); // directory that contains platform android.jar per API
		// level
	    }
	    opt.set_force_android_jar(
		    cfg.androidPlatforms + File.separator + "android-30" + File.separator + "android.jar"); // safe
	    // default;
	    // override
	    // if needed
	    opt.set_process_multiple_dex(true);
	    // Recommended for Android
	    opt.set_allow_phantom_refs(true);
	    opt.set_whole_program(true);
	} else {
	    // JAR / pure Java
	    opt.set_prepend_classpath(true);
	    opt.set_soot_classpath(buildClasspath(cfg));
	    opt.set_process_dir(Collections.singletonList(cfg.inputArtifactPath));
	    opt.set_src_prec(Options.src_prec_class);
	    opt.set_allow_phantom_refs(true);
	    opt.set_keep_line_number(true);
	    opt.set_whole_program(true);
	}

	// Output: we do not want Soot to write classfiles; just keep Jimple in memory.
	opt.set_output_format(Options.output_format_none);

	if (cfg.enableSootOptimizations) {
	    opt.setPhaseOption("wjop", "enabled:true");
	    opt.setPhaseOption("wjtp", "enabled:true");
	}
	if (cfg.enableJimpleOptimizationPack) {
	    opt.setPhaseOption("jop", "enabled:true");
	}

	// Load only application classes? We'll mark everything as application to get
	// bodies.
	opt.set_app(true);

	// Load basic classes
	Scene.v().addBasicClass("java.lang.Object", SootClass.BODIES);
	Scene.v().addBasicClass("java.lang.String", SootClass.BODIES);
    }

    public static void ensureRuntimeOnClasspath(Configuration cfg) {
	// Skip if APK: Android front-end will wire platform jars.
	if (cfg.isAPK) {
	    return;
	}

	final soot.Scene scene = soot.Scene.v();
	String cp = scene.getSootClassPath();

	// Quick heuristic: try to load java.lang.Object. If found, we’re good.
	boolean runtimePresent;
	try {
	    runtimePresent = scene.containsClass("java.lang.Object");
	} catch (Exception e) {
	    runtimePresent = false;
	}
	if (runtimePresent) {
	    System.out.println("[debug] Java runtime already present on Soot classpath.");
	    return;
	}
	// Try common Java locations.
	String javaHome = System.getProperty("java.home");
	List<String> candidates = new ArrayList<>();
	if (javaHome != null) {
	    // JDK 8 layout.
	    candidates.add(javaHome + File.separator + "lib" + File.separator + "rt.jar");
	    // Some distros put classes in 'jre/lib/rt.jar' when java.home is JDK root.
	    candidates.add(javaHome + File.separator + "jre" + File.separator + "lib" + File.separator + "rt.jar");
	    // Fallback: jmods/java.base.jmod (JDK 9+). Soot expects a jar; some users unzip
	    // + jar it.
	    candidates.add(javaHome + File.separator + "jmods" + File.separator + "java.base.jmod");
	}

	String found = "";
	for (String cand : candidates) {
	    File f = new File(cand);
	    if (f.isFile()) {
		found = f.getAbsolutePath();
		break;
	    }
	}

	if (!found.isEmpty()) {
	    String newCp = cp + File.pathSeparator + found;
	    scene.setSootClassPath(newCp);
	    System.out.println("[info] Appended Java runtime to Soot classpath: " + found);
	} else {
	    System.err.println("[warn] Could not auto-locate a Java runtime jar (rt.jar or similar). "
		    + "You may need to pass one via -cp.");
	    System.err.println("[warn] Current Soot CP: " + cp);
	}
    }

    private static String buildClasspath(Configuration cfg) {
	// Compose: input artifact dir + extra cp entries + current Soot cp
	List<String> entries = new ArrayList<>();
	// existing Soot cp
	String sootCp = Scene.v().defaultClassPath();
	if (sootCp != null && !sootCp.isEmpty())
	    entries.add(sootCp);
	// input artifact
	entries.add(cfg.inputArtifactPath);
	// extras
	if (cfg.extraClassPaths != null) {
	    for (String e : cfg.extraClassPaths) {
		if (e != null && !e.isEmpty())
		    entries.add(e);
	    }
	}
	return String.join(File.pathSeparator, entries);
    }

}