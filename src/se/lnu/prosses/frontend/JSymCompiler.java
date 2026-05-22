package se.lnu.prosses.frontend;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FilenameUtils;

import se.lnu.prosses.configs.Configuration;
import se.lnu.prosses.configs.Constants;
import se.lnu.prosses.configs.SymmariesAnalysis;
import se.lnu.prosses.configs.SymmariesHeapDom;
import se.lnu.prosses.configs.TypeHandlingConfig;
import se.lnu.prosses.core.ProjectHelper;
import se.lnu.prosses.core.SootSetup;
import se.lnu.prosses.utils.Utils;

//Added to enable the webhook with Sonatype Nexus
//import com.sun.net.httpserver.HttpServer;
//import com.sun.net.httpserver.HttpHandler;
//import com.sun.net.httpserver.HttpExchange;
//import java.io.InputStream;
//import java.net.InetSocketAddress;
//import java.util.Scanner;
//import java.net.URL;
//import java.nio.file.StandardCopyOption;

public class JSymCompiler {
    // ------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------
    //private static final String DEFAULT_SYRS_PATH = "syrs_default/";
	private static final String DEFAULT_SYRS_PATH = "syrs";
    private static final String BASH_HEADER = "#!/bin/bash\n";
    private static final String VARIANT_PLACEHOLDER = "${variant}";

    //private static ProjectHelper project; - changed to public - removed static for possible later concurrency bits
	public ProjectHelper project;

