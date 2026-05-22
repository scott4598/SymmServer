package se.lnu.prosses.reporting;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import se.lnu.prosses.configs.Constants;
import se.lnu.prosses.core.SourceSinkHelper;
import se.lnu.prosses.utils.Utils;

public class ApplicationStatsHelper {
    public class ApplicationName implements Comparable {
	public String originalName = "";
	public String heapDomName = "";
	// public String fullName;
	public String analysisSettings = "";

	ApplicationName() {

	}

	public ApplicationName(String applicationName, String algIndex) {
	    originalName = applicationName;
	    analysisSettings = algIndex;
	}

	@Override
	public int compareTo(Object o) {
	    ApplicationName compared = (ApplicationName) (o);
	    if (originalName.compareTo(compared.originalName) == 0
		    && analysisSettings.compareTo(compared.analysisSettings) != 0)
		return analysisSettings.compareTo(compared.analysisSettings);
	    else
		return originalName.compareTo(compared.originalName);
	    // return originalName.compareTo(compared.originalName)*2 +
	    // algoId.compareTo(compared.algoId);
	}

	public String getfullName() {
	    // TODO Auto-generated method stub
	    return originalName + analysisSettings;
	}

	public CharSequence getFullName() {
	    // TODO Auto-generated method stub
	    return originalName + analysisSettings;
	}

    }

    public final ApplicationName applicationName;
    // public final String algoId;
    public HashMap<String, MethodStatsHelper> methodsStatsMap = new HashMap<String, MethodStatsHelper>();
    HashMap<String, String> methodSignatureToMethFileMap = new HashMap<String, String>();
    ArrayList<String> sourcesList = new ArrayList<String>();

    /// time-related statistics
    float avgMethodAnalysisTime = 0f;
    public float total_accounted_time;
    public float total_clock_time;
    public float total_effective_time;

    // statistics of application
    ArrayList<String> skippedMethodsList = new ArrayList<String>();
    ArrayList<String> processedMethodsList = new ArrayList<String>();
    public ArrayList<String> non_true_guard_methods = new ArrayList<String>();
    int nb_skipped_summarizations;
    int nb_total_analyzed_files = 0;
    int nb_summarized_meths = 0;
    public int nb_meths;
    public int nb_totsafe_meths;
    int nb_unsafe_meths;
    int unsat_guard;

    // method-specific related statistics
    float meanMethodSize;
    int maxMethodSize;

    float meanFootprintSize;
    float meanMethodLoc;
    float meanMethodTrans;
    float meanMethodVariables;
    float meanSkippedMethodLoc;
    float meanSkippedMethodTrans;
    float meanSkippedMethodVariables;
    float meanSummarizedMethodLoc;
    float meanSummarizedMethodTrans;
    float meanSummarizedMethodVariables;
    float meanSupportSize;
    float stddevFootprintSize;
    float stddevMethodLoc;
    float stddevMethodTrans;
    float stddevMethodVariables;
    float stddevSkippedMethodLoc;
    float stddevSkippedMethodTrans;
    float stddevSkippedMethodVariables;
    float stddevSummarizedMethodLoc;
    float stddevSummarizedMethodTrans;
    float stddevSummarizedMethodVariables;
    float stddevSupportSize;
    int maxFootprintSize;
    int maxMethodLoc;
    int maxMethodTrans;
    int maxMethodVariables;
    int maxSkippedMethodLoc;
    int maxSkippedMethodTrans;
    int maxSkippedMethodVariables;
    int maxSummarizedMethodLoc;
    int maxSummarizedMethodTrans;
    int maxSummarizedMethodVariables;
    int maxSupportSize;
    int minFootprintSize;
    int minMethodLoc;
    int minMethodTrans;
    int minMethodVariables;
    int minSkippedMethodLoc;
    int minSkippedMethodTrans;
    int minSkippedMethodVariables;
    int minSummarizedMethodLoc;
    int minSummarizedMethodTrans;
    int minSummarizedMethodVariables;
    int minSupportSize;

    int nb_summarizations = 0;
    int maxSummarizationsPerMeth = 0;

    private int nb_excluded_meths_by_user;

    // generic info (non-analysis-related)
    int nb_entries_in_all_meths_file;
    public int javaLOC;
    public int totalByteCodeLOC = 0;
    int nb_jisymbGenerationErrors; // app-specific
    // int nb_lib_methods;

    int nb_classes;
    int nb_interfaces;
    int alias_rel_size;
    float alias_rel_density;
    int field_alias_rel_size;
    float field_alias_rel_density;
    private int nb_all_meth_in_dir;

    private static String i(int i) {
	return String.format("%,d", i);
    }

    private static String f1(float f) {
	return String.format("%.1f", f);
    }

