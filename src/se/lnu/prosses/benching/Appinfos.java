package se.lnu.prosses.benching;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import se.lnu.prosses.reporting.SourcesStatsHelper;
import se.lnu.prosses.utils.Utils;

class AppInfos implements java.io.Serializable {
    public final String appDir;
    public final String classesDir;
    public final String[] classpath;
    public String cause4exclusion = null;

    AppInfos(String appDir, String classesDir, String[] classpath) {
	this.appDir = appDir;
	this.classesDir = classesDir;
	this.classpath = classpath;
    }

    public String getName() {
	return new File(this.appDir).getName();
    }

    @Override
    public String toString() {
	return this.getName();
    }

    public void recap() {
	System.out.println("---" + this + "---");
	System.out.println("- appdir: " + appDir);
	System.out.println("- classes: " + classesDir);
	System.out.println("- classpath: " + Arrays.toString(classpath));
	if (this.isExcluded())
	    System.out.println("- cause for exclusion: " + cause4exclusion);
    }

    public void exclude(String cause) {
	this.cause4exclusion = cause;
    }

    public boolean isExcluded() {
	return this.cause4exclusion != null;
    }

    public String causeForExclusion() {
	return this.cause4exclusion;
    }

    public static class FullAppInfos extends AppInfos {
	public final String zipFile;
	public final String baseDir;
	public final SourcesStatsHelper srcStats;

	FullAppInfos(String zipFile, String appDir, String baseDir, String classesDir, String[] classpath,
		SourcesStatsHelper srcStats) {
	    super(appDir, classesDir, classpath);
	    this.zipFile = zipFile;
	    this.baseDir = baseDir;
	    this.srcStats = srcStats;
	}

	@Override
	public void recap() {
	    super.recap();
	    System.out.println("- zip: " + zipFile);
	    System.out.println("- basedir: " + baseDir);
	    srcStats.recap();
	    if (this.isExcluded())
		System.out.println("- cause for exclusion: " + cause4exclusion);
	}

    }

    static void saveAppInfos(File appInfosFile, Map<String, AppInfos> appInfos) throws java.io.IOException {
	java.io.ObjectOutputStream f = new java.io.ObjectOutputStream(new java.io.FileOutputStream(appInfosFile));
	Utils.log(null, "Saving app-specific infos into " + appInfosFile);
	f.writeObject(appInfos);
	f.flush();
	f.close();
    }

    static Map<String, AppInfos> loadAppInfos(File appInfosFile)
	    throws java.io.IOException, java.lang.ClassNotFoundException {
	try {
	    java.io.ObjectInputStream f = new java.io.ObjectInputStream(new java.io.FileInputStream(appInfosFile));
	    Utils.log(null, "Loading app-specific infos from " + appInfosFile);
	    Map<String, AppInfos> c = (Map<String, AppInfos>) f.readObject();
	    f.close();
	    return c;
	} catch (java.io.FileNotFoundException _) {
	    return new HashMap<>();
	}
    }
}
