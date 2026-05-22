package se.lnu.prosses.benching;

import java.util.HashMap;

import se.lnu.prosses.configs.SymmariesAnalysis;
import se.lnu.prosses.configs.SymmariesHeapDom;
import se.lnu.prosses.configs.TypeHandlingConfig;

public class BenchmarkExperimentConfig {
    protected static String benchmarkRelativePath;
    protected static String ReportName;
    protected static SymmariesAnalysis analysis;
    protected static SymmariesHeapDom heapDom;
    protected static int methodSkipParameter = 500;
    protected static boolean enableSootOptimizations = true;

    protected static boolean enableJimpleOptimizationPack = false;
    protected static boolean generateSymmariesInput = false;

    protected static boolean exceptionEnabeled = false;
    protected static boolean generateJimple = true;

    protected static String engine;

    protected static TypeHandlingConfig typeHandlingConfig = new TypeHandlingConfig(false, false, true);

    protected static String inputFolderPath = null;
    public static String outputFolderPath = null;
    protected static String[] secstubsFiles = null;
    public static String xmlSourceSinkFile = null;
    public static boolean abortOnError;

    public static HashMap<String, String> methodBody = new HashMap<>();

}