    private static String f2(float f) {
	return String.format("%.2f", f);
    }

    private static String f3(float f) {
	return String.format("%.3f", f);
    }

    private static final String floatRegexp = "[+-]?(\\d+\\.?\\d*|nan|inf)";

    public static float parseFloat(String s) {
	// System.out.println ("'" + s + "'");
	s = s.trim();
	switch (s) {
	case "-inf":
	    return Float.NEGATIVE_INFINITY;
	case "inf":
	case "+inf":
	    return Float.POSITIVE_INFINITY;
	case "nan":
	case "-nan":
	case "+nan":
	    return Float.NaN;
	default:
	    return Float.parseFloat(s);
	}
    }

    public ApplicationStatsHelper(ApplicationName applicationName, File jisymb_errLogFile, File meth_statsFile,
	    File analysis_proc_statsFile, File types_proc_statsFile, File class_statsFile, File secsumsFile,
	    File elapsedTimeFile, boolean analyzeGuardsResults, DirectedGraph<String, DefaultEdge> call_graph)
	    throws FileNotFoundException, java.io.IOException {
	this.applicationName = applicationName;
	// algoId = algoId;
	build_application_size_statistics(meth_statsFile.getParentFile());
	extract_stats_from_meth_stats_file(meth_statsFile, applicationName.analysisSettings, call_graph);

	if (elapsedTimeFile != null)
	    this.total_clock_time = Float.parseFloat(Utils.readStringTextFile(elapsedTimeFile.getAbsolutePath()));

	nb_entries_in_all_meths_file = Utils
		.readStringTextFile(meth_statsFile.getParentFile().getAbsolutePath() + "/Meth/all.meth_files")
		.split("\n").length;

	nb_all_meth_in_dir = Utils
		.getFilesOfTypes(meth_statsFile.getParentFile().getAbsolutePath() + "/Meth", new String[] { ".meth" })
		.size();

	if (jisymb_errLogFile != null && jisymb_errLogFile.exists())
	    nb_jisymbGenerationErrors = (int) Files.lines(jisymb_errLogFile.toPath()).count();
	if (analysis_proc_statsFile != null && analysis_proc_statsFile.exists())
	    extractAnalysisProcStats(analysis_proc_statsFile);
	if (types_proc_statsFile != null && types_proc_statsFile.exists())
	    extractTypesProcStats(types_proc_statsFile);
	if (class_statsFile != null && class_statsFile.exists())
	    extractStatsFromClass_StatsFile(class_statsFile);

	if (nb_total_analyzed_files != 0 && analysis_proc_stats != null)
	    try {
		avgMethodAnalysisTime = (parseFloat(analysis_proc_stats[0]) / nb_total_analyzed_files);
	    } catch (Exception _) {
	    }

	if (analyzeGuardsResults) {
	    for (File methFile : Utils.getFilesOfTypes(meth_statsFile.getParent(),
		    new String[] { Constants.methExtension })) {
		Scanner sc = new Scanner(methFile);
		methodSignatureToMethFileMap.put(getMethodSignatureFromMethPath(sc.nextLine()),
			methFile.getAbsolutePath());
		String methodName = getMethodNameFromMethPath(methFile.getAbsolutePath());
		if (!processedMethodsList.contains(methodName))
		    skippedMethodsList.add(methodName);
		sc.close();
	    }
	    for (File srcsFile : Utils.getFilesOfTypes(meth_statsFile.getParent(),
		    new String[] { Constants.srcsExtension })) {
		for (String source : Utils.readStringTextFile(srcsFile.getAbsolutePath()).split("\n"))
		    sourcesList.add(source);
	    }
	    extract_secsums_from_file(secsumsFile);
	    // numberOfProcessedMethods = processedMethodsList.size();
	    set_insecure_methods();
	}
    }

    /*
     * public void setAnalysisStatistics() { for(MethodStatsHelper
     * methodStatsHelper: methodsStatsMap.values())
     * if(methodStatsHelper.summarization!=null &&
     * methodStatsHelper.summarization.trim().equals("done")){ totalAnalysisTime +=
     * methodStatsHelper.triangularization_time + methodStatsHelper.synthesis_time +
     * methodStatsHelper.model_instantiation_time; analyzedMethods++; } if
     * (analyzedMethods!=0) avgMethodAnalysisTime =
     * totalAnalysisTime/(analyzedMethods); else avgMethodAnalysisTime = 0;
     * 
     * }
     */

    public ApplicationStatsHelper() {
	applicationName = null;
	// TODO Auto-generated constructor stub
    }

