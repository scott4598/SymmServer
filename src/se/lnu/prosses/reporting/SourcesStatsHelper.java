package se.lnu.prosses.reporting;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import se.lnu.prosses.utils.Utils;

public class SourcesStatsHelper implements java.io.Serializable {
    public final long javaLOC;

    public SourcesStatsHelper(Path appSrcPath) throws java.io.IOException {
	this.javaLOC = getTotalJavaLOC(appSrcPath);
    }

    public void recap() {
	System.out.println("- LOC (Java): " + javaLOC);
    }

    // ---

    static public long getTotalJavaLOC(Path appSrcPath) throws java.io.IOException {
	long i = 0L;
	for (File f : Utils.getFilesOfTypes(appSrcPath.toString(), new String[] { ".java" })) {
	    try {
		i += Files.lines(f.toPath()).filter(l -> !org.apache.commons.lang3.StringUtils.isBlank(l)).count();
	    } catch (java.io.UncheckedIOException _) {
	    }
	}
	return i;
    }
}
