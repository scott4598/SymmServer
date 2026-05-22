package se.lnu.prosses.reporting;

import java.io.File;
import java.util.HashMap;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import se.lnu.prosses.utils.StringUnicodeEncoderDecoder;
import se.lnu.prosses.utils.Utils;

public class MethodStatsHelper {
    String methodID;
    long scfg_generation_time;
    int nb_scfg_locations;
    int nb_scfg_transitions;
    int nb_scfg_variables;
    int nb_scfg_state_variables;
    int nb_scfg_input_variables;
    int nb_scfg_contr_variables;
    int max_scfg_in_degree;
    int max_scfg_out_degree;
    String summarization;
    long model_instantiation_time;
    long synthesis_time;
    long triangularization_time;
    boolean nb_unsafe_meths;

    String analysisSettings;
    // ApplicationName usedAlgorithm;
    // String guard;
    HashMap<String, String> checkpointGuardMaps = new HashMap<String, String>();
    public long nb_symb_heap_typ_vars;
    public int nb_symb_heap_mut_refs;
    public int nb_symb_heap_const_refs;
    public int nb_symb_heap_mut_rel_vars;
    public int nb_symb_heap_const_rel_vars;
    public String unsat_guard;
    public String true_guard;
    public int support_size;
    public int nb_prims;
    public int nb_refs;
    public int nb_stms;
    public String algoId;

    public String body;
    public boolean isEntryPoint;

    public String exportStats() {
	float total_time = (scfg_generation_time + synthesis_time + triangularization_time);
	return methodID + ";" + algoId + ";" + nb_prims + ";" + nb_refs + ";" + nb_stms + ";" + total_time + ";"
		+ nb_symb_heap_typ_vars + ";" + nb_symb_heap_mut_refs + ";" + nb_symb_heap_const_refs + ";"
		+ nb_symb_heap_mut_rel_vars + ";" + nb_symb_heap_const_rel_vars + ";" + nb_scfg_locations + ";"
		+ nb_scfg_transitions + ";" + nb_scfg_variables + ";" + nb_scfg_state_variables + ";"
		+ nb_scfg_input_variables + ";" + nb_scfg_contr_variables + ";" + max_scfg_in_degree + ";"
		+ max_scfg_out_degree + ";" + model_instantiation_time + ";" + support_size + ";" + scfg_generation_time
		+ ";" + synthesis_time + ";" + triangularization_time + ";" + summarization + ";" + unsat_guard + ";"
		+ true_guard;
    }

    public static String getSheetHeader() {
	return "methodID;algoId;nb_prims;nb_refs;"
		+ "nb_stms;total_time;nb_symb_heap_typ_vars;nb_symb_heap_mut_refs;nb_symb_heap_const_refs;nb_symb_heap_mut_rel_vars;"
		+ "nb_symb_heap_const_rel_vars;nb_scfg_locations;nb_scfg_transitions;nb_scfg_variables;nb_scfg_state_variables;"
		+ "nb_scfg_input_variables;nb_scfg_contr_variables;max_scfg_in_degree;max_scfg_out_degree;model_instantiation_time;"
		+ "support_size;scfg_generation_time;synthesis_time;triangularization_time;summarization;unsat_guard;true_guard\n";
    }

    public String exportStatsForDataset() {
	return nb_scfg_locations + "," + nb_scfg_transitions + "," + nb_scfg_variables + "," + nb_scfg_state_variables
		+ "," + nb_scfg_input_variables + "," + nb_scfg_contr_variables + "," + max_scfg_in_degree + ","
		+ max_scfg_out_degree;
    }

    public long getTotalTime() {
	// TODO Auto-generated method stub
	return model_instantiation_time + synthesis_time + triangularization_time;
    }

    private boolean isBottomPC(String string) {
	return StringUnicodeEncoderDecoder.encodeStringToUnicodeSequence("pc = ⊥")
		.equals(StringUnicodeEncoderDecoder.encodeStringToUnicodeSequence(string.trim()));
    }

    public boolean has_true_guard() {
	return true_guard != null && (true_guard.equals("true"));
	/*
	 * for (String methodPoint : getCheckpointGuardMaps().keySet()) if
	 * (methodPoint.contains("guard")) return
	 * (getCheckpointGuardMaps().get(methodPoint).trim().equals("tt") ||
	 * isBottomPC(getCheckpointGuardMaps().get(methodPoint).trim())); return false;
	 */
    }

    /*
     * public boolean isInsecure() { for (String methodPoint :
     * getCheckpointGuardMaps().keySet()) if (methodPoint.contains("guard")) return
     * getCheckpointGuardMaps().get(methodPoint).trim().equals("ff"); return false;
     * }
     */
    public HashMap<String, String> getCheckpointGuardMaps() {
	return checkpointGuardMaps;
    }

