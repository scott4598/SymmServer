package se.lnu.prosses.configs;

public class TypeHandlingConfig {
	public boolean skipMethodsWithIncompatibleTypes;
	public boolean autoFixInconsistentTypes;
	public boolean allowIncompatibleTypes;
	public TypeHandlingConfig (boolean skipMethodsWithIncompatibleTypes,
				   boolean autoFixInconsistentTypes,
				   boolean allowIncompatibleTypes) {
		this.skipMethodsWithIncompatibleTypes = skipMethodsWithIncompatibleTypes;
		this.autoFixInconsistentTypes = autoFixInconsistentTypes;
		this.allowIncompatibleTypes = allowIncompatibleTypes;
	}
}
