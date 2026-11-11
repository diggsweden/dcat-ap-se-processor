// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.rdf.validate;

import org.jspecify.annotations.NonNull;

import se.ams.dcatprocessor.specification.DcatCardinality;
import se.ams.dcatprocessor.util.Util;

/**
 * Placeholder for validation errors created when creating DCAT-AP-SE file
 *
 * @author nacbr
 *
 */
public class ValidationError {
	
	/**
	 * Enums for defining type of error
	 */
	public enum ErrorType {
		DUPLICATE_URI_WITHIN_FILE,
		DUPLICATE_URI_BETWEEN_FILES,
		MANDATORY_VALUE_MISSING,
		VALUE_OUTSIDE_OF_SPEC,
		UNKNOWN_KEY,
		UNKNOWN_VALUE,
		ILLEGAL_FORMAT,
		ADDRESS_TO_FEW,
		ADDRESS_TO_MANY,
		EMPTY_URI_VALUE
	}
	
	private final ErrorType errorType;
	
	/**
	 * Filename where the error occurred
	 */
	private final String fileName;
	
	/**
	 * Key where the error occurred
	 */
	private String key;
	
	/**
	 * Value where the error occurred
	 */
	private String value;
	
	/**
	 * Description of the error
	 */
	private final String description;

	/**
	 * Section where the error occurred
	 */
	private String section;
	
	//Predefined errormessages
	private static final String ERROR_DESCRIPTION_DUPLICATE_WITHIN_FILE = "URI: %s exist multiple times in file: %s";
	private static final String ERROR_DESCRIPTION_DUPLICATE_BETWEEN_FILES = "URI: %s exist in the following files: %s";
	private static final String ERROR_DESCRIPTION_ILLEGAL_FORMAT = "The value %s has wrong format for key %s.";
	private static final String ERROR_DESCRIPTION_UNKNOWN_VALUE = "The value %s is not one of the values allowed for key %s.";
	private static final String ERROR_DESCRIPTION_GENERAL = "Key %s with value %s";
	private static final String ERROR_DESCRIPTION_UNKNOWN_KEY = "The key %s does not exist in specification";
	private static final String ERROR_DESCRIPTION_NUMBER_OUTSIDE_SPEC = "The key %s occurs %s times but the allowed range is %s";
	private static final String ERROR_MANDATORY_VALUE_MISSING = "The key %s is mandatory";
	private static final String ERROR_ADDRESS_TO_FEW = "Address has too few values, should contain street-address, postal-code, locality and country-name, separated with ;";
	private static final String ERROR_ADDRESS_TO_MANY = "Address has too many values, should contain street-address, postal-code, locality and country-name, separated with ;";
	private static final String ERROR_EMPTY_URI_VALUE = "The key %s must not be empty";

	private final String UNABLE_ADD_VALIDATION_ERROR_CURRENT_FILE_MISSING = this.getClass() + " : Error validating input data. Reason: Current filename is not set";

	
	/**
	 * Convenience constructor that creates a description of the error
	 * depending on the type of error
	 * 
	 * If the list of filenames contains multiple elements it will be concatenated to one string
	 * 
	 * @param errorType - Type of error
	 * @param fileNames - Filename(s) where the error occurred
	 * @param value - Value where the error occurred
	 * @param section - Section where the error occurred, e.g. Catalog, Dataset
	 */
	public ValidationError(@NonNull ErrorType errorType, @NonNull String[] fileNames, @NonNull String value) {
		this.errorType = errorType;
		this.value = value;
		
		/**
		 * The filename(s) must not be null for ValidationError
		 */
		Util.checkNotNull(fileNames, UNABLE_ADD_VALIDATION_ERROR_CURRENT_FILE_MISSING);
		
		fileName = Util.mergeStringsWithSeparator(fileNames, null);
		this.description = createDescription(errorType, fileName, null, value);
	}
		