    public void setCheckpointGuardMaps(HashMap<String, String> checkpointGuardMaps) {
	this.checkpointGuardMaps = checkpointGuardMaps;
    }

    String scape_method_name_from_meth_path(String methodPath) {
	methodPath = methodPath.replaceAll("\"", "");
	return new File(methodPath).getName().trim();
    }

    void extract_single_method_stats(String str_single_method_statistics, String analysisSettings,
	    DirectedGraph<String, DefaultEdge> call_graph) {
	String name = scape_method_name_from_meth_path(str_single_method_statistics.split(":")[0]);
	if (name.equals("stats"))
	    return;
	this.analysisSettings = analysisSettings;
	algoId = analysisSettings;
	methodID = name;
	int timeUnit = 1000000;
	// Utils.log(getClass(), method_result);
	if (str_single_method_statistics.contains("infos") && str_single_method_statistics.split("infos:").length > 1) {
	    String[] ImplElements = str_single_method_statistics.split("impl:")[1].split("infos:")[0].split(",");
	    for (String element : ImplElements) {
		// System.out.println(element);
		String lhs = element.split(":")[0].trim().replaceAll("\\{", "");
		String rhs = element.substring(element.indexOf(":") + 1).replaceAll("\\}", "").trim();

		switch (lhs) {
		case "nb_prims":
		    nb_prims = Integer.parseInt(rhs);
		    ;
		    break;
		case "nb_refs":
		    nb_refs = Integer.parseInt(rhs);
		    ;
		    break;
		case "nb_stms":
		    nb_stms = Integer.parseInt(rhs);
		    ;
		    break;
		}
	    }
	    String[] infoElements = str_single_method_statistics.split("infos:")[1].split("stats:")[0].split(",");

	    for (String element : infoElements) {
		// System.out.println(element);
		String lhs = element.split(":")[0].trim().replaceAll("\\{", "");
		String rhs = element.substring(element.indexOf(":") + 1).replaceAll("\\}", "").trim();

		switch (lhs) {
		case "nb_symb_heap_typ_vars":
		    nb_symb_heap_typ_vars = Integer.parseInt(rhs);
		    ;
		    break;
		case "nb_symb_heap_mut_refs":
		    nb_symb_heap_mut_refs = Integer.parseInt(rhs);
		    break;
		case "nb_symb_heap_const_refs":
		    nb_symb_heap_const_refs = Integer.parseInt(rhs);
		    break;
		case "nb_symb_heap_mut_rel_vars":
		    nb_symb_heap_mut_rel_vars = Integer.parseInt(rhs);
		    break;
		case "nb_symb_heap_const_rel_vars":
		    nb_symb_heap_const_rel_vars = Integer.parseInt(rhs);
		    break;
		case "scfg_generation_time":
		    scfg_generation_time = (long) (timeUnit * Float.parseFloat(rhs));
		    break;
		case "nb_scfg_locations":
		    nb_scfg_locations = Integer.parseInt(rhs);
		    break;
		case "nb_scfg_transitions":
		    nb_scfg_transitions = Integer.parseInt(rhs);
		    break;
		case "nb_scfg_variables":
		    nb_scfg_variables = Integer.parseInt(rhs);
		    break;
		case "nb_scfg_state_variables":
		    nb_scfg_state_variables = Integer.parseInt(rhs);
		    break;
		case "nb_scfg_input_variables":
		    nb_scfg_input_variables = Integer.parseInt(rhs);
		    break;
		case "nb_scfg_contr_variables":
		    nb_scfg_contr_variables = Integer.parseInt(rhs);
		    break;
		case "max_scfg_in_degree":
		    max_scfg_in_degree = Integer.parseInt(rhs);
		    break;
		case "max_scfg_out_degree":
		    max_scfg_out_degree = Integer.parseInt(rhs);
		    break;
		case "summarization":
		    summarization = rhs;
		    break;
		case "model_instantiation_time":
		    model_instantiation_time = (long) (timeUnit * Float.parseFloat(rhs));
		    break;
		case "support_size":
		    support_size = Integer.parseInt(rhs);
		    break;
		case "synthesis_time":
		    synthesis_time = (long) (timeUnit * Float.parseFloat(rhs));
		    break;
		case "triangularization_time":
		    triangularization_time = (long) (timeUnit * Float.parseFloat(rhs));
		    break;
		case "unsat_guard":
		    unsat_guard = rhs;
		    break;
		case "true_guard":
		    true_guard = rhs;
		    break;

		}

		if (call_graph.containsVertex(name)) {
		    if (call_graph.inDegreeOf(name) == 0)
			isEntryPoint = true;
		} else
		    Utils.logErr(getClass(), name + " is not in the call graph");
	    }
	}
    }
}