    // ------------------------------------------------------------------
    // MAIN
    // ------------------------------------------------------------------
	//Original Version:
	/* Removed main method - not being used and causing issues with 'static'
	// Removed static from all methods making use of 'project'
    public static void main(String[] args) {
		//if (args.length == 0) {	//Already exists within JSymInterface - redundant (needed extra handling)
		//    usage(0);				//Probably would be okay left in - but removed atm
		//}
		Configuration config = buildConfigSetting(args);
		project = new ProjectHelper(config);

		try {
			run(config);
		} catch (Exception e) {
			Utils.log(JSymCompiler.class, "Fatal error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
    }
	 */
    // ------------------------------------------------------------------
    // CONFIG PARSER
    // ------------------------------------------------------------------
	//changed to public from private
    public static Configuration buildConfigSetting(String[] args) {
	String inputPath = absolutePath(args[0]);
	if (inputPath.isEmpty()) {
	    Utils.log(JSymCompiler.class, "The file " + args[0] + " does not exist.");
	    System.exit(0);
	}

	boolean isJar = inputPath.endsWith(".jar");
	boolean isApk = inputPath.endsWith(".apk");
	if (!isJar && !isApk) {
	    Utils.log(JSymCompiler.class, "Please enter a valid JAR or APK file.");
	    System.exit(0);
	}

	String targetPath = null;
	String xmlSrcSinkFilePath = "";
	String symmariesPath = "";
	String extraClasspath = null;
	String androidPlatforms = null;
	SymmariesAnalysis analysis = SymmariesAnalysis.IMPLICIT_CONF;
	SymmariesHeapDom heapDom = SymmariesHeapDom.DEEPALIAS; //It is set correctly here as a default
		//Utils.log(JSymCompiler.class, "heapDom: " + heapDom);
	boolean enableJimpleOptimizationPack = false;
	boolean enableSootOptimizations = false;
	boolean runSyrs = false;
	List<String> secsumFilePaths = new ArrayList<>();
	boolean includeAll = true;
	boolean includeInner = false;
	boolean analyzeLibs = false;
	List<String> pkgPrefixes = new ArrayList<>();

	for (int i = 1; i < args.length; i++) {
		switch (args[i]) {
			case "-o":
				targetPath = absolutePath(args[++i]);
				if (targetPath.isEmpty() || !new File(targetPath).isDirectory()) {
					Utils.log(JSymCompiler.class, "The output folder " + args[i] + " does not exist. Creating it.");
					Utils.createDirectory(args[i]);
					targetPath = absolutePath(args[i]);
				}
				break;
			case "-ss":
				xmlSrcSinkFilePath = absolutePath(args[++i]);
				if (xmlSrcSinkFilePath.isEmpty()) {
					Utils.log(JSymCompiler.class, "The source/sink file " + args[i] + " does not exist.");
					System.exit(0);
				}
				break;
			case "-sp":
				symmariesPath = absolutePath(args[++i]);
				if (symmariesPath.isEmpty()) {
					Utils.log(JSymCompiler.class, "Symmaries path " + args[i] + " may not exist.");
				}
				break;
			case "-tf":
				String secFile = absolutePath(args[++i]);
				if (secFile.isEmpty()) {
					Utils.log(JSymCompiler.class, "Security summary file " + args[i] + " does not exist.");
					System.exit(0);
				}
				secsumFilePaths.add(secFile);
				break;
			case "-cp":
				extraClasspath = args[++i];
				break;
			case "-ap":
				androidPlatforms = absolutePath(args[++i]);
				if (androidPlatforms.isEmpty()) {
					Utils.log(JSymCompiler.class, "Android platform dir " + args[i] + " does not exist.");
					System.exit(0);
				}
				break;
			case "-analysis":
				analysis = SymmariesAnalysis.ofString(args[++i]);
				break;
			case "+taints":
				analysis = SymmariesAnalysis.TAINT;
				break;
			case "-heapdom":
				heapDom = SymmariesHeapDom.ofString(args[++i]);
				Utils.log(JSymCompiler.class, "heapDomChanged: " + heapDom);
				break;
			case "-SO":
				enableSootOptimizations = true;
				break;
			case "-JO":
				enableJimpleOptimizationPack = true;
				break;
			case "+runSyrs":
				runSyrs = true;
				break;
			case "--help":
			case "-h":
				usage(0);
				break;
			case "--all":
				includeAll = true;
				break;
			case "--pkg":
				pkgPrefixes.clear();
				String csv = args[++i].substring("--pkg=".length()).trim();
				if (!csv.isEmpty()) {
					for (String p : csv.split(",")) {
						p = p.trim();
						if (!p.isEmpty()) {
							pkgPrefixes.add(p.endsWith(".") ? p : p + ".");
						}
					}
				}
				if (pkgPrefixes.isEmpty())
					includeAll = true;
				break;
			case "--include-inner":
				includeInner = true;
				break;
			case "--libs":
				analyzeLibs = true;
				break;
			default:
				Utils.log(JSymCompiler.class, "Unknown argument " + args[i] + ". Skipped.");
		}
	}

	if (xmlSrcSinkFilePath.isEmpty()) {
	    Utils.log(JSymCompiler.class, "Source/sink file is missing.");
	}

	if (symmariesPath.isEmpty()) {
	    Utils.log(JSymCompiler.class, "Symmaries path is missing. Using default path.");
	    symmariesPath = DEFAULT_SYRS_PATH;
	}

	if (targetPath == null) {
	    targetPath = new File(inputPath).getParent() + File.separator + "Syrs";
	    Utils.log(JSymCompiler.class, "Target folder is missing. Set it to " + targetPath);
	    Utils.createDirectory(targetPath);
	}

	TypeHandlingConfig typeHandlingConfig = new TypeHandlingConfig(true, // skipMethodsWithIncompatibleTypes
		false, // autoFixInconsistentTypes
		false // allowIncompatibleTypes
	);

	String[] extraClassPathArray = extraClasspath == null ? new String[] {} : new String[] { extraClasspath };
	String commandFilePath = targetPath + File.separator + "syrsCommand.command";

	Configuration config = new Configuration();
	//config.buildConfiguration(inputPath, targetPath, JSymCompiler(extraClassPathArray), Possible some issue may come from this
		config.buildConfiguration(inputPath, targetPath, extraClassPathArray,
		secsumFilePaths.toArray(new String[0]), xmlSrcSinkFilePath,
		androidPlatforms == null ? "" : androidPlatforms, isApk, symmariesPath, runSyrs, commandFilePath,
		analysis, heapDom, 300, false, enableSootOptimizations, enableJimpleOptimizationPack, true,
		typeHandlingConfig, false, includeAll, includeInner, pkgPrefixes, analyzeLibs);

	return config;
    }