    public String exportApplicationResults() {
	String out = "This application is " + (non_true_guard_methods.size() == 0 ? "SECURE" : "INSECURE")
		+ " according to Symmaries\n";
	out += "The Total Number of Methods: " + nb_meths + "\n";
	out += "Number of Processed Methods: " + processedMethodsList.size() + "\n";
	out += "Number of Skipped Methods: " + skippedMethodsList.size() + "\n";
	if (skippedMethodsList.size() > 0) {
	    out += "List of Skipped Methods:\n ";
	    for (String method : skippedMethodsList)
		out += method + "\n ";
	} else
	    out += "All the methods have been processed!";
	out += "\n[List of processed Methods]: ";
	for (String methodName : processedMethodsList) {
	    for (String checkpoint : methodsStatsMap.get(methodName).getCheckpointGuardMaps().keySet())
		out += checkpoint + " : " + methodsStatsMap.get(methodName).getCheckpointGuardMaps().get(checkpoint)
			+ "\n";
	}

	out += "\n[List of entry points or source methods]:\n ";
	for (String methodName : processedMethodsList)
	    try {
		String escapedMethodName = methodName.substring(methodName.lastIndexOf(File.separator) + 1,
			methodName.lastIndexOf("."));
		if (sourcesList.contains(escapedMethodName) || escapedMethodName.contains("doGet"))
		    for (String checkpoint : methodsStatsMap.get(methodName).getCheckpointGuardMaps().keySet())
			out += checkpoint + " : "
				+ methodsStatsMap.get(methodName).getCheckpointGuardMaps().get(checkpoint) + "\n";
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	return out.replaceAll("\n\n", "\n");
    }

    public String exportApplicationResultsToSheet(SourceSinkHelper sourceSinkHelper, String groundTruth,
	    String appCodeAbsolutePath) {
	String skippedMethods = "", highSources = "", lowSinks = "", res = "";
	if (skippedMethodsList.size() > 0) {
	    for (String method : skippedMethodsList)
		skippedMethods += method + " AND ";
	}

	for (String sourceType : sourceSinkHelper.highSources.keySet()) {
	    for (String[] entry : sourceSinkHelper.highSources.get(sourceType)) {
		// for(String elm : entry)
		highSources += sourceType + "_" + entry[1] + " AND ";
	    }
	    ;
	}

	for (String sinkType : sourceSinkHelper.lowSinks.keySet()) {
	    for (String[] entry : sourceSinkHelper.lowSinks.get(sinkType)) {
		// for(String elm : entry)
		lowSinks += sinkType + "_" + entry[1] + " AND ";
	    }
	    ;
	}

	for (String sinkType : sourceSinkHelper.lowSinks.keySet())
	    // if the sink is a method, we do not know which method to check
	    //
	    // else
	    // if the sink is a return, we add its method ret
	    if (sinkType.equals(Utils.XMlReturnValue)) {
		for (String[] sink : sourceSinkHelper.lowSinks.get(sinkType)) {
		    String singMethodName = sink[1].split("\\(")[0];
		    for (String methodName : processedMethodsList) {
			if (methodName.contains(singMethodName))
			    for (String checkpoint : methodsStatsMap.get(methodName).getCheckpointGuardMaps().keySet())
				if (checkpoint.contains("ret"))
				    res += checkpoint + "_"
					    + methodsStatsMap.get(methodName).getCheckpointGuardMaps().get(checkpoint)
					    + " AND ";
		    }
		}
	    }

	for (String methodName : processedMethodsList) {
	    if (methodName.contains("doGet"))
		for (String checkpoint : methodsStatsMap.get(methodName).getCheckpointGuardMaps().keySet())
		    res += checkpoint.split(":")[1] + "_"
			    + methodsStatsMap.get(methodName).getCheckpointGuardMaps().get(checkpoint);
	}

	return String.join(";", applicationName.getfullName(), groundTruth.trim().replaceAll("\n", ""), res,
		highSources, lowSinks, skippedMethods);
    }

    public static String getSheetHeader(boolean generic, boolean withAnalysisProcStats, boolean withTypesProcStats) {
	return "" + "ApplicationName; " + "JavaLOC; " + "ByteCodeLOC; " + "#meths_in_dir; "
		+ "#meths_in_all_meths_file; " + "#nb_meths; "
		// + "Mean/Max; #BytecodeLOC/AllMethFiles; " + "Average #Bytecode/AllMethFiles;
		// " // duplicate,
		// for
		// verification
		+ "#Jisymb Errors; "
		+ (generic ? ""
			: "Analysis; " + "%Total Processed Meths (Neither Excluded by User nor Skipped by Syrs); "
				+ "%Successfully Summarized by Syrs(Not Skipped by Syrs); "
				// Those below are meths that are missed by Symmaries
				// because they have a strictly identical signature
				// than another method. Are missing something that
				// distinguishes method signatures in class files?
				+ "#nb_excluded_meths_by_user; " + "#nb_ignored_by_syrs; " + "#nb_skipped_by_syrs; "
				+ "#nb_total_skipped_ignored_by_syrs; " + "conditionally_insecure_methods_count;"
				+ "#Unsat Guards; " + "#Secure; " + "#updating_methods; "
				// + "potentially_insecure_entrypoint_count;" + "entry_point_count;"
				+ "Total Accounted Time; " + "Total Effective Time; " + "Clock Time ; "
				+ "Average Method Analysis Time; "
				+ ((withAnalysisProcStats)
					? String.join(" (analysis); ", Constants.timeFmtDescr) + " (analysis); "
					: ""))

	// + "Mean/Max SCFG Locations; " + "Min/Mean/Max SCFG Locations (Summarized); "
	// + "Min/Mean/Max SCFG Locations (Skipped); "
	//
	// + "Mean/Max SCFG Transitions; " + "Min/Mean/Max SCFG Transitions
	// (Summarized); "
	// + "Min/Mean/Max SCFG Transitions (Skipped); "
	//
	// + "Mean/Max SCFG Variables; " + "Min/Mean/Max SCFG Variables (Summarized); "
	// + "Min/Mean/Max SCFG Variables (Skipped); "
	//
	// + "Min/Mean/Max Support Size; " + "Min/Mean/Max Footprint Size; "
	//
	// + "Mean/Max #Summarizations Per Meth; "
	//
	// + ((withTypesProcStats) ? String.join(" (types); ", Constants.timeFmtDescr) +
	// " (types); " : "")
	//
	// + "#Classes; " + "#Interfaces; " + "Size(AliasRel); " + "Density(AliasRel); "
	// + "Size(FieldAliasRel); "
	// + "Density(FieldAliasRel); " + "delta-_FA(java.lang.Object); " +
	// "delta-_FA(java.lang.Object[]); "
	// + "delta-_FA(java.util.Collection)"
	// + "nbClasses; "
	// + "nbLibMethods; "
	;
    }

    public String exportStats(boolean generic) {
	return String.join("; ", applicationName.originalName, i(javaLOC), i(totalByteCodeLOC), i(nb_all_meth_in_dir), // total
														       // in
														       // the
														       // dir
		i(nb_entries_in_all_meths_file), // total
		i(nb_meths), // total number of meth files in .meth_stats file
		// f1(meanMethodSize) + "/" + i(maxMethodSize), f1((float) totalByteCodeLOC /
		// (float) nb_all_meth_files),
		i(nb_jisymbGenerationErrors))

		+ (generic ? ""
			: (String.join("; ", ";" + applicationName.analysisSettings,
				f2((float) nb_total_analyzed_files / nb_entries_in_all_meths_file * 100f),
				f2(nb_total_analyzed_files / (float) nb_meths * 100f),
				i(nb_entries_in_all_meths_file - nb_meths), // "#nb_ignored_by_syrs;
									    // "
				i(nb_skipped_summarizations), // skipped
							      // by
							      // Symmaries
				i(nb_excluded_meths_by_user), //
				i(nb_skipped_summarizations + nb_entries_in_all_meths_file - nb_meths), // total number
													// of skipped
				// method: manually or
				// by Symmaries
				i(nb_total_analyzed_files - nb_totsafe_meths), i(nb_unsafe_meths), i(nb_totsafe_meths),
				i(nb_updating_method),
				// i(potentially_insecure_entrypoint_count), i(entry_point_count),
				f3(total_accounted_time), f3(total_effective_time), f3(this.total_clock_time),
				f3(avgMethodAnalysisTime))
				+ ((analysis_proc_stats == null) ? ""
					: "; " + joinProcStats("; ", analysis_proc_stats))))

	// + (String.join("; ", "", f2(meanMethodLoc) + "/" + maxMethodLoc,
	// minSummarizedMethodLoc + "/" + f2(meanSummarizedMethodLoc) + "/" +
	// maxSummarizedMethodLoc,
	// minSkippedMethodLoc + "/" + f2(meanSkippedMethodLoc) + "/" +
	// maxSkippedMethodLoc,
	//
	// f2(meanMethodTrans) + "/" + maxMethodTrans,
	// minSummarizedMethodTrans + "/" + f2(meanSummarizedMethodTrans) + "/" +
	// maxSummarizedMethodTrans,
	// minSkippedMethodTrans + "/" + f2(meanSkippedMethodTrans) + "/" +
	// maxSkippedMethodTrans,
	//
	// f2(meanMethodVariables) + "/" + maxMethodVariables,
	// minSummarizedMethodVariables + "/" + f2(meanSummarizedMethodVariables) + "/"
	// + maxSummarizedMethodVariables,
	// minSkippedMethodVariables + "/" + f2(meanSkippedMethodVariables) + "/"
	// + maxSkippedMethodVariables,
	//
	// minSupportSize + "/" + f2(meanSupportSize) + "/" + maxSupportSize,
	// minFootprintSize + "/" + f2(meanFootprintSize) + "/" + maxFootprintSize,
	//
	// f2((float) nb_summarizations / (float) nb_summarized_meths) + "/" +
	// maxSummarizationsPerMeth))
	//
	// + ((types_proc_stats == null) ? "" : "; " + joinProcStats(";",
	// types_proc_stats))
	//
	// + (String.join("; ", "", i(nb_classes), i(nb_interfaces), i(alias_rel_size),
	// f3(alias_rel_density),
	// i(field_alias_rel_size), f3(field_alias_rel_density),
	// i(field_alias_in_degree_Object),
	// i(field_alias_in_degree_Object_array), i(field_alias_in_degree_Collection)))
	// nb_lib_methods + ";"
	;
    }

    private static String joinProcStats(String sep, String[] ps) {
	return String.join(sep,
		(java.util.stream.IntStream.range(0, ps.length)
			.mapToObj(i -> Constants.timeFmt[i].format(Float.parseFloat(ps[i])))
			// .mapToObj(i -> String.format (Constants.timeFmt[i],
			// Float.parseFloat (ps[i])))
			// (java.util.Arrays.stream (ps)
			// .map (s -> f2 (Float.parseFloat (s)))
			.toArray(String[]::new)));
    }

    String getMethodNameFromMethPath(String methodPath) {
	methodPath = methodPath.replaceAll("\"", "");
	return new File(methodPath).getName().trim();
    }

    void extract_secsums_from_file(File secsumFile) {
	String fileContent = Utils.readTextFile(secsumFile.getAbsolutePath()).toString();
	try {
	    if (!fileContent.trim().equals("")) {
		Utils.log(getClass(), "Processing .secsums file  " + secsumFile.getAbsolutePath());
		String[] applicationResults = fileContent.trim().split("\\}");
		for (String method_result : applicationResults) {
		    HashMap<String, String> methodsResults = new HashMap<String, String>();
		    String[] elements = method_result.split("\\{")[0].split("\\(")[0].split(" ");
		    String method_name = elements[elements.length - 1];
		    for (String element : method_result.split("\\{")[1].split(";")) {
			String lhs = element.split("=")[0].trim().trim();
			if (lhs.startsWith("ok("))
			    throw new Exception();
			// if (lhs.matches("(guard)") || lhs.startsWith("ret(level())") ||
			// lhs.startsWith("ok("))
			{
			    String rhs = element.substring(element.indexOf("=") + 1).trim();
			    methodsResults.put(method_name + "%" + lhs, rhs);
			}
		    }
		    String methodSignature = getMethodSignatureFromMethPath(method_result);
		    if (methodSignatureToMethFileMap.get(methodSignature) != null) { // if this methods is not a
			// third=part
			// method
			String methodFileName = getMethodNameFromMethPath(
				methodSignatureToMethFileMap.get(methodSignature));
			if (methodsStatsMap.containsKey(methodFileName))
			    methodsStatsMap.get(methodFileName).setCheckpointGuardMaps(methodsResults);
			else
			    Utils.logErr(getClass(), "Could not find an entry for the method " + methodFileName);
		    }
		}
	    }
	} catch (RuntimeException ex) {
	    Utils.logErr(getClass(), "Could  not extract the security summaries from " + secsumFile.getAbsolutePath()
		    + "It's possibly empty or does not follow the secsum files syntax");
	} catch (Exception e) {
	    Utils.logErr(getClass(), "Found OK() guard" + secsumFile.getAbsolutePath());
	    System.exit(0);
	}
    }

    public String[] analysis_proc_stats;

    void extractAnalysisProcStats(File af) {
	try {
	    analysis_proc_stats = new String(Files.readAllBytes(af.toPath())).trim().split("\\s*;\\s*");
	} catch (java.io.IOException _) {
	    Utils.log(getClass(), "Unable to load process-wise statistics file " + af);
	}
    }

    public String[] types_proc_stats;

    void extractTypesProcStats(File af) {
	try {
	    types_proc_stats = new String(Files.readAllBytes(af.toPath())).trim().split("\\s*;\\s*");
	} catch (java.io.IOException _) {
	    Utils.log(getClass(), "Unable to load process-wise statistics file " + af);
	}
    }

    void extract_stats_from_meth_stats_file(File meth_statsFile, String analysisSettings,
	    DirectedGraph<String, DefaultEdge> call_graph) {
	String fileContent = Utils.readStringTextFile(meth_statsFile.getAbsolutePath());
	Utils.log(getClass(), "Processing " + meth_statsFile.getName());
	String[] all_methods_statistics = fileContent.replaceAll(" ", "").replaceAll("methods:\\{", "")
		.replaceAll(" ", "").replaceAll("\n", "").split("stats: ")[0].split("}},");
	// System.out.print(fileContent);
	// List<String> processedMethodsList = new ArrayList<String>();

	for (int index = 0; index < all_methods_statistics.length; index++) {
	    MethodStatsHelper single_method_stats = new MethodStatsHelper();
	    single_method_stats.extract_single_method_stats(all_methods_statistics[index].trim(), analysisSettings,
		    call_graph);
	    if (single_method_stats.methodID != null && !single_method_stats.methodID.contains("infos")) {
		methodsStatsMap.put(single_method_stats.methodID, single_method_stats);
		processedMethodsList.add(single_method_stats.methodID);
	    }
	    Pattern processedPattern = Pattern.compile("\\bprocessed\\:\\s*(\\d+),");
	    Matcher processedMatcher = processedPattern.matcher(all_methods_statistics[index].trim());
	    if (processedMatcher.find()) {
		int processed = Integer.parseInt(processedMatcher.group(1));
		if (processed > maxSummarizationsPerMeth)
		    maxSummarizationsPerMeth = processed;
		nb_summarizations += processed;
		nb_summarized_meths++;
	    }

	}

	// totalMethods = applicationResults.length;
	String summary = fileContent.substring(fileContent.lastIndexOf("stats:"));
	Matcher m = Pattern.compile("(nb_unsafe_meths)\\:\\s+\\d+\\,").matcher(summary);
	while (m.find())
	    nb_unsafe_meths = Integer.parseInt(m.group().replace("nb_unsafe_meths:", "").replace(",", "").trim());
	m = Pattern.compile("(unsat_guard)\\:\\s+\\d+\\,").matcher(summary);
	while (m.find())
	    unsat_guard = Integer.parseInt(m.group().replace("unsat_guard:", "").replace(",", "").trim());
	m = Pattern.compile("(nb_skipped_summarizations)\\:\\s+\\d+").matcher(summary);
	while (m.find())
	    nb_skipped_summarizations = Integer
		    .parseInt(m.group().replace("nb_skipped_summarizations:", "").replace(",", "").trim());
	m = Pattern.compile("(total_accounted_time)\\:\\s+" + floatRegexp).matcher(summary);
	while (m.find())
	    total_accounted_time = Float
		    .parseFloat(m.group().replace("total_accounted_time:", "").replace(",", "").trim());
	m = Pattern.compile("(total_effective_time)\\:\\s+" + floatRegexp).matcher(summary);
	while (m.find())
	    total_effective_time = Float
		    .parseFloat(m.group().replace("total_effective_time:", "").replace(",", "").trim());
	m = Pattern.compile("(nb_meths)\\:\\s+\\d+").matcher(summary);
	while (m.find())
	    nb_meths = Integer.parseInt(m.group().replace("nb_meths:", "").replace(",", "").trim());
	m = Pattern.compile("(nb_totsafe_meths)\\:\\s+\\d+").matcher(summary);
	while (m.find())
	    nb_totsafe_meths = Integer.parseInt(m.group().replace("nb_totsafe_meths:", "").replace(",", "").trim());
	m = Pattern.compile("(nb_unsafe_meths)\\:\\s+\\d+").matcher(summary);
	while (m.find())
	    nb_unsafe_meths = Integer.parseInt(m.group().replace("nb_unsafe_meths:", "").replace(",", "").trim());

	float[] tmp = extractStatisticsPerParameter(summary, "nb_scfg_variables");
	maxMethodVariables = (int) tmp[0];
	minMethodVariables = (int) tmp[1];
	meanMethodVariables = tmp[2];
	stddevMethodVariables = tmp[3];

	tmp = extractStatisticsPerParameter(summary, "nb_scfg_locations");
	maxMethodLoc = (int) tmp[0];
	minMethodLoc = (int) tmp[1];
	meanMethodLoc = tmp[2];
	stddevMethodLoc = tmp[3];

	tmp = extractStatisticsPerParameter(summary, "nb_scfg_transitions");
	maxMethodTrans = (int) tmp[0];
	minMethodTrans = (int) tmp[1];
	meanMethodTrans = tmp[2];
	stddevMethodTrans = tmp[3];

	tmp = extractStatisticsPerParameter(summary, "support_size");
	maxSupportSize = (int) tmp[0];
	minSupportSize = (int) tmp[1];
	meanSupportSize = tmp[2];
	stddevSupportSize = tmp[3];

	tmp = extractStatisticsPerParameter(summary, "footprint_size");
	maxFootprintSize = (int) tmp[0];
	minFootprintSize = (int) tmp[1];
	meanFootprintSize = tmp[2];
	stddevFootprintSize = tmp[3];

	tmp = extractStatisticsPerParameter(summary, "summarized_nb_scfg_variables");
	maxSummarizedMethodVariables = (int) tmp[0];
	minSummarizedMethodVariables = (int) tmp[1];
	meanSummarizedMethodVariables = tmp[2];
	stddevSummarizedMethodVariables = tmp[3];

	tmp = extractStatisticsPerParameter(summary, "summarized_nb_scfg_locations");
	maxSummarizedMethodLoc = (int) tmp[0];
	minSummarizedMethodLoc = (int) tmp[1];
	meanSummarizedMethodLoc = tmp[2];
	stddevSummarizedMethodLoc = tmp[3];

	tmp = extractStatisticsPerParameter(summary, "summarized_nb_scfg_transitions");
	maxSummarizedMethodTrans = (int) tmp[0];
	minSummarizedMethodTrans = (int) tmp[1];
	meanSummarizedMethodTrans = tmp[2];
	stddevSummarizedMethodTrans = tmp[3];

	tmp = extractStatisticsPerParameter(summary, "skipped_nb_scfg_variables");
	maxSkippedMethodVariables = (int) tmp[0];
	minSkippedMethodVariables = (int) tmp[1];
	meanSkippedMethodVariables = tmp[2];
	stddevSkippedMethodVariables = tmp[3];

	tmp = extractStatisticsPerParameter(summary, "skipped_nb_scfg_locations");
	maxSkippedMethodLoc = (int) tmp[0];
	minSkippedMethodLoc = (int) tmp[1];
	meanSkippedMethodLoc = tmp[2];
	stddevSkippedMethodLoc = tmp[3];

	tmp = extractStatisticsPerParameter(summary, "skipped_nb_scfg_transitions");
	maxSkippedMethodTrans = (int) tmp[0];
	minSkippedMethodTrans = (int) tmp[1];
	meanSkippedMethodTrans = tmp[2];
	stddevSkippedMethodTrans = tmp[3];

	nb_excluded_meths_by_user = nb_all_meth_in_dir - nb_entries_in_all_meths_file;
	nb_total_analyzed_files = nb_meths - nb_skipped_summarizations;// - nb_excluded_meths_by_user;
	if (nb_total_analyzed_files != 0)
	    avgMethodAnalysisTime = (total_accounted_time / nb_meths);
	// potentially_insecure_methods_count = (insecure_methods_count - 1);

    }

    float[] extractStatisticsPerParameter(String summary, String param) {
	Matcher m = Pattern.compile("(\\b" + param + ")\\:[^\\}]*\\}").matcher(summary);
	float[] out = new float[4];
	while (m.find()) {
	    String tmp = m.group();
	    int index = 0;
	    String[] items = { "max", "min", "mean", "stddev" };
	    for (String item : items) {
		Matcher m2 = Pattern.compile("(" + item + ")\\:\\s*" + floatRegexp).matcher(tmp);
		if (m2.find()) {
		    String tmp2 = m2.group();
		    out[index] = parseFloat(tmp2.replace(item + ":", "").trim());
		}
		index++;
	    }
	}
	return out;
    }

    static public long getTotalJavaLOC(String appSrcPath) throws java.io.IOException {
	long i = 0L;
	for (File f : Utils.getFilesOfTypes(appSrcPath, new String[] { ".java" })) {
	    i += Files.lines(f.toPath()).count();
	}
	return i;
    }

    // static public Integer[] get_methods_LOC_statistics(String methPath) {
    // int instructionNum = 0, max = 0;
    // // String all_methods_locs = "";
    // ArrayList<File> methods = Utils.getFilesOfTypes(methPath, new String[] {
    // ".meth" });
    // for (File meth : methods) {
    // String content = Utils.readStringTextFile(meth.getAbsolutePath());
    // int meth_loc = content.split("\\n").length;
    // instructionNum += meth_loc;
    // max = (meth_loc > max ? meth_loc : max);
    // // all_methods_locs += meth_loc + ",";
    // }
    // nb_all_meth_files = methods.size();
    // meanMethodSize = instructionNum / methods.size();
    // maxMethodSize = max;
    // if (instructionNum != null)
    // totalByteCodeLOC += instructionNum;
    // // return new String[] { Integer.toString(instructionNum / methods.size()),
    // // Integer.toString(max),
    // // Integer.toString(instructionNum), all_methods_locs,
    // // Integer.toString(methods.size()) };
    // return new Integer[] { instructionNum / methods.size(), max, instructionNum,
    // methods.size() };
    // }

    protected void build_application_size_statistics(File syrsInputFolder) {
	if (syrsInputFolder.exists()) {
	    int instructionNum = 0, max = 0;
	    // String all_methods_locs = "";
	    ArrayList<File> methods = Utils.getFilesOfTypes(syrsInputFolder.getAbsolutePath(),
		    new String[] { ".meth" });
	    for (File meth : methods) {
		String content = Utils.readStringTextFile(meth.getAbsolutePath());
		int meth_loc = content.split("\\n").length;
		instructionNum += meth_loc;
		max = (meth_loc > max ? meth_loc : max);
		// all_methods_locs += meth_loc + ",";
	    }
	    meanMethodSize = instructionNum / methods.size();
	    maxMethodSize = max;
	    totalByteCodeLOC += instructionNum;
	} else
	    Utils.log(getClass(), "Directory " + syrsInputFolder + " does not exist!");
    }

    String getMethodSignatureFromMethPath(String method_result) {
	return method_result.split("\\{")[0].replaceAll(" ", "").replaceAll("\n", "").trim();
    }

    void set_insecure_methods() {
	for (MethodStatsHelper methodPoint : methodsStatsMap.values())
	    if (methodPoint.true_guard != null && !methodPoint.has_true_guard())
		non_true_guard_methods.add(methodPoint.methodID);
    }

    void extractStatsFromClass_StatsFile(File class_statsFile) {
	String fileContent = Utils.readStringTextFile(class_statsFile.getAbsolutePath());
	Utils.log(getClass(), "Processing " + class_statsFile.getName());

	Matcher m = Pattern.compile("(nb_classes)\\:\\s+\\d+\\,").matcher(fileContent);
	while (m.find())
	    nb_classes = Integer.parseInt(m.group().replace("nb_classes:", "").replace(",", "").trim());
	m = Pattern.compile("(nb_interfaces)\\:\\s+\\d+\\,").matcher(fileContent);
	while (m.find())
	    nb_interfaces = Integer.parseInt(m.group().replace("nb_interfaces:", "").replace(",", "").trim());
	m = Pattern.compile("(\\balias_rel_size)\\:\\s+\\d+").matcher(fileContent);
	while (m.find())
	    alias_rel_size = Integer.parseInt(m.group().replace("alias_rel_size:", "").replace(",", "").trim());
	m = Pattern.compile("(\\balias_rel_density)\\:\\s+" + floatRegexp).matcher(fileContent);
	while (m.find())
	    alias_rel_density = parseFloat(m.group().replace("alias_rel_density:", "").replace(",", ""));
	m = Pattern.compile("(\\bfield_alias_rel_size)\\:\\s+\\d+").matcher(fileContent);
	while (m.find())
	    field_alias_rel_size = Integer
		    .parseInt(m.group().replace("field_alias_rel_size:", "").replace(",", "").trim());
	m = Pattern.compile("(\\bfield_alias_rel_density)\\:\\s+" + floatRegexp).matcher(fileContent);
	while (m.find())
	    field_alias_rel_density = parseFloat(m.group().replace("field_alias_rel_density:", "").replace(",", ""));

	m = Pattern.compile("(\\bfield_alias_in_degree\\(java\\.lang\\.Object\\))\\:\\s+\\d+").matcher(fileContent);
	while (m.find())
	    field_alias_in_degree_Object = Integer.parseInt(
		    m.group().replace("field_alias_in_degree(java.lang.Object):", "").replace(",", "").trim());

	m = Pattern.compile("(\\bfield_alias_in_degree\\(java\\.lang\\.Object\\[\\]\\))\\:\\s+\\d+")
		.matcher(fileContent);
	while (m.find())
	    field_alias_in_degree_Object_array = Integer.parseInt(
		    m.group().replace("field_alias_in_degree(java.lang.Object[]):", "").replace(",", "").trim());

	m = Pattern.compile("(\\bfield_alias_in_degree\\(java\\.util\\.Collection\\))\\:\\s+\\d+").matcher(fileContent);
	while (m.find())
	    field_alias_in_degree_Collection = Integer.parseInt(
		    m.group().replace("field_alias_in_degree(java.util.Collection):", "").replace(",", "").trim());

    }

    private int field_alias_in_degree_Object;
    private int field_alias_in_degree_Object_array;
    private int field_alias_in_degree_Collection;
    public int entry_point_count;
    // public int potentially_insecure_methods_count;
    public int potentially_insecure_entrypoint_count;
    public int nb_updating_method;

}
