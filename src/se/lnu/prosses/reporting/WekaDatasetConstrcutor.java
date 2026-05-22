package se.lnu.prosses.reporting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import se.lnu.prosses.reporting.ApplicationStatsHelper.ApplicationName;
import se.lnu.prosses.utils.Utils;

public class WekaDatasetConstrcutor extends SymmariesResultsHelper {

    public WekaDatasetConstrcutor() {
	super();
    }

    public WekaDatasetConstrcutor(HashMap<ApplicationName, ApplicationStatsHelper> applicationsAnalysisResultsMap) {
	ApplicationsAnalysisResultsMap = applicationsAnalysisResultsMap;
    }

    public void constructDataSet(String targetPath) {
	String out = generateRtffFileHeader();
	HashMap<ApplicationName, List<ApplicationStatsHelper>> applicationGroups = new HashMap<ApplicationName, List<ApplicationStatsHelper>>();
	for (ApplicationStatsHelper application : ApplicationsAnalysisResultsMap.values()) {
	    if (!applicationGroups.containsKey(application.applicationName))
		applicationGroups.put(application.applicationName, new ArrayList<ApplicationStatsHelper>());
	    applicationGroups.get(application.applicationName).add(application);
	}
	for (ApplicationName applicationName : applicationGroups.keySet()) {
	    for (MethodStatsHelper methodStats : applicationGroups.get(applicationName).get(0).methodsStatsMap
		    .values()) {
		MethodStatsHelper fastestExperiment = methodStats;
		for (ApplicationStatsHelper application : applicationGroups.get(applicationName)) {
		    if (fastestExperiment == null || application.methodsStatsMap.get(methodStats.methodID)
			    .getTotalTime() < fastestExperiment.getTotalTime()) {
			fastestExperiment = application.methodsStatsMap.get(methodStats.methodID);
		    }
		}
		out += fastestExperiment.exportStatsForDataset() + " , alg";// + fastestExperiment.usedAlgorithm + '\n';
	    }
	}
	if (Utils.writeTextFile(targetPath, out))
	    System.out.println("Exported the dataset to " + targetPath);
	else
	    Utils.logErr(getClass(), "Could not export the dataset to " + targetPath);
    }

    private String generateRtffFileHeader() {
	String out = "nb_scfg_locations,  nb_scfg_transitions,  nb_scfg_variables "
		+ "					,  nb_scfg_state_variables,  nb_scfg_input_variables,  nb_scfg_contr_variables,  max_scfg_in_degree,  max_scfg_out_degree";

	out = "@relation LCCvsLCSH\n";
	out += // "@attribute methodID String \n"
	       // + "@attribute scfg_generation_time numeric\n"
		"@attribute   nb_scfg_locations numeric\n" + "@attribute   nb_scfg_transitions numeric\n"
			+ "@attribute   nb_scfg_variables numeric\n" + "@attribute   nb_scfg_state_variables numeric\n"
			+ "@attribute   nb_scfg_input_variables numeric\n"
			+ "@attribute   nb_scfg_contr_variables numeric\n" + "@attribute   max_scfg_in_degree numeric\n"
			+ "@attribute   max_scfg_out_degree numeric\n"
			// + "@attribute summarization String\n"
			// + "@attribute model_instantiation_time numeric\n"
			// + "@attribute synthesis_time numeric\n"
			// + "@attribute triangularization_time numeric\n"
			+ "@attribute   chosenAlgorithm {alg1, alg2, alg3, alg4}\n";
	out += "@data\n";
	return out;
    }
}