    private static String[] addEmpty(String[] arr) {
	String[] out = Arrays.copyOf(arr, arr.length + 1);
	out[arr.length] = "";
	return out;
    }

    // ------------------------------------------------------------------
    // CORE DRIVER
    // ------------------------------------------------------------------
	//Change to public from private
    public void run(Configuration cfg) throws IOException {
	long start = System.currentTimeMillis();
	Utils.log(JSymCompiler.class, "Starting JSymCompiler at " + (new Date(start)));
	Utils.log(JSymCompiler.class, "Input: " + cfg.inputArtifactPath);

	clearDirs(cfg);
	Path targetDir = Paths.get(cfg.targetDir);
	Files.createDirectories(targetDir.resolve("meth"));
	Files.createDirectories(targetDir.resolve("logs"));

	List<String> allClasses = getClassesFromJar(cfg);
	System.out.println("[info] Classes to process: " + allClasses.size());

	String javaHome = System.getProperty("java.home");
	SootSetup.setupSoot(cfg);
	// Ensure JRE libs (or Android jars) are on the Soot classpath.
	SootSetup.ensureRuntimeOnClasspath(cfg);

	// Force-load core library classes before running our processing logic.
	soot.Scene.v().loadNecessaryClasses();
	// (Optional but helpful) request signatures for common exception types.
	soot.Scene.v().addBasicClass("java.lang.Throwable", soot.SootClass.SIGNATURES);
	soot.Scene.v().addBasicClass("java.lang.Exception", soot.SootClass.SIGNATURES);
	soot.Scene.v().addBasicClass("java.lang.RuntimeException", soot.SootClass.SIGNATURES);

	project.clear();
	project.loadSecstubsFromXMLFile();
	project.loadAssumedSecuritySignatures();
	project.extractSourceSinkFromXML();
	project.extractSourceSinksFromSecuritySignatures();

	project.processApplication(allClasses);
	createBashCommandFile();
	project.writeErrLog();

	if (cfg.runSyrs) {
		if(!Utils.runSymmaries(project.configurations.syrsCommandFile, project.configurations.syrsCommand))
			Utils.log(JSymCompiler.class, "You may check the generated command file " + cfg.targetDir + "/symmariesCommand.command.");
	}
    }

    // ------------------------------------------------------------------
    // PATH NORMALIZATION
    // ------------------------------------------------------------------
    private static String absolutePath(String path) {
	if (path == null)
	    return "";
	File f = new File(path);
	if (!f.exists())
	    return "";
	try {
	    return f.getCanonicalPath();
	} catch (IOException e) {
	    return f.getAbsolutePath();
	}
    }

    // ------------------------------------------------------------------
    // USAGE
    // ------------------------------------------------------------------
    private static void usage(int exitCode) {
	PrintStream ps = (exitCode == 0 ? System.out : System.err);
	ps.println("JSymCompiler - Translate Jimple to .meth");
	ps.println("Usage: JSymCompiler <input.jar|input.apk> [options]\n"
		+ "  -o   <dir>     Output directory (default: <inputParent>/Syrs)\n"
		+ "  -ss  <file>    Sources/Sinks XML\n" + "  -sp  <dir>     Symmaries path (default: syrs_default/)\n"
		+ "  -tf  <file>    Security summary stub (.secstub). Repeatable.\n"
		+ "  -cp  <path>    Extra classpath entry (jars or dirs; repeat w/ pathsep if needed)\n"
		+ "  -ap  <dir>     Android platforms root (needed when input is .apk)\n"
		+ "  -analysis <kind>  Analysis mode (IMPLICIT_CONF, TAINT, NONE, ...)\n"
		+ "  +taints       Shortcut for -analysis TAINT\n"
		+ "  -heapdom <k>  Heap domain (deep, FLAT, shallow, connect, dumb, ...)\n" //Changed from DEEPALIAS, added others (maybe more)
		+ "  -SO           Enable extra Soot optimizations\n"
		+ "  -JO           Enable Jimple optimization pack\n"
		+ "  -all          Include all packages in the .jar file\n"
		+ "  -pkg          Include only this specific list of packages.\n"
		+ "  -libs         Include libraray analysis including JavaAPI.\n"
		+ "  +runSyrs      Run downstream SyRS analysis (placeholder)\n" + "  -h|--help     Show this help");
	//Add syrs options to be read in through JSymCompiler
	//Have these be added/swapped to the set-up of default options
	//Add this info to the print output once done
	ps.flush();
	System.exit(exitCode);
    }

