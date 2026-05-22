package se.lnu.prosses.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.xmlpull.v1.XmlPullParserException;

import se.lnu.prosses.utils.Utils;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.infoflow.android.SetupApplication;
import soot.options.Options;
//import de.ecspride.util.SourcesSinks;

/**
 * ������������������������ ��������soot��������������������������������������
 *
 * @author wang
 */
public class SootInit {
    // public final static String ANDROID_JAR_PATH = "lib/android.jar";
    static ArrayList<String> sourceMethodNames = new ArrayList<String>();
    static ArrayList<String> sinkMethodNames = new ArrayList<String>();

    /*
     * protected static void enableSpark() {// ��������spark������������
     * PhaseOptions.v().setPhaseOption("cg.spark", "enabled:true"); HashMap<String,
     * String> opt = new HashMap<>();// TODO �������������������� opt.put("verbose",
     * "true"); opt.put("propagator", "worklist");
     * opt.put("simple-edges-bidirectional", "false"); opt.put("on-fly-cg", "true");
     * opt.put("set-impl", "double"); opt.put("double-set-old", "hybrid");
     * opt.put("double-set-new", "hybrid"); opt.put("pre_jimplify", "true");
     * opt.put("apponly", "true"); SparkTransformer.v().transform("", opt); }
     */

    private static void extendSootClassPath(String[] requiredClassesPaths) {
	// if the array is empty, finish
	if (requiredClassesPaths == null)
	    return;

	// Convert the paths-array to one String (Paths separated by ';')
	StringBuilder stringBuilder = new StringBuilder();
	for (String path : requiredClassesPaths) {
	    if (path != null && !path.isEmpty()) {
		path = path.replaceAll("%20", " ");
		if (new File(path).exists()) {
		    stringBuilder.append(path + File.pathSeparator);
		}
	    }
	}

	// Add the string to soot-class-path
	if (stringBuilder.length() > 0) {
	    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
	    Scene.v().extendSootClassPath(stringBuilder.toString());
	}
    }

    /*
     * public static void extractMethodNamesFromResultSet(InfoflowResults res) { if
     * (res != null && res.size() > 0) { Iterator<ResultSinkInfo> sinkIterator =
     * res.getResults().keySet().iterator(); while (sinkIterator.hasNext()) {
     * ResultSinkInfo resultSink = sinkIterator.next(); if
     * (resultSink.getDefinition() instanceof MethodSourceSinkDefinition) {
     * MethodSourceSinkDefinition methodSink = (MethodSourceSinkDefinition)
     * resultSink.getDefinition();
     * sinkMethodNames.add(methodSink.getMethod().getMethodName()); } } }
     * 
     * return; }
     */

    /*
     * public static void
     * extractSourceMethodNamesFromSourcesSinksFile(SetupApplication
     * setupApplication) { Iterator<SourceSinkDefinition> sinkIterator =
     * setupApplication.getSources().iterator(); while (sinkIterator.hasNext()) {
     * SourceSinkDefinition resultSink = sinkIterator.next(); if (resultSink
     * instanceof MethodSourceSinkDefinition) { MethodSourceSinkDefinition
     * methodSink = (MethodSourceSinkDefinition) resultSink;
     * sourceMethodNames.add(methodSink.getMethod().getClassName() +
     * methodSink.getMethod().getMethodName()); } } return; }
     */

