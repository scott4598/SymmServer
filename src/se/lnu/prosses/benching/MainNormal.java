package se.lnu.prosses.benching;

import java.io.File;

import se.lnu.prosses.configs.SymmariesAnalysis;
import se.lnu.prosses.configs.SymmariesHeapDom;

public class MainNormal extends BenchmarkExperimentConfig {
    public static void main(String[] args) throws Exception {
	benchmarkRelativePath = "/WebApplications/Final/";
	ReportName = new File(benchmarkRelativePath).getName();
	methodSkipParameter = 0;
	analysis = SymmariesAnalysis.EXPLICIT_CONF;
	heapDom = SymmariesHeapDom.DEEPALIAS;
	exceptionEnabeled = false;
	abortOnError = false;
	generateJimple = false;
	String targetSubfolder = analysis.toString();

	// JimpleProjectHelper project = new JimpleProjectHelper();
	// project.configurations.xmlSecsstubsPath = inputFolderPath+
	// "Secstubs/allSecStubs.xml";
	// project.loadSecstubsFromXMLFile();
	// String output = "";
	/*
	 * for(ArrayList<SecuritySignature> methods:
	 * project.securitySignatures.values()) for(SecuritySignature ss: methods)
	 * output += ss.toString(); Utils.writeTextFile(inputFolderPath+
	 * "Secstubs/allSecStubs.secstubs", output);
	 */

	/*
	 * new JavaApplicationHelper().processSingleJavaApplication(inputFolderPath +
	 * "/academy-web-target/classes/", outputFolderPath + "/academy-web-target/" +
	 * targetSubfolder, "", inputFolderPath + "Secstubs/allSecStubs.xml",
	 * secstubsFiles, exceptionEnabeled, true, engine, outputFolderPath +
	 * "/Misc/CommandFiles/Omegapoint" + targetSubfolder + "Command.command",
	 * analysis, heapDom, 120, false, true,// no soot optim false,
	 * typeHandlingConfig, generateJimple);
	 */

	// processAndroidApplications(true);
	// processJavaApplicationsFromClassFiles();
	// processJavaApplications();
	// processAndroidApplications(false);
    }

    private static void processAndroidApplications(boolean generateSCGSInputs) {
	inputFolderPath = "/Volumes/Academics/Workspaces/SymmariesExperiments/inputs/";
	String targetSubfolder = analysis.toString();
	String androidJDKPath = new File(inputFolderPath).getParent() + "/Config/android-4.1.1.4.jar";
	// String androidJDKPath = new File(inputFolderPath).getParent() +
	// "/Config/android.jar";
	// ArrayList<String> excludedApplications = new ArrayList<String>();
	/*
	 * new JavaApplicationHelper().processSingleJavaApplication(
	 * "/Volumes/Academics/Workspaces/SymmariesExperiments/inputs/pedometer/Pedometer.apk",
	 * outputFolderPath + "/android/" + targetSubfolder, inputFolderPath +
	 * "/Config/SourcesAndSinks.txt", inputFolderPath + "/Config/allSecStubs.xml",
	 * secstubsFiles, new String[] {androidJDKPath}, exceptionEnabeled, true,
	 * engine, outputFolderPath + "/Misc/CommandFiles/android" + targetSubfolder +
	 * "androidCommand.command", analysis, heapDom, 120, true, false, true,// no
	 * soot optim false, typeHandlingConfig, generateJimple);
	 */
    }

}