    // ------------------------------------------------------------------
    // CLASS EXTRACTION
    // ------------------------------------------------------------------
    private static List<String> getClassesFromJar(Configuration cfg) throws IOException {
	List<String> classes = new ArrayList<>();
	try (JarFile jarFile = new JarFile(new File(cfg.inputArtifactPath))) {
	    Enumeration<JarEntry> entries = jarFile.entries();
	    while (entries.hasMoreElements()) {
		JarEntry entry = entries.nextElement();
		String name = entry.getName();
		if (!name.endsWith(".class") || name.startsWith("META-INF/"))
		    continue;

		String cls = name.replace('/', '.').substring(0, name.length() - 6);
		if (!cfg.includeInner && cls.contains("$"))
		    continue;

		if (!cfg.includeAll && cfg.pkgPrefixes.stream().noneMatch(cls::startsWith)) {
		    continue;
		}
		classes.add(cls);
	    }
	}
	Collections.sort(classes);
	return classes;
    }

    // ------------------------------------------------------------------
    // DIRECTORY MANAGEMENT
    // ------------------------------------------------------------------
    private void clearDirs(Configuration cfg) {
	try {
	    if (cfg.clearOldGeneratedfiles || !new File(cfg.targetDir + "/CFG").exists()
		    || !new File(cfg.targetDir + "/Jimple").exists() || !new File(cfg.targetDir + "/Meth").exists()) {
		if (cfg.generateJimple)
		    Utils.remakeDirectory(cfg.targetDir + "/Jimple");
		if (cfg.generateCfg)
		    Utils.remakeDirectory(cfg.targetDir + "/CFG");
		if (!new File(cfg.targetDir + "/Meth").exists())
		    new File(cfg.targetDir + "/Meth").mkdirs();
	    }
	} catch (IOException e) {
	    Utils.logErr(JSymCompiler.class, "The path " + cfg.targetDir + " does not exist!\n");
	}
    }

    public void createBashCommandFile() {
	project.configurations.syrsCommand = buildBashCommand(project.configurations);

	String commandFileName = project.configurations.syrsCommandFile.isEmpty()
		? FilenameUtils.removeExtension(project.configurations.syrsCommandFile) + "_"
			+ getVariantCode(project.configurations) + "."
			+ FilenameUtils.getExtension(project.configurations.syrsCommandFile)
		: project.configurations.syrsCommandFile;

	File cmdFile = new File(commandFileName);
	String cmdPath = cmdFile.getAbsolutePath();

	if (cmdFile.getParent() != null) {
	    Utils.createDirectory(cmdFile.getParent());
	    if (!cmdFile.exists()) {
		Utils.createFile(cmdPath);
	    }
	    if (!Utils.writeTextFile(cmdPath, BASH_HEADER + project.configurations.syrsCommand)) {
		Utils.log(JSymCompiler.class, "Could not write the Symmaries command file " + cmdPath);
	    }
	} else {
	    Utils.logErr(JSymCompiler.class,
		    "The path " + cmdPath + " does not exist. Failed to write the Syrs commands file.");
	}
    }

    // --- Variant Code Generation ---
    public static String getVariantCode(SymmariesAnalysis analysis, SymmariesHeapDom heapDom, int methSkip,
	    boolean exceptionsEnabled, String realibReorder, String timeout, String cuddMem) {
	return getVariantCode(analysis, heapDom.toString(), methSkip, exceptionsEnabled, realibReorder, timeout,
		cuddMem);
    }

