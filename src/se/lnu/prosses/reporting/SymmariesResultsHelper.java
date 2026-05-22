package se.lnu.prosses.reporting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import se.lnu.prosses.benching.BenchmarkExperimentConfig;
import se.lnu.prosses.configs.Constants;
import se.lnu.prosses.core.ProjectHelper;
import se.lnu.prosses.reporting.ApplicationStatsHelper.ApplicationName;
import se.lnu.prosses.utils.Utils;

public class SymmariesResultsHelper extends BenchmarkExperimentConfig {

    private static ArrayList<String> usedPackages = new ArrayList<String>();
    private static ArrayList<String> usedMethods = new ArrayList<String>();
    static FilenameFilter secsumsFilter = new FilenameFilter() {
	@Override
	public boolean accept(File dir, String name) {
	    return name.toLowerCase().endsWith(Constants.secsumsFileExtension);
	}
    };
    static FilenameFilter methFilter = new FilenameFilter() {
	@Override
	public boolean accept(File dir, String name) {
	    return name.toLowerCase().endsWith(Constants.meth_statsFileExtension);
	}
    };

    public Map<ApplicationName, ApplicationStatsHelper> ApplicationsAnalysisResultsMap = new TreeMap<ApplicationName, ApplicationStatsHelper>();

    protected Map<String, Boolean> groundTruthResultsMap = new HashMap<String, Boolean>();

    protected void export_applications_statistics(String outputPath, String reportPath, String inputpath) {
	String out = "";// generateRtffFileHeader();
	for (ApplicationStatsHelper application : ApplicationsAnalysisResultsMap.values()) {
	    out += application.exportApplicationResults();
	}
	if (Utils.writeTextFile(reportPath, out))
	    Utils.log(this.getClass(), "Exported applications statistics to " + reportPath);
    }

