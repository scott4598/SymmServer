package se.lnu.prosses.configs;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    // public String androidJarPath;

    // Android callbacks
    /******************** Android Files *************************/
    public String callbacks;
    public String monitorHelperPath;

    /******************** Input Files *************************/
    public String[] sigsecsfiles;
    public String xmlSecsstubsPath = "";
    public String xmlSourcesAndSinks;

    /******************** Output File Paths *************************/
    public String syrsCommandFile = "";

    public String targetDir;
    public String syrsCommand;

    /******************** JSymCompiler settings **********************/
    public String inputArtifactPath;

    public boolean isAPK;
    public boolean enableSootOptimizations = false; // default?
    public boolean enableJimpleOptimizationPack = false;

    public String[] extraClassPaths;

    public boolean clearOldGeneratedfiles = false;
    public boolean abortOnError = false;

    public String defaultSecuritySummary = "-<~;";

    public TypeHandlingConfig typeHandlingConfig;

    public List<String> thirdPartyMethods = new ArrayList<String>();
    public List<String> excludedMathods = new ArrayList<String>();
    public List<String> excludedClasses = new ArrayList<String>();

    public boolean generateSymmCommand;

    public boolean runSyrs;
    public boolean generateJimple = true;
    public boolean generateCfg = false;

    public String syrsPath = "";

    /******************** Symmaries settings **********************/

    public static final String REALIB_NO_REORDER = "-r";
    public static final String REALIB_STATIC_REORDER = "-dr";
    public static final String REALIB_DYNAMIC_REORDER = "+dr";
    public String realibReorder = REALIB_STATIC_REORDER;

    public int methodSkipParameter = 100;
    public String timeout = "5m";
    public String cuddMem = "4GiB";

    // public String scgsPath;
    public boolean exceptionsEnabled = false;

    public boolean includeInner = true;

    public boolean analyzelibs = false;

    public SymmariesAnalysis analysis = SymmariesAnalysis.HEAP_ONLY;
    public SymmariesHeapDom heapDom = SymmariesHeapDom.DEEPALIAS;

    public boolean generateSyrsScfg = false;
    // public boolean ignoreCheckPoints;
    // Symmaries command
    //public String engine = "syrs.opt"; //Scott - Changed the Symmaries call
    public String engine = "syrs";
    public boolean generateSymmariesInput = false;
    public String androidPlatforms;
    public boolean includeAll;
    public List<String> pkgPrefixes;

    /****************************************************************/

    public Configuration() {
    }

    public Configuration(SymmariesAnalysis analysis, boolean e) {
	this.analysis = analysis;
	// ignoreCheckPoints = false;
    }

    public void buildConfiguration(String pPath, String targetFolder, String[] prequiredClassesPaths,
	    String[] secsumfilePath, String pxmlScrSinkPath, String pxmlSecsstubsPath, boolean exceptionEnabeled,
	    String pSyrsPath, boolean prunSyrs, String commadFilePath, SymmariesAnalysis panalysis,
	    SymmariesHeapDom pheapDom, int pmethodSkipParameter, boolean ploadFromApk, boolean penableSootOptimizations,
	    boolean penableJimpleOptimizationPack, boolean pabortOnError, TypeHandlingConfig typeHandlingConfig1,
	    boolean pgenerateJimple, boolean includeAll2, boolean includeInner2, List<String> pkgPrefixes2,
	    boolean analyzeLibs2) {
	inputArtifactPath = pPath;
	targetDir = targetFolder;
	extraClassPaths = prequiredClassesPaths;
	sigsecsfiles = secsumfilePath;
	exceptionsEnabled = exceptionEnabeled;
	syrsPath = pSyrsPath;
	syrsCommandFile = commadFilePath;
	methodSkipParameter = pmethodSkipParameter;
	enableSootOptimizations = penableSootOptimizations;
	enableJimpleOptimizationPack = penableJimpleOptimizationPack;
	isAPK = ploadFromApk;
	analysis = panalysis;
	heapDom = pheapDom;
	xmlSourcesAndSinks = pxmlScrSinkPath;
	xmlSecsstubsPath = pxmlSecsstubsPath;
	abortOnError = pabortOnError;
	typeHandlingConfig = typeHandlingConfig1;
	generateJimple = pgenerateJimple;
	engine = pSyrsPath;
	runSyrs = prunSyrs;
	includeAll = includeAll2;
	includeInner = includeInner2;
	pkgPrefixes = pkgPrefixes2;
	analyzelibs = analyzeLibs2;
    }

    /*
     * public boolean isLocalReax() { if (reaxPath == null || isValidIP(reaxPath))
     * return false; return true; }
     */

    // Check if the IP is valid
    /*
     * private boolean isValidIP(String IP) { String[] IpParts = IP.split("\\."); if
     * (IpParts.length == 4) { for (int i = 0; i < 4; i++) { try { int intIpParts =
     * Integer.parseInt(IpParts[i]); if (intIpParts < 0 || intIpParts > 255) {
     * return false; }
     * 
     * } catch (NumberFormatException e) { return false; } } } else { return false;
     * } return true; }
     */

}