    public static String getVariantCode(SymmariesAnalysis analysis, String heapDomCode, int methSkip,
	    boolean exceptionsEnabled, String realibReorder, String timeout, String cuddMem) {
	return String.join("_", "", analysis.toString(), exceptionsEnabled ? "+exceptions" : "-exceptions", heapDomCode,
		methSkip >= 0 ? Integer.toString(methSkip) : "", realibReorder, "timeout=" + timeout,
		"cuddmem=" + cuddMem, "");
    }

    protected static String getVariantCode(Configuration config) {
	return getVariantCode(config.analysis, "\"${heapDomain}\"", config.methodSkipParameter,
		config.exceptionsEnabled, config.realibReorder, config.timeout, config.cuddMem);
    }

    // --- Command Construction ---
    protected static String buildBashCommand(Configuration config) {
	String commonOptions = buildCommonOptions(config);
	String synthOptions = buildSynthOptions(config);

	String targetDir = "${dir}" + File.separator;
	String oldScgsVersFile = targetDir + Constants.statsFilePrefix + VARIANT_PLACEHOLDER + ".syrs-version";

	String updateTypesCmd = buildUpdateTypesCmd(config, commonOptions, targetDir);
	String analyzeCmd = buildAnalyzeCmd(config, commonOptions, synthOptions, targetDir);

	return String.join("\n", "dir=" + qte(Utils.polishFilePath(config.targetDir)) + ";",
		"syrsvers=\"$(" + config.syrsPath + " -V)\";",
		getVersionCheckCommands(oldScgsVersFile, updateTypesCmd, analyzeCmd));
    }

    private static String buildCommonOptions(Configuration config) {
	return String.join(" ", "-ll w",
		"-tf " + String.join(",", config.exceptionsEnabled ? "+exceptions" : "-exceptions",
			config.analysis.getTransFlags(), config.heapDom.getvarHeapDomain()));
    }

    private static String buildSynthOptions(Configuration config) {
	return String.join(" ", "-of secsum", "-rf " + config.realibReorder,
		"-rf cuddmem=" + config.cuddMem, "-pf timeout=" + config.timeout)
		+ (config.methodSkipParameter >= 0 ? " --methskip-cond " + config.methodSkipParameter : "");
    }

    private static String buildUpdateTypesCmd(Configuration config, String commonOptions, String targetDir) {
	return String.join(" ", Constants.symmariesEnv, config.syrsPath, commonOptions,
		qte(targetDir + "types.classes"), "-o", qte(targetDir + "types.classes_bin"), "-o",
		qte(targetDir + "types" + Constants.class_statsFileExtension));//,"-allow-phantom-refs"
    }

    private static String buildAnalyzeCmd(Configuration config, String commonOptions, String synthOptions,
	    String targetDir) {
	return String.join(" ", Constants.symmariesEnv, config.syrsPath, commonOptions,
		qte(targetDir + "types.classes_bin"), qte(targetDir + "Meth/all.secstubs"),
		qte(targetDir + "Meth/all.meth_files"), synthOptions, "--safe-walk", "-o",
		qte(targetDir + Constants.statsFilePrefix + VARIANT_PLACEHOLDER + ".secsums"), "-o",
		qte(targetDir + Constants.statsFilePrefix + VARIANT_PLACEHOLDER + ".meth_stats"));
    }

    private static String getVersionCheckCommands(String oldScgsVersFile, String updateTypesCmd, String analyzeCmd) {
	return String.join("\n",
		"if test -r " + qte(oldScgsVersFile) + "; then oldvers=\"$(< " + qte(oldScgsVersFile)
			+ ")\"; else oldvers=\"none\"; fi;",
		"if [ \"$oldvers\" != \"$syrsvers\" ]; then",
		"   " + updateTypesCmd + " && " + analyzeCmd + " && echo \"$syrsvers\" > " + qte(oldScgsVersFile),
		"fi");
    }

    // --- Helper Methods ---
    private static String qte(String s) {
	return "\"" + s + "\"";
    }
}