    public void export_list_of_used_JavaLib_methods(File directory, String targetFileName) {
	try {
	    extract_all_Java_Libs(directory);
	    String output = "";
	    usedPackages.sort(null);
	    for (String pack : usedPackages)
		output += pack + "\n";
	    // Utils.log(this.getClass(),output);
	    Utils.writeTextFile(directory + targetFileName + "Packages.txt", output);
	    output = "";
	    usedMethods.sort(null);
	    for (String method : usedMethods)
		output += method + "\n";
	    // Utils.log(this.getClass(),output);
	    Utils.writeTextFile(directory + targetFileName + "Methods.txt", output);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    void extract_all_Java_Libs(File secstubsDirectory) throws Exception {
	for (File file : Utils.getFilesOfTypes(secstubsDirectory.getAbsolutePath(),
		new String[] { Constants.secstubsFileExtension }))
	    process_secstub_file(file);
    }

    protected void extract_single_app_statistics(File syrsDir, String experimentPath, boolean analyzeGuardsResults,
	    boolean ABMBenchamrk) {
	if (syrsDir.exists()) {

	    DirectedGraph<String, DefaultEdge> call_graph = Utils.readDotFile(syrsDir + "/Meth/all-cg.dot");
	    for (File meth_statsFile : Utils.getFilesOfTypes(syrsDir.getAbsolutePath(),
		    new String[] { Constants.meth_statsFileExtension }))
	    // if (meth_statsFile.getAbsolutePath().contains("shallow"))
	    {
		try {
		    Utils.log(getClass(), "\n\n Processing the meth_stats file " + meth_statsFile.getParent());

		    String algorithmIndex = meth_statsFile.getName().replaceAll(Constants.statsFilePrefix, "")
			    .replaceAll(Constants.meth_statsFileExtension, "");

		    String applicationName = "";
		    // DON'T REMOVE: application name for ABM analysis results
		    if (ABMBenchamrk)
			applicationName = new File(meth_statsFile.getAbsolutePath().replaceAll(experimentPath, ""))
				.getParentFile().getName();
		    else
			// application name for IFSPEC analysis results
			applicationName = new File(meth_statsFile.getAbsolutePath().replaceAll(experimentPath, ""))
				.getParentFile().getParentFile().getParentFile().getParentFile().getName();

		    ApplicationStatsHelper stats = new ApplicationStatsHelper();
		    ApplicationName application = stats.new ApplicationName(applicationName, algorithmIndex);
		    if (!process_application_by_full_name(ApplicationsAnalysisResultsMap, application)) {
			File jisymb_errLogFile = new File(meth_statsFile.getParentFile(), Constants.errLogFile);
			File types_proc_statsFile = new File(meth_statsFile.getParentFile(),
				"types" + Constants.proc_statsFileExtension);
			File class_statsFile = new File(meth_statsFile.getParentFile(),
				"types" + Constants.class_statsFileExtension);
			File elapsed_timeFile = new File(meth_statsFile.getAbsolutePath()
				.replaceAll(Constants.meth_statsFileExtension, Constants.elapsed_timeFileExtension));
			File analysis_proc_statsFile = new File(
				org.apache.commons.io.FilenameUtils.removeExtension(meth_statsFile.getAbsolutePath())
					+ Constants.analysis_proc_statsFileExtension);
			File secsumsFile = new File(meth_statsFile.getAbsolutePath()
				.replaceAll(Constants.meth_statsFileExtension, Constants.secsumsFileExtension));
			stats = new ApplicationStatsHelper(application, jisymb_errLogFile, meth_statsFile,
				analysis_proc_statsFile, types_proc_statsFile, class_statsFile, secsumsFile,
				elapsed_timeFile, analyzeGuardsResults, call_graph);
			ApplicationsAnalysisResultsMap.put(application, stats);
			Utils.log(getClass(), "Processed " + meth_statsFile.getAbsolutePath());
		    }
		} catch (IOException e) {
		    System.err.println(
			    "Failed to load the experiments results. Maybe Symmaries has failed to run or has not been called!");
		} catch (NumberFormatException e) {
		    System.err.println("Bad-formatted statistics file:" + meth_statsFile.getAbsolutePath());
		}
	    }

	} else
	    Utils.log(getClass(), syrsDir + " does not exist!");

    }

    private boolean process_application_by_full_name(
	    Map<ApplicationName, ApplicationStatsHelper> applicationsAnalysisResultsMap2, ApplicationName application) {

	for (ApplicationName app : applicationsAnalysisResultsMap2.keySet())
	    if (app.compareTo(application) == 0)
		return true;

	return false;
    }

    protected void compare_to_IFSPec_ground_truths(String inputpath, String reportPath, String reportSheetPath,
	    String outFolderPath) {
	int totalNumberOfAnalyzedResults = 0, insecureConsistents = 0, secureConsisstents = 0, unsounds = 0,
		restrictives = 0;
	String textualReport = "",
		resultsSheet = "App Name; groundTruth; result; highSources;lowSink;skippedMethods\n ",
		lineSeparator = "\n------------------------------------\n";
	List<String> processedApps = new ArrayList<String>();
	Utils.log(this.getClass(), "Comparing the results to the benachmark\n ");
	SortedSet<String> soretedApps = new TreeSet<>(groundTruthResultsMap.keySet());
	String groundTruth = "";
	for (String originalApp : soretedApps) {
	    ArrayList<ApplicationName> keys = get_applications_by_original_name(ApplicationsAnalysisResultsMap.keySet(),
		    originalApp);
	    // new TreeSet<>(ApplicationsAnalysisResultsMap.keySet());
	    for (ApplicationName applicationName : keys) {
		String appCodeRelativePath = benchmarkRelativePath + File.separator + applicationName.originalName;
		String appCodeAbsolutePath = inputpath + File.separator + applicationName.originalName;
		if (!processedApps.contains(appCodeRelativePath)) {
		    processedApps.add(appCodeRelativePath);
		    textualReport += appCodeRelativePath + lineSeparator
			    + get_sources_sinks(appCodeAbsolutePath, appCodeRelativePath);
		    for (File file : Utils.getFilesOfTypes(appCodeAbsolutePath, new String[] { ".java" }))
			textualReport += Utils.readTextFile(file.getAbsolutePath());

		    groundTruth = Utils.readTextFile(appCodeAbsolutePath + "/ground-truth.txt");
		    textualReport += "Grounh Truth: " + groundTruth;
		    textualReport += "Requirements: " + Utils.readTextFile(appCodeAbsolutePath + "/requirements.txt");
		    textualReport += "Description:" + Utils.readTextFile(appCodeAbsolutePath + "/description.txt");
		    textualReport += lineSeparator;

		}

		ProjectHelper project = new ProjectHelper();
		project.configurations.xmlSourcesAndSinks = appCodeAbsolutePath + "/rifl.xml";
		project.extractSourceSinkFromXML();
		// HashMap<String, List<String[]>> highSources =
		// project.sourceSinkHelper.highSources;
		// HashMap<String, List<String[]>> lowSinks = project.sourceSinkHelper.lowSinks;

		if (ApplicationsAnalysisResultsMap.get(applicationName) != null) {
		    String appResults = ApplicationsAnalysisResultsMap.get(applicationName).exportApplicationResults();

		    String comaprisonResult = "";
		    Utils.log(getClass(), "App source code path is :" + appCodeAbsolutePath);

		    Utils.log(this.getClass(), lineSeparator + "Processing the Symmaries analysis results of "
			    + applicationName.getfullName());
		    if (groundTruthResultsMap.get(applicationName.originalName) == null) {
			System.err.println("Failed to find the ground truth results of " + appCodeRelativePath);
		    } else if (ApplicationsAnalysisResultsMap.get(applicationName).non_true_guard_methods.size() == 0)
			if (groundTruthResultsMap.get(applicationName.originalName)) {
			    secureConsisstents++;
			    comaprisonResult = "Consistent secure results for " + applicationName.getfullName();
			    Utils.log(this.getClass(), appResults);
			} else {
			    unsounds++;
			    comaprisonResult = "Unsound results for " + applicationName.getfullName();
			    Utils.logErr(this.getClass(), appResults);
			}
		    else if (groundTruthResultsMap.get(applicationName.originalName)) {
			restrictives++;
			comaprisonResult = "Restrictive results  for " + applicationName.getfullName();
			Utils.logErr(this.getClass(), appResults);

		    } else {
			insecureConsistents++;
			comaprisonResult = "Consistent insecure results for " + applicationName.getfullName();
			Utils.log(this.getClass(), appResults);
		    }
		    textualReport += comaprisonResult + "\n" + appResults + lineSeparator;
		    resultsSheet += ApplicationsAnalysisResultsMap.get(applicationName).exportApplicationResultsToSheet(
			    project.sourceSinkHelper, groundTruth, appCodeAbsolutePath) + '\n';
		}
		// }
	    }
	}

	Utils.writeTextFile(reportPath, textualReport + "\n\n\n\n ");
	Utils.writeTextFile(reportSheetPath, resultsSheet + "\n\n\n\n ");
	Utils.log(this.getClass(), "Exported applications statistics to " + reportPath);
	Utils.log(this.getClass(), "\n**************************************************************\n"
		+ "The number of applications processed by SCGS is: " + ApplicationsAnalysisResultsMap.size());
	Utils.log(this.getClass(),
		"The total number of appliactions whose results are compared: " + totalNumberOfAnalyzedResults);
	Utils.log(this.getClass(), "The number of restrictive results: " + restrictives);
	Utils.log(this.getClass(), "The number of insecureConsistents: " + insecureConsistents);
	Utils.log(this.getClass(), "The number of secureConsisstents: " + secureConsisstents);
	Utils.log(this.getClass(), "The number of unsounds: " + unsounds);
    }

    private ArrayList<ApplicationName> get_applications_by_original_name(Set<ApplicationName> keySet,
	    String originalApp) {

	ArrayList<ApplicationName> apps = new ArrayList<ApplicationName>();
	for (ApplicationName app : keySet)
	    if (app.originalName.equals(originalApp))
		apps.add(app);
	return apps;
    }

    private String get_sources_sinks(String inputpath, String applicationName) {
	String csOut = "";
	for (File file : Utils.getFilesOfTypes(
		new File(inputpath + applicationName).getParentFile().getParentFile().getParent(),
		new String[] { ".xml" })) {
	    ProjectHelper project = new ProjectHelper();
	    project.configurations.xmlSourcesAndSinks = file.getAbsolutePath();
	    project.extractSourceSinkFromXML();
	    csOut += "High Sources: \n";
	    for (String key : project.sourceSinkHelper.highSources.keySet()) {
		csOut += key + " -> ";
		for (String[] entry : project.sourceSinkHelper.highSources.get(key)) {
		    for (String elm : entry)
			csOut += elm + '/';
		    csOut += "\n";
		}
	    }
	    csOut += "\nLow Sinks: \n";
	    for (String key : project.sourceSinkHelper.lowSinks.keySet()) {
		csOut += key + " -> ";
		for (String[] entry : project.sourceSinkHelper.lowSinks.get(key)) {
		    for (String elm : entry)
			csOut += elm + '/';
		    csOut += "\n";
		}
	    }
	}
	return csOut;
    }

    private void process_secstub_file(File file) {
	try {
	    Utils.log(this.getClass(), "Processing " + file.getCanonicalPath());
	    Scanner sc = new Scanner(file);
	    while (sc.hasNextLine()) {
		String line = sc.nextLine().replaceAll("static", "").trim();
		if (line.contains(":")) {
		    String curPackage = (line.contains(":") ? line.substring(line.indexOf(' ') + 1, line.indexOf(':'))
			    : line.substring(line.indexOf('('))),
			    curMethod = (line.contains(":") ? line.substring(line.indexOf(' ') + 1, line.indexOf('('))
				    : line.substring(line.indexOf('(')));

		    if (!usedPackages.contains(curPackage))// ) && (curPackage.contains("java") ||
			// curPackage.contains("android")))
			usedPackages.add(curPackage);
		    if (usedPackages.contains(curPackage) && !usedMethods.contains(curMethod))
			usedMethods.add(curMethod);

		}
	    }
	    sc.close();
	} catch (FileNotFoundException e) {
	    System.err.println("Could not open the secstub file!");
	} catch (IOException e) {
	    System.err.println("Could not find the path!");
	}
    }

    public void export_methods_statistics_to_sheet(File report) {
	String out = MethodStatsHelper.getSheetHeader() + "\n";
	for (ApplicationStatsHelper applicationName : this.ApplicationsAnalysisResultsMap.values())
	    for (MethodStatsHelper methodStats : applicationName.methodsStatsMap.values())
		out += methodStats.exportStats() + '\n';
	if (Utils.writeTextFile(report.getAbsolutePath(), out))
	    Utils.log(getClass(), "Analysis stats exported to " + report.getAbsolutePath());
    }

    public void export_entry_points_guards(String report_path) {
	for (ApplicationStatsHelper application_info : this.ApplicationsAnalysisResultsMap.values()) {
	    String insecure_methods = "LIST of POTENTIALLY INSECURE METHODS\n\n";
	    String entrypoints_methods = "LIST of ALL ENTRY POINTS\n\n";
	    String insecure_entry_points = "LIST of POTENTIALLY INSECURE ENTRY POINTS\n\n";
	    String all_methods = "NUMBER of ALL METHODS : " + application_info.methodsStatsMap.size() + "\n";
	    all_methods += "NUMBER of SKIPPED METHODS : " + application_info.skippedMethodsList.size() + "\n";
	    int i = 1, j = 1, k = 1;

	    for (MethodStatsHelper methodStats : application_info.methodsStatsMap.values()) {
		String line = methodStats.methodID + "  " + methodStats.getCheckpointGuardMaps() + '\n';
		// all_methods += line;
		if (methodStats.isEntryPoint) {
		    entrypoints_methods += i++ + " " + line;
		    if (!methodStats.has_true_guard())
			insecure_entry_points += j++ + " " + line;
		}
		if (!methodStats.has_true_guard())
		    insecure_methods += k++ + " " + line;
	    }
	    all_methods += "NUMBER of POTENTIALLY INSECURE METHODS : " + (k - 1) + "\n";
	    all_methods += "NUMBER of ENTRY POINTS  : " + (i - 1) + "\n";
	    all_methods += "NUMBER of POTENTIALLY INSECURE ENTRY POINTS : " + (j - 1) + "\n\n\n";

	    String report = new File(report_path + "/" + application_info.applicationName.originalName
		    + application_info.applicationName.analysisSettings + "_entrypoint.text").getAbsolutePath();

	    if (Utils.writeTextFile(report, String.join("\n\n",
		    new String[] { all_methods, insecure_methods, entrypoints_methods, insecure_entry_points })))
		Utils.log(getClass(), "Analysis stats exported to " + report);
	}
    }

    public void export_method_effects(String report_path) {
	HashMap<String, List<String>> updates_list = new HashMap<String, List<String>>();
	String all_updates = "";
	for (ApplicationStatsHelper application_info : this.ApplicationsAnalysisResultsMap.values()) {
	    String app_name = application_info.applicationName.originalName;
	    updates_list.put(app_name, new ArrayList<String>());
	    for (MethodStatsHelper methodStats : application_info.methodsStatsMap.values()) {
		boolean flag = false;
		for (String checkpoint : application_info.methodsStatsMap.get(methodStats.methodID).checkpointGuardMaps
			.keySet()) {
		    if (checkpoint.split("%").length > 1 && !checkpoint.contains("%guard")
			    && !checkpoint.contains(":")) {
			String cp = checkpoint.split("%")[1].replaceAll("ret", "");
			cp = cp.substring(1, cp.length() - 1).trim();
			if (!cp.equals(application_info.methodsStatsMap.get(methodStats.methodID).checkpointGuardMaps
				.get(checkpoint))) {
			    updates_list.get(app_name).add(String.join(",", new String[] { methodStats.methodID, cp,
				    application_info.applicationName.analysisSettings,
				    application_info.methodsStatsMap.get(methodStats.methodID).checkpointGuardMaps
					    .get(checkpoint) }));
			    flag = true;
			}
		    }
		}
		if (flag)
		    application_info.nb_updating_method++;

	    }
	    String report = new File(report_path + "/" + app_name + // application_info.applicationName.analysisSettings
								    // +
		    "_method_updates.csv").getAbsolutePath();
	    if (Utils.writeTextFile(report, updates_list.get(app_name)))
		Utils.log(getClass(), "Analysis stats exported to " + report);
	    all_updates += updates_list.get(app_name);

	}

	String report = new File(report_path + "/all_apps_method_updates.csv").getAbsolutePath();
	if (Utils.writeTextFile(report, all_updates))
	    Utils.log(getClass(), "Analysis stats exported to " + report);
    }

    private boolean updated_footprint(String checkpoint) {
	// TODO Auto-generated method stub
	return false;
    }

    /*
     * This method only call a method insecure, if it is insecure by all heap
     * domains.
     */
    public void export_conditionally_secure_methods_in_all_doms(String report_path) {

	// Get all apps names
	ArrayList<String> app_names = new ArrayList<String>();
	for (ApplicationStatsHelper application_info : this.ApplicationsAnalysisResultsMap.values())
	    if (!app_names.contains(application_info.applicationName.originalName))
		app_names.add(application_info.applicationName.originalName);

	// Get all methods that have non-true guards for all heap domains
	HashMap<String, ArrayList<String>> insecure_methods_for_all_heap_domains = new HashMap<String, ArrayList<String>>();
	for (String app_name : app_names)
	    for (ApplicationStatsHelper application_info : this.ApplicationsAnalysisResultsMap.values())
		if (application_info.applicationName.originalName.equals(app_name))
		    if (insecure_methods_for_all_heap_domains.get(app_name) != null) {
			insecure_methods_for_all_heap_domains.put(app_name,
				Utils.list_intersection(insecure_methods_for_all_heap_domains.get(app_name),
					application_info.non_true_guard_methods));
		    } else
			insecure_methods_for_all_heap_domains.put(app_name, application_info.non_true_guard_methods);

	//
	HashMap<String, ArrayList<ApplicationStatsHelper>> apps_heap_domain_map = new HashMap<String, ArrayList<ApplicationStatsHelper>>();
	for (ApplicationStatsHelper application_info : this.ApplicationsAnalysisResultsMap.values()) {
	    if (apps_heap_domain_map.get(application_info.applicationName.originalName) == null)
		apps_heap_domain_map.put(application_info.applicationName.originalName,
			new ArrayList<ApplicationStatsHelper>());
	    apps_heap_domain_map.get(application_info.applicationName.originalName).add(application_info);
	}

	String guard_results_record = "AppName, Method, IsEntryPoint, HeapDomain, Guard\n";
	for (ApplicationStatsHelper application_info : this.ApplicationsAnalysisResultsMap.values()) {
	    if (app_names.contains(application_info.applicationName.originalName)) {
		ArrayList<String> insecure_methods_guards = new ArrayList<String>();
		int entry_point_count = 1, insecure_entrypoint_count = 1, insecure_methods_count = 1;

		for (MethodStatsHelper methodStats : application_info.methodsStatsMap.values()) {
		    {
			if (methodStats.isEntryPoint)
			    entry_point_count++;
			if (!methodStats.has_true_guard() && insecure_methods_for_all_heap_domains
				.get(application_info.applicationName.originalName).contains(methodStats.methodID)) {
			    for (ApplicationStatsHelper inner_app_info : apps_heap_domain_map
				    .get(application_info.applicationName.originalName)) {
				for (String checkpoint : inner_app_info.methodsStatsMap
					.get(methodStats.methodID).checkpointGuardMaps.keySet())
				    if (checkpoint.contains("%guard"))
					insecure_methods_guards.add(String.join(",",
						new String[] { methodStats.methodID,
							// (methodStats.isEntryPoint ? "T" : "F"),
							inner_app_info.applicationName.analysisSettings,
							inner_app_info.methodsStatsMap
								.get(methodStats.methodID).checkpointGuardMaps
								.get(checkpoint) }));
			    }
			    insecure_methods_count++;
			    if (methodStats.isEntryPoint)
				insecure_entrypoint_count++;
			}
		    }
		}

		for (String method_guard : insecure_methods_guards)
		    guard_results_record += String.join(",",
			    new String[] { application_info.applicationName.originalName, method_guard
			    // ,
			    // (insecure_methods_count - 1) + "", entry_point_count - 1 + "",
			    // insecure_entrypoint_count - 1 + ""
				    , "\n" });
		app_names.remove(application_info.applicationName.originalName);
		application_info.potentially_insecure_entrypoint_count = (insecure_entrypoint_count - 1);
		application_info.entry_point_count = (entry_point_count - 1);

	    }
	}
	if (Utils.writeTextFile(report_path + "/" // + application_info.applicationName.originalName
		+ "conditionally_insecure_methods.csv", guard_results_record))
	    Utils.log(getClass(), "Summary of vulnerability analysis of " + // application_info.applicationName.originalName
									    // +
		    "List of conditionally insecure methods exported to " + report_path);

    }

    protected void export_apps_statistics_to_sheet(File report, Collection<String> algoIds) {
	boolean generic = algoIds != null && algoIds.isEmpty();
	String out = ApplicationStatsHelper.getSheetHeader(generic, true, true) + "\n";
	System.out.println(generic);
	for (ApplicationStatsHelper application : ApplicationsAnalysisResultsMap.values()) {
	    if (algoIds == null || algoIds.contains(application.applicationName.analysisSettings))
		out += application.exportStats(generic) + '\n';
	}
	if (Utils.writeTextFile(report.toString(), out))
	    Utils.log(getClass(), "App. stats exported to " + report);
    }

    protected void extract_all_apps_statistics(File syrsWorksDir, boolean ABMBenchmark) {
	int counter = 0, noApps = Utils.getDirectories(syrsWorksDir.getAbsolutePath()).size();
	for (String syrsDirName : Utils.getDirectories(syrsWorksDir.getAbsolutePath())) {
	    File syrsDir = new File(syrsWorksDir, syrsDirName);
	    // polish_call_graph(syrsDir);
	    extract_single_app_statistics(syrsDir, syrsWorksDir.getAbsolutePath(), true, ABMBenchmark);
	    Utils.log(getClass(), ++counter * 100 / noApps + "% has been processed.");
	}
    }

    protected void export_statistics_to_sheet(String statistics_report_file_name, Collection<String> algoIds) {
	this.export_methods_statistics_to_sheet(new File(statistics_report_file_name + "_method_statistics.csv"));
	this.export_apps_statistics_to_sheet(new File(statistics_report_file_name + "_apps_statistics.csv"), algoIds);
    }

    private void polish_call_graph(File syrsDir) {

	String cnt = Utils.readTextFile(syrsDir + "/Meth/all-cg.dot");
	cnt = cnt.replaceAll("/home/lnk85/symmaries/7194/files/SyrsWorks/" + syrsDir.getName() + "/Meth/", "")
		.replaceAll("\"", "").replaceAll("/Meth/", "");
	Utils.writeTextFile(syrsDir + "/Meth/all-cg.dot", cnt);
    }

    public static class OfDir {
	public static void main(String[] args) {
	    if (args.length != 2) {
		System.err.println("Usage: <syrsWorksDir> <csvFile>");
		System.exit(1);
	    }
	    File syrsWorksDir = new File(args[0]);
	    File report = new File(args[1]);
	    // new SymmariesResultsHelper().generat_statistics_sheet(syrsWorksDir, report,
	    // null, true);
	}
    }

}