    public static void initSootForApk(String[] requiredClassesPaths2, String apk, String sourcesSinks, String callbacks)
	    throws IOException, XmlPullParserException {
	SetupApplication setupApp = new SetupApplication(requiredClassesPaths2[0], apk);
	setupApp.setCallbackFile(callbacks);

	try {
	    setupApp.runInfoflow(sourcesSinks);
	    // setupApp.calculateSourcesSinksEntrypoints();
	    // setupApp.constructCallgraph();// .runInfoflow(sourcesSinks);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	ArrayList<String> included_packages = new ArrayList<String>();
	included_packages.add("java");

	soot.G.reset();

	Options.v().setPhaseOption("jb.lns", "enabled:true");

	Options.v();
	Utils.log(SootInit.class, Options.getDeclaredOptionsForPhase("jap.npc"));
	Options.v();
	Utils.log(SootInit.class, Options.getDeclaredOptionsForPhase("jap.abc"));

	// Options.v().set_check_init_throw_analysis(setting);
	// Options.v().setPhaseOption("jap.npc","only-array-ref:true");
	Options.v().setPhaseOption("jap.npc", "enabled:true");
	Options.v().setPhaseOption("jap.abc", "enabled:true");
	Options.v().setPhaseOption("jap.abc", "with-all:true");
	Options.v().setPhaseOption("jb", "use-original-names:true");
	Options.v().setPhaseOption("jb", "preserve-source-annotations:true");
	Options.v().setPhaseOption("jj", "use-original-names:true");
	Options.v().setPhaseOption("jj", "lp:true");
	Options.v().setPhaseOption("wjtp", "rdc:true");
	Options.v().setPhaseOption("jop", "enabled:true");
	Options.v().setPhaseOption("jap", "che:true");

	// Options.v().setPhaseOption("jap.abc","with-arrayref:true");

	Options.v().show_exception_dests();
	// Options.v().set_throw_analysis(soot.options.Options.throw_analysis_unit);

	Options.v().set_src_prec(Options.src_prec_apk);
	Options.v().set_process_dir(Collections.singletonList(apk));
	Options.v().set_force_android_jar(requiredClassesPaths2[0]);
	Options.v().set_whole_program(true);
	Options.v().set_allow_phantom_refs(true);
	//Options.v().set_no_bodies_for_excluded(true);	//Unsure whether to include this - doesn't seem to make a difference
	Options.v().set_output_format(Options.output_format_none);

	Options.v().set_include_all(true);
	Options.v().set_app(true);
	Utils.log(SootInit.class, "Current Classpath:" + Scene.v().getSootClassPath());

	try {
	    SootMethod entryPoint = setupApp.getDummyMainMethod();
	    Options.v().set_main_class(entryPoint.getSignature());
	    Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
	    Scene.v().extendSootClassPath(requiredClassesPaths2[0]);
	    Utils.log(SootInit.class, Scene.v().getSootClassPath());
	    Scene.v().loadDynamicClasses();
	    Scene.v().loadNecessaryClasses();

	} catch (Exception exp) {
	    Utils.logErr(SootInit.class, "FDROID Could not load the application " + apk);
	    Utils.logErr(SootInit.class, exp.toString());
	}
    }

    public static void initSootForJavaClasses(String javaDir, String[] requiredClassesPaths,
	    boolean enableSootOptimizations, boolean enableJimpleOptimizationPack, int appType) {
	Utils.log(SootInit.class, "Loading Soot with main class folder " + javaDir);
	soot.G.reset();

	// setSootOptions(Options.v());
	 //Options.v().set_no_bodies_for_excluded(true);
	// Options.v().setPhaseOption("jb", "pp:true");
	Options.v().setPhaseOption("jb", "use-original-names:true");
	Options.v().setPhaseOption("jb.ls", "enabled:false");
	Options.v().setPhaseOption("jb.a", "enabled:true");
	Options.v().setPhaseOption("jb.a", "only-stack-locals:false");
	Options.v().setPhaseOption("jb.ule", "enabled:true");
	Options.v().setPhaseOption("jb.ulp", "enabled:true");
	Options.v().setPhaseOption("jb.ulp", "unsplit-original-locals:false");
	Options.v().setPhaseOption("jb.cp", "enabled:true");
	Options.v().setPhaseOption("jb.cp", "only-regular-locals:false");
	Options.v().setPhaseOption("jb.cp", "only-stack-locals:false");
	Options.v().setPhaseOption("jb.dae", "enabled:true");
	Options.v().setPhaseOption("jb.cp-ule", "enabled:true");
	Options.v().setPhaseOption("jb.lp", "enabled:true");
	Options.v().setPhaseOption("jb.lp", "unsplit-original-locals:false");
	Options.v().setPhaseOption("wjtp", "rdc:true");
	if (enableJimpleOptimizationPack)
	    Options.v().setPhaseOption("wjop", "enabled:true");

	Options.v().setPhaseOption("jop", "enabled:true"); // leave the defaults...
	if (enableSootOptimizations) {
	    Options.v().setPhaseOption("jop.cse", "enabled:true");
	    Options.v().setPhaseOption("jop.lcm", "enabled:true");
	    Options.v().setPhaseOption("jop.cp", "enabled:true");
	    Options.v().setPhaseOption("jop.cp", "only-regular-locals:false");
	    Options.v().setPhaseOption("jop.cp", "only-stack-locals:false");
	    Options.v().setPhaseOption("jop.cbf", "enabled:true");
	    Options.v().setPhaseOption("jop.dae", "enabled:true");
	    Options.v().setPhaseOption("jop.nce", "enabled:true");
	    Options.v().setPhaseOption("jop.ule", "enabled:true");

	}

	// Options.v().set_check_init_throw_analysis(setting);
	Options.v().setPhaseOption("jap.npc", "enabled:true");
	// Options.v().setPhaseOption("jap.npc","only-array-ref:true");
	Options.v().setPhaseOption("jap.abc", "enabled:true");
	Options.v().setPhaseOption("jap.abc", "with-all:true");
	// Options.v().setPhaseOption("jap.abc","with-arrayref:true");

	Options.v().set_allow_phantom_refs(true);
	Options.v().set_print_tags_in_output(true);
	Options.v().print_tags_in_output();
	Options.v().set_xml_attributes(true);
	Options.v().set_keep_line_number(true);
	Options.v().set_validate(true);
	Options.v().set_src_prec(appType);
	if (appType == Options.src_prec_apk) {
	    Options.v().set_force_android_jar(requiredClassesPaths[0]);
	    Options.v().set_android_jars(requiredClassesPaths[0]);
	}

	Options.v().set_process_dir(Collections.singletonList(javaDir));
	Options.v().set_whole_program(true);
	// enableSpark();
	// Options.v().setPhaseOption("cg.spark verbose:true", "on");
	// Utils.log(SootInit.class, "Soot class path: " +
	// Scene.v().getSootClassPath());
	Scene.v().getSootClassPath();
	// Scene.v().extendSootClassPath(requiredClassesPaths);
	extendSootClassPath(requiredClassesPaths);
	// Scene.v().setSootClassPath(javaDir);

	Scene.v().loadNecessaryClasses();
	Scene.v().loadDynamicClasses();
    }

    /*
     * public static void setSootOptions(Options options, List<String> libraries) {
     * libraries.add("java.lang.*"); libraries.add("java.util.*");
     * libraries.add("java.io.*"); libraries.add("soot.*");
     * libraries.add("securibench.*"); libraries.add("android.*");
     * options.set_exclude(libraries);
     * options.set_output_format(Options.output_format_none); }
     */

}