	/**
	 * Convenience constructor that creates a description of the error
	 * depending on the type of error
	 * 
	 * @param errorType - Type of error
	 * @param fileName - Filename where the error occurred
	 * @param key - Key where the error occurred, e.g. foaf:homepage, dcterms:issued
	 * @param value - Value where the error occurred
	 * @param section - Section where the error occurred, e.g. Catalog, Dataset
	 */
	public ValidationError(@NonNull ErrorType errorType, @NonNull String fileName, @NonNull String key, @NonNull String value, @NonNull String section) {
		this.errorType = errorType;
		this.fileName = fileName;
		this.key = key;
		this.value = value;
		this.section = section;
		this.description = createDescription(errorType, fileName, key, value);
	}
	
	/**
	 * Convenience constructor to be used when the error is NUMBER_OUTSIDE_SPEC
	 * Creates a description for this particular error
	 * 
	 * @param fileName - Filename where the error occurred
	 * @param key - Key where the error occurred, e.g. foaf:homepage, dcterms:issued
	 * @param value - Value where the error occurred
	 * @param cardinality - Contains the allowed range FYI
	 * @param section - Section where the error occurred, e.g. Catalog, Dataset
	 */
	public ValidationError(@NonNull String fileName, @NonNull String key, @NonNull Integer value, DcatCardinality cardinality, @NonNull String section) {
		this.errorType = ErrorType.VALUE_OUTSIDE_OF_SPEC;
		this.fileName = fileName;
		this.key = key;
		this.value = value.toString();
		this.section = section;
		this.description = ERROR_DESCRIPTION_NUMBER_OUTSIDE_SPEC.formatted(key, value, cardinality.getInterval());
		

	}

	/**
	 * Convenience constructor to be used when the error is UNKNOWN_KEY
	 * Creates a description for this particular error
	 * 
	 * @param fileName - Filename where the error occurred
	 * @param key - Key where the error occurred
	 * @param section - Section where the error occurred, e.g. Catalog, Dataset
	 */
	public ValidationError(@NonNull String fileName, @NonNull String key, @NonNull String section) {
		this.errorType = ErrorType.UNKNOWN_KEY;
		this.fileName = fileName;
		this.key = key;
		this.section = section;
		this.description = ERROR_DESCRIPTION_UNKNOWN_KEY.formatted(key);
	}

	public ErrorType getErrorType() {
		return errorType;
	}

	public String getFileName() {
		return fileName;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public String getSection() {
		return section;
	}

	/**
	 * Creates a description of the validation error depending of the type of error
	 * @param errorType - Type of error
	 * @param fileName - Filename where the error occurred
	 * @param key - Key where the error occurred
	 * @param value - Value where the error occurred
	 * @return - The description
	 */
	private String createDescription(ErrorType errorType, String fileName, String key, String value) {
        return switch (errorType) {
            case ErrorType.ADDRESS_TO_FEW -> ERROR_ADDRESS_TO_FEW.formatted(key);
            case ErrorType.ADDRESS_TO_MANY -> ERROR_ADDRESS_TO_MANY.formatted(key);
            case ErrorType.UNKNOWN_VALUE -> ERROR_DESCRIPTION_UNKNOWN_VALUE.formatted(value, key);
            case ErrorType.MANDATORY_VALUE_MISSING -> ERROR_MANDATORY_VALUE_MISSING.formatted(key);
            case ErrorType.ILLEGAL_FORMAT -> ERROR_DESCRIPTION_ILLEGAL_FORMAT.formatted(value, key);
            case ErrorType.DUPLICATE_URI_WITHIN_FILE -> ERROR_DESCRIPTION_DUPLICATE_WITHIN_FILE.formatted(value, fileName);
            case ErrorType.DUPLICATE_URI_BETWEEN_FILES -> ERROR_DESCRIPTION_DUPLICATE_BETWEEN_FILES.formatted(value, fileName);
			case ErrorType.EMPTY_URI_VALUE -> ERROR_EMPTY_URI_VALUE.formatted(key);
            default -> ERROR_DESCRIPTION_GENERAL.formatted(key, value);
        };
	}
}
