// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import se.ams.dcatprocessor.models.Catalog;
import se.ams.dcatprocessor.models.DataClass;
import se.ams.dcatprocessor.models.DataService;
import se.ams.dcatprocessor.models.DataSet;
import se.ams.dcatprocessor.models.Distribution;
import se.ams.dcatprocessor.models.FileStorage;
import se.ams.dcatprocessor.models.Organization;
import se.ams.dcatprocessor.rdf.validate.ValidationError;
import se.ams.dcatprocessor.rdf.validate.ValidationError.ErrorType;
import se.ams.dcatprocessor.testutil.TestHelper;

@SpringBootTest
class RDFWorkerTest {
	
	@Autowired
    private ObjectProvider<RDFWorker> rdfWorkerProvider;

	private RDFWorker rdfWorker;
	private Catalog testCatalog1;
	private List<FileStorage> testFileStorageList1;
	private List<FileStorage> testFileStorageList2;
	
	private String catalogFileName = "catalog.json";

	/**
	 * Set the instance of PropertyLoader to null
	 * to force them to re-instansiate since they are Singletons
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */

	@BeforeEach
	void setup() {
		rdfWorker = rdfWorkerProvider.getObject();
		testCatalog1 = createTestCatalog1();
		testFileStorageList1 = createTestFileStorageList1();
		testFileStorageList2 = createTestFileStorageList2();
	}

	/**
	 * Happy tests
	 */
	@Test
	void testThatValidCatalogCreatesDcatFile() {
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
		} catch (Exception e) {
			fail("Unexpected exception when creating a fully functional DCAT-AP-SE with message: " + e.getMessage());
		}
	}


	//Using a different Catalog
	@Test
	void testThatValidCatalogWithMergedFilesCreatesDcatFile() throws Exception{
		testFileStorageList1.addAll(createTestFileStorageList2());
		try {
			rdfWorker.createDcatFile(createTestCatalog2(), testFileStorageList1);
		} catch (Exception e) {
			fail("Unexpected exception when creating a fully functional DCAT-AP-SE with message: " + e.getMessage());
		}
	}
	
	@Test
	void testThatMissingCatalogPublisherThrowsException () {
		testCatalog1.publisher = null;
		try {
			rdfWorker.createDcatFile(testCatalog1, createTestFileStorageList1());
			fail("Expected DCATException when missing Catalog.Publisher");
		} catch (DcatException | IOException e) {
			assertEquals("class se.ams.dcatprocessor.rdf.RDFWorker : Unable to create DCAT. Reason: Catalog.publisher is a mandatory but is missing", e.getMessage());
		}

	}

	@Test
	void testThatMissingDatasetThrowsException() {
		testFileStorageList1.get(0).dcat_dataset = null;
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing Catalog.dcat_dataset");
		} catch (DcatException | IOException e) {
			assertEquals("class se.ams.dcatprocessor.rdf.RDFWorker : Unable to create DCAT. Reason: Catalog.dcat_dataset is a mandatory but is missing", e.getMessage());
		}	
	}

	@Test
	void testThatMissingAboutThrowsException() {
		testCatalog1.about = null;
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing Catalog.about");
		} catch (DcatException | IOException e) {
			assertEquals("class se.ams.dcatprocessor.rdf.RDFWorker : Unable to create DCAT. Reason: Catalog.about is a mandatory but is missing", e.getMessage());
		}
	}
		
	@Test
	void testThatMissingTitleReturnsValidationError() throws Exception {
		String removedKey = "dcterms:title";
		testCatalog1.dcData.remove(removedKey);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing Catalog-dcterms:title");
		} catch (DcatException e) {
			String description = "The key " + removedKey + " occurs 0 times but the allowed range is 1..n";
			TestHelper.assertOneValidationError(e.getValidationResults(), catalogFileName, ErrorType.VALUE_OUTSIDE_OF_SPEC, removedKey, null, description);
		}
	}

	@Test
	void testThatMissingLicenseReturnsValidationError() throws Exception {
		String removedKey = "dcterms:license";
		testCatalog1.dcData.remove(removedKey);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing Distribution.dcat:accessURL");
		} catch (DcatException e) {
			String description = "The key " + removedKey + " occurs 0 times but the allowed range is 1";
			TestHelper.assertOneValidationError(e.getValidationResults(), testCatalog1.fileName, ErrorType.VALUE_OUTSIDE_OF_SPEC, removedKey, null, description);
		}
	}

	@Test
	void testThatMissingPublisherAboutThrowsException() {
		testCatalog1.publisher.about = null;
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing Publisher.about");
		} catch (DcatException | IOException e) {
			assertEquals("class se.ams.dcatprocessor.rdf.RDFWorker : Unable to create DCAT. Reason: Agent.about is a mandatory but is missing", e.getMessage());
		}
	}

	@Test
	void testThatMissingPublisherNameReturnsValidationError() throws Exception {
		String removedKey = "foaf:name";
		testCatalog1.publisher.dcData.remove(removedKey);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing Publisher.foaf:name");
		} catch (DcatException e) {
			String description = "The key " + removedKey + " occurs 0 times but the allowed range is 1..n";
			TestHelper.assertOneValidationError(e.getValidationResults(), testCatalog1.fileName, ErrorType.VALUE_OUTSIDE_OF_SPEC, removedKey, null, description);
		}
	}
	
	@Test
	void testThatMissingDatasetTitleReturnsValidationError () throws Exception {
		String removedKey = "dcterms:title";
		testFileStorageList1.get(0).dcat_dataset.get(0).dcData.remove(removedKey);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing Publisher.foaf:name");
		} catch (DcatException e) {
			String description = "The key " + removedKey + " occurs 0 times but the allowed range is 1..n";
			TestHelper.assertOneValidationError(e.getValidationResults(), testFileStorageList1.get(0).fileName, ErrorType.VALUE_OUTSIDE_OF_SPEC, removedKey, null, description);
		}
	}

	@Test
	void testThatMissingOrganizationAboutThrowsException() {
		testFileStorageList1.get(0).dcat_dataset.get(0).organizations.get(0).about = "";		
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing ContactPoint3.about");
		} catch (DcatException | IOException e) {
			assertEquals("class se.ams.dcatprocessor.rdf.RDFWorker : Unable to create DCAT. Reason: ContactPoint.about is a mandatory but is missing", e.getMessage());
		}
	}
	
	@Test
	void testThatMissingContactPointEmailReturnsValidationError() throws Exception {
		String removedKey = "vcard:hasEmail";
		testFileStorageList1.get(0).dcat_dataset.get(0).organizations.get(1).dcData.remove(removedKey);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing ContactPoint.vcard:hasEmail");
		} catch (DcatException e) {
			String description = "The key " + removedKey + " occurs 0 times but the allowed range is 1..n";
			TestHelper.assertOneValidationError(e.getValidationResults(), testFileStorageList1.get(0).fileName, ErrorType.VALUE_OUTSIDE_OF_SPEC, removedKey, null, description);
		}
	}

	@Test
	void testThatMissingDistributionAccessUrlReturnsValidationError() throws Exception {
		String removedKey = "dcat:accessURL";
		testFileStorageList1.get(0).dcat_dataset.get(0).dcat_distribution.get(0).dcData.remove(removedKey);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing Distribution.dcat:accessURL");
		} catch (DcatException e) {
			String description = "The key " + removedKey + " occurs 0 times but the allowed range is 1";
			TestHelper.assertOneValidationError(e.getValidationResults(), testFileStorageList1.get(0).fileName, ErrorType.VALUE_OUTSIDE_OF_SPEC, removedKey, null, description);
		}
	}

	/**
	 * Test setting values using illegal format
	 * Test adding more values than allowed according to specification
	 */
	@Test
	void testThatMultipleLicensesReturnValidationError() throws Exception {
		String addedKey = "dcterms:license";
		String addedValue = "http://www.apache.org/licenses/LICENSE-2.0";		
		testCatalog1.dcData.put(addedKey, addedValue);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when multiple Catalog-dcterms:license entrys");
		} catch (DcatException e) {
			String description = "The key " + addedKey + " occurs 2 times but the allowed range is 1";
			TestHelper.assertOneValidationError(e.getValidationResults(), testCatalog1.fileName, ErrorType.VALUE_OUTSIDE_OF_SPEC, addedKey, null, description);
		}
	}
	
	@Test
	void testThatMultipleAccessUrlsReturnValidationError() throws Exception {
		String addedKey = "dcat:accessURL";
		String addedValue = "https://example.com/accessURL_extra";		
		testFileStorageList1.get(0).dcat_dataset.get(0).dcat_distribution.get(1).dcData.put(addedKey, addedValue);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when added too many dcat:accessURL");
		} catch (DcatException e) {
			String description = "The key " + addedKey + " occurs 2 times but the allowed range is 1";
			TestHelper.assertOneValidationError(e.getValidationResults(), testFileStorageList1.get(0).fileName, ErrorType.VALUE_OUTSIDE_OF_SPEC, addedKey, null, description);
		}
	}

	@Test
	void testThatDescriptionWithLanguageIsAccepted () {
		//Add description in Azerbadjan
		testCatalog1.dcData.put( "dcterms:description", "aze¤azərbaycan dili");
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
		} catch (DcatException | IOException e) {
			fail("Unexpected exception when adding a dcterms:description in a new language");
		}
	}
	
	/*
	 * Create DCAT with multiple errors and verify the correct ValidationErrors
	 */
	//Duplicate URI in the same file
	@Test
	void testThatDuplicateUriBetweenFilesReturnsValidationError() {
		String duplicateDataSetURI1 = "http://www.dataset_af1000.se";

		testFileStorageList1.get(0).dcat_dataset.get(0).about = duplicateDataSetURI1;

		List<FileStorage> fileStorageList2 = createTestFileStorageList2();
		fileStorageList2.get(0).dcat_dataset.get(1).about = duplicateDataSetURI1;

		testFileStorageList1.addAll(fileStorageList2);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing there is duplicate URI between the files");
		} catch (DcatException | IOException e) {
			assertEquals(e.getClass(), DcatException.class);
			DcatException dcatException = (DcatException) e;

			String fileName = testFileStorageList1.get(0).fileName + "," + testFileStorageList1.get(1).fileName;
			TestHelper.assertOneValidationError(dcatException.getValidationResults(), fileName, ErrorType.DUPLICATE_URI_BETWEEN_FILES,
					null, duplicateDataSetURI1, "URI: " + duplicateDataSetURI1 + " exist in the following files: " + fileName);
		}
	}	
	
	/*
	 * Create DCAT with multiple errors and verify the correct ValidationErrors
	 */	
	//Duplicate URI within and between files 
	@Test
	void testThatDuplicateUriWithinAndBetweenFilesReturnsValidationErrors() {
		String duplicateDataSetURI1 = "http://www.dataset_af1000.se";
		String duplicateDataSetURI2 = "http://www.dataset_af2000.se";

		// Errors between file 1 and 2
		testFileStorageList1.get(0).dcat_dataset.get(0).about = duplicateDataSetURI1;
		testFileStorageList2.get(0).dcat_dataset.get(0).dcat_distribution.get(1).about = duplicateDataSetURI1;

		// Errors within file 2
		testFileStorageList2.get(0).dcat_dataset.get(0).dcat_distribution.get(1).dataServices
				.get(0).about = duplicateDataSetURI2;
		testFileStorageList2.get(0).dcat_dataset.get(0).dcat_distribution.get(1).dataServices
				.get(1).about = duplicateDataSetURI2;

		testFileStorageList1.addAll(testFileStorageList2);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing there is duplicate URI between the files");
		} catch (DcatException | IOException e) {
			assertEquals(e.getClass(), DcatException.class);
			DcatException dcatException = (DcatException) e;
			List<ValidationError> validationErrors = dcatException.getValidationResults();
		
			assertEquals(2, validationErrors.size());
		
			String fileName1 = testFileStorageList1.get(1).fileName;
			String fileName2 = testFileStorageList1.get(0).fileName + "," + testFileStorageList1.get(1).fileName;

			ValidationError withinFileError = TestHelper.findValidationError(validationErrors, ErrorType.DUPLICATE_URI_WITHIN_FILE, duplicateDataSetURI2);
			ValidationError betweenFilesError = TestHelper.findValidationError(validationErrors, ErrorType.DUPLICATE_URI_BETWEEN_FILES, duplicateDataSetURI1);
		
			TestHelper.assertValidationError(withinFileError, fileName1, ErrorType.DUPLICATE_URI_WITHIN_FILE,
				null, duplicateDataSetURI2,
				"URI: " + duplicateDataSetURI2 + " exist multiple times in file: " + fileName1);
			
			TestHelper.assertValidationError(betweenFilesError, fileName2, ErrorType.DUPLICATE_URI_BETWEEN_FILES,
			null, duplicateDataSetURI1,
				"URI: " + duplicateDataSetURI1 + " exist in the following files: " + fileName2);
		}
	}

	/*
	 * Create DCAT with multiple errors and verify the correct ValidationErrors
	 */	
	//Duplicate URI within and between files + type and formaterrors 
	@Test
	void testThatDuplicateUriAndFormatErrorsReturnAllValidationErrors() {
		String duplicateDataSetURI1 = "http://www.dataset_af1000.se";
		String duplicateDataSetURI2 = "http://www.dataset_af2000.se";

		// Errors between file 1 and 2
		testFileStorageList1.get(0).dcat_dataset.get(0).about = duplicateDataSetURI1;
		testFileStorageList2.get(0).dcat_dataset.get(0).dcat_distribution.get(1).about = duplicateDataSetURI1;

		// Errors within file 2
		testFileStorageList2.get(0).dcat_dataset.get(0).dcat_distribution.get(1).dataServices
				.get(0).about = duplicateDataSetURI2;
		testFileStorageList2.get(0).dcat_dataset.get(0).dcat_distribution.get(1).dataServices
				.get(1).about = duplicateDataSetURI2;
		
		//Value not allowed for type and wrong format
		testFileStorageList1.get(0).dcat_dataset.get(1).dcData.put("dcat:theme", "Invalid URI");
		testFileStorageList2.get(0).dcat_dataset.get(0).dcData.put("dcterms:issued", "1277273");
		
		testFileStorageList1.addAll(testFileStorageList2);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when missing there is duplicate URI between the files");
		} catch (DcatException | IOException e) {
			assertEquals(e.getClass(), DcatException.class);
			DcatException dcatException = (DcatException) e;
			List<ValidationError> validationErrors = dcatException.getValidationResults();

			assertEquals(5, validationErrors.size());

			String fileName1 = testFileStorageList1.get(0).fileName;
			String fileName2 = testFileStorageList1.get(1).fileName;
			String fileName1_2 = fileName1 + "," + fileName2;

			// Test ValidationErrors for file 1
			ValidationError themeError = TestHelper.findValidationError(validationErrors, ErrorType.UNKNOWN_VALUE, "Invalid URI");
			TestHelper.assertValidationError(themeError, fileName1, ErrorType.UNKNOWN_VALUE, "dcat:theme", "Invalid URI",
					"The value Invalid URI is not one of the values allowed for key dcat:theme.");

			// Test ValidationErrors for file 2
			ValidationError cardinalityError = TestHelper.findValidationError(validationErrors, ErrorType.VALUE_OUTSIDE_OF_SPEC, "2");
			TestHelper.assertValidationError(cardinalityError, fileName2, ErrorType.VALUE_OUTSIDE_OF_SPEC, "dcterms:issued", "2",
					"The key dcterms:issued occurs 2 times but the allowed range is 0..1");

			ValidationError formatError = TestHelper.findValidationError(validationErrors, ErrorType.ILLEGAL_FORMAT, "1277273");
			TestHelper.assertValidationError(formatError, fileName2, ErrorType.ILLEGAL_FORMAT, "dcterms:issued", "1277273",
					"The value 1277273 has wrong format for key dcterms:issued.");

			ValidationError duplicateWithinError = TestHelper.findValidationError(validationErrors, ErrorType.DUPLICATE_URI_WITHIN_FILE, duplicateDataSetURI2);
			TestHelper.assertValidationError(duplicateWithinError, fileName2, ErrorType.DUPLICATE_URI_WITHIN_FILE, null, duplicateDataSetURI2,
					"URI: " + duplicateDataSetURI2 + " exist multiple times in file: " + fileName2);

			// Test ValidationErrors for file 1 and 2
			ValidationError duplicateBetweenError = TestHelper.findValidationError(validationErrors, ErrorType.DUPLICATE_URI_BETWEEN_FILES, duplicateDataSetURI1);
			TestHelper.assertValidationError(duplicateBetweenError, fileName1_2, ErrorType.DUPLICATE_URI_BETWEEN_FILES, null, duplicateDataSetURI1,
					"URI: " + duplicateDataSetURI1 + " exist in the following files: " + fileName1_2);
		}
	}
	
	@Test
	void testThatUnknownKeyReturnsValidationError() throws Exception {
		String madeUpKey = "madeup:valuenotinspec";
		String value = "foo";
		testCatalog1.dcData.put(madeUpKey, value);
		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when using a key not in specification");
		} catch (DcatException e) {
			String description = "The key " + madeUpKey + " does not exist in specification";
			TestHelper.assertOneValidationError(e.getValidationResults(), catalogFileName, ErrorType.UNKNOWN_KEY, madeUpKey, null, description);
		}
	}

	@Test
	void testThatHvdCategoryIsNotRequiredWithoutTheHvdLegislation() {
		DataSet dataSet = testFileStorageList1.get(0).dcat_dataset.get(0);
		dataSet.dcData.put("dcatap:applicableLegislation", "http://data.europa.eu/eli/dir/2019/1024/oj");

		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
		} catch (Exception e) {
			fail("hvdCategory must not be required for legislation other than the high value data regulation: " + e.getMessage());
		}
	}

	@Test
	void testThatHvdCategoryIsRequiredUnderTheHvdLegislation() throws Exception {
		String removedKey = "dcatap:hvdCategory";
		DataSet dataSet = testFileStorageList1.get(0).dcat_dataset.get(0);
		dataSet.dcData.put("dcatap:applicableLegislation", "http://data.europa.eu/eli/reg_impl/2023/138/oj");

		try {
			rdfWorker.createDcatFile(testCatalog1, testFileStorageList1);
			fail("Expected DCATException when hvdCategory is missing under the high value data regulation");
		} catch (DcatException e) {
			String description = "The key " + removedKey + " occurs 0 times but the allowed range is 1..n";
			TestHelper.assertOneValidationError(e.getValidationResults(), testFileStorageList1.get(0).fileName, ErrorType.VALUE_OUTSIDE_OF_SPEC, removedKey, null, description);
		}
	}

	@Test
	void testThatNestedNodeErrorSectionIncludesParent() {
		DataClass temporal = testFileStorageList1.get(0).dcat_dataset.get(1).temporals.get(0);
		temporal.dcData.remove("dcat:startDate");
		temporal.dcData.put("dcat:startDate", "2021-03-16abc");

		DcatException exception = assertThrows(DcatException.class,
			() -> rdfWorker.createDcatFile(testCatalog1, testFileStorageList1));

		List<ValidationError> errors = exception.getValidationResults();
		assertEquals(1, errors.size());
		assertEquals("Dataset.PeriodOfTime", errors.get(0).getSection());
	}

	@Test
	void testThatDeeplyNestedRightsErrorSectionIncludesFullPath() {
		Distribution distribution = testFileStorageList1.get(0).dcat_dataset.get(0).dcat_distribution.get(0);
		distribution.rights.dcData.remove("odrs:attributionURL");
		distribution.rights.dcData.put("odrs:attributionURL", "not-a-url");

		DcatException exception = assertThrows(DcatException.class,
			() -> rdfWorker.createDcatFile(testCatalog1, testFileStorageList1));

		List<ValidationError> errors = exception.getValidationResults();
		assertEquals(1, errors.size());
		assertEquals("Dataset.Distribution.RightsStatement", errors.get(0).getSection());
	}

	@Test
	void testThatOrganizationPhoneErrorSectionIncludesFullPath() {
		DataClass phone = testFileStorageList1.get(0).dcat_dataset.get(0).organizations.get(1).phone.get(0);
		phone.dcData.remove("vcard:hasValue");
		phone.dcData.put("vcard:hasValue", "tel:abc");
		DcatException exception = assertThrows(DcatException.class,
			() -> rdfWorker.createDcatFile(testCatalog1, testFileStorageList1));

		List<ValidationError> errors = exception.getValidationResults();
		assertEquals(1, errors.size());
		assertEquals("Dataset.Organization.Voice", errors.get(0).getSection());
	}

	@Test
	void testThatDistributionErrorSectionIncludesDataset() {
		Distribution distribution = testFileStorageList1.get(0).dcat_dataset.get(0).dcat_distribution.get(0);
		distribution.dcData.remove("dcat:accessURL");
		distribution.dcData.put("dcat:accessURL", "not-a-url");

		DcatException exception = assertThrows(DcatException.class,
			() -> rdfWorker.createDcatFile(testCatalog1, testFileStorageList1));

		List<ValidationError> errors = exception.getValidationResults();
		assertEquals(1, errors.size());
		assertEquals("Dataset.Distribution", errors.get(0).getSection());
	}

	@Test
	void testThatAgentErrorSectionIncludesParent() {
		testCatalog1.publisher.dcData.put("foaf:homepage", "not-a-url");

		DcatException exception = assertThrows(DcatException.class,
			() -> rdfWorker.createDcatFile(testCatalog1, testFileStorageList1));

		ValidationError error = TestHelper.findValidationError(exception.getValidationResults(), ErrorType.ILLEGAL_FORMAT, "not-a-url");
		assertEquals("Catalog.Agent", error.getSection());
	}

	@Test
	void testThatCardinalityErrorSectionIncludesParent() {
		testFileStorageList1.get(0).dcat_dataset.get(0).dcat_distribution.get(0).dcData.remove("dcat:accessURL");

		DcatException exception = assertThrows(DcatException.class,
			() -> rdfWorker.createDcatFile(testCatalog1, testFileStorageList1));

		List<ValidationError> errors = exception.getValidationResults();
		assertEquals(1, errors.size());
		assertEquals("Dataset.Distribution", errors.get(0).getSection());
	}

	/**
	 * @return
	 */
	private Catalog createTestCatalog1() {
		Catalog catalog = new Catalog();
		catalog.fileName = catalogFileName;
		catalog.about = "http://www.foo";
		catalog.dcData.put("dcterms:title", "Katalogtitel");
		catalog.dcData.putAll( "dcterms:description", List.of("sv¤Katalogbeskrivning_1", "en¤Katalogbeskrivning_2"));
		catalog.dcData.putAll( "dcterms:license", List.of("http://creativecommons.org/publicdomain/zero/1.0/"));
		catalog.dcData.put( "foaf:homepage", "http://www.arbetsformedl.se");
		catalog.dcData.put( "dcat:themeTaxonomy", "http://publications.europa.eu/resource/authority/data-theme");
		
		//Add rights
		DataClass license1 = createDataClass("http://wwww.licenseURI_1.com", List.of("dcterms:title", "dcterms:description"),List.of("en¤License title 5", "en¤License description 5"));
		DataClass license2 = createDataClass("http://wwww.licenseURI_2.com", List.of("dcterms:title", "dcterms:description"), List.of("en¤License title 6", "en¤License description 6"));

		DataClass copyrightHolder1 = createDataClass("http://www.af22457834.se", List.of("foaf:name", "dcterms:type"), List.of("Aktör namn 1", "http://purl.org/adms/publishertype/Company"));	
		DataClass copyrightHolder2 = createDataClass("http://www.af22457998.se", List.of("foaf:name", "dcterms:type"), List.of("Aktör namn 2", "http://purl.org/adms/publishertype/NationalAuthority"));
		
		catalog.rights = createRights(List.of(copyrightHolder1, copyrightHolder2), List.of(license1, license2),
				List.of("odrs:attributionText", "odrs:attributionURL", "odrs:copyrightNotice", "odrs:copyrightYear", "odrs:jurisdiction", "odrs:reuserGuidelines"),
				List.of("Erkännandetext 1", "http://www.attributionURL1.com", "Meddelande om upphovsrätt 1", "2021", "http://www.jurisdictionURL1.com", "http://www.reuserGuidelinesURL1.com"));
				
		//Add geographic area
		DataClass spatial = createSpatial();
		catalog.spatial.add(spatial);

		DataClass agent = new DataClass();
		agent.about = "https://example.com/publisher1";
		agent.dcData.putAll("foaf:name", List.of("Exampel organization 1", "Exampel organization 2"));
		agent.dcData.put("dcterms:type", "http://purl.org/adms/publishertype/LocalAuthority/");
		catalog.publisher = agent;
		
		return catalog;
	}
	
	private List<FileStorage> createTestFileStorageList1() {
		
		FileStorage fileStorage = new FileStorage();
		
		fileStorage.fileName = "apispecifikation1.raml";

		DataSet dataSet1 = new DataSet();
		dataSet1.about = "https://example.com/dataset1";
		dataSet1.dcData.put( "dcterms:title", "en¤Dataset title");
		dataSet1.dcData.putAll( "dcterms:description", List.of("bh¤भोजपुरी", "en¤Dataset description1"));
		dataSet1.dcData.putAll( "dcat:keyword", List.of("bh¤भोजपुरी", "en¤Dataset keyword", "am¤অসমীয়া"));
		dataSet1.dcData.put( "dcat:distribution", "https://example.com/distribution1");
		dataSet1.dcData.put( "dcat:contactPoint", "https://example.com/contactpoint1");
		dataSet1.dcData.put( "dcterms:issued", "2012");
		dataSet1.dcData.put( "dcterms:spatial", "http://sws.geonames.org/6695072");
		dataSet1.dcData.put( "dcat:landingPage", "http://www.landingpage1.org");
		dataSet1.dcData.put( "adms:identifier", "Identifierare 1");
		dataSet1.dcData.put( "adms:identifier", "Identifierare 2");

		//Add documents
		dataSet1.documents.add(createDataClass("https://documentlink_1.com", List.of("dcterms:title", "dcterms:description"), List.of("en¤Document title 1", "sv¤Document beskrivning 1")));
		
		//Add Standard
		dataSet1.conformsTo.add(createDataClass("https://conformsToLink_1.com", List.of("dcterms:title", "dcterms:description"), List.of("en¤Conforms to title 1", "sv¤Conforms to beskrivning 1")));
		
		fileStorage.dcat_dataset.add(dataSet1);
			
		DataSet dataSet2 = new DataSet();
		dataSet2.about = "https://example.com/dataset2";
		dataSet2.dcData.put( "dcterms:title", "en¤Dataset2 title");
		dataSet2.dcData.putAll( "dcterms:description", List.of("en¤Dataset description2_1", "en¤Dataset description2_2"));
		dataSet2.dcData.put( "dcat:distribution", "https://example.com/distribution2");
		dataSet2.dcData.put( "dcat:contactPoint", "https://example.com/contactpoint2");
		dataSet2.dcData.put( "dcat:theme", "http://publications.europa.eu/resource/authority/data-theme/ENER");
		dataSet2.dcData.put( "dcat:theme", "http://publications.europa.eu/resource/authority/data-theme/AGRI");
		dataSet2.dcData.put( "dcterms:accessRights", "http://publications.europa.eu/resource/authority/access-right/RESTRICTED");
		dataSet2.dcData.put( "dcterms:modified", "2009-05-30T09:00:00");
		dataSet2.dcData.put( "dcterms:identifier", "Identifier 1");
		
		// Add Övrig aktör
		dataSet2.otherAgents
				.add(createOtherAgent(List.of("http://www.af22457895.se", "foaf:name", "dcterms:type", "dcat:hadRole"),
						List.of("http://www.af22457895.se", "Övrig agent namn 1",
								"http://purl.org/adms/publishertype/NonProfitOrganisation",
								"http://inspire.ec.europa.eu/metadata-codelist/ResponsiblePartyRole/distributor")));
		
		//Add temporal
		DataClass temporal = new DataClass();
		temporal.dcData.put("dcat:startDate", "2021-03-26");
		temporal.dcData.put("dcat:endDate", "2022-02-26");
		dataSet2.temporals.add(temporal);

		//Add geographic area
		DataClass spatial = createSpatial();
		dataSet2.spatial.add(spatial);

		fileStorage.dcat_dataset.add(dataSet2);
		
		Organization org1 = new Organization();
		org1.about = "https://example.com/contactpoint1";
		org1.dcData.put("rdf:type", "http://www.w3.org/2006/vcard/ns#Organization");
		org1.dcData.put("vcard:fn", "Arne Andersson1");
		org1.dcData.put("vcard:hasEmail", "mailto:open@skovde.se");
		DataClass adress = new DataClass();
		adress.dcData.put("vcard:street-address", "Arbetsvägen 1");
		adress.dcData.put("vcard:postal-code", "95200");
		adress.dcData.put("vcard:locality", "Arbetsstad1");
		adress.dcData.put("vcard:country-name", "Arbetarland1");
		org1.adress.add(adress);
		adress = new DataClass();
		adress.dcData.put("vcard:street-address", "Arbetsvägen 2");
		adress.dcData.put("vcard:postal-code", "95200");
		adress.dcData.put("vcard:locality", "Arbetsstad2");
		adress.dcData.put("vcard:country-name", "Arbetarland2");
		org1.adress.add(adress);
		
		dataSet1.organizations.add(org1);

		Organization org2 = new Organization();
		org2.about = "https://example.com/contactpoint2";
		org2.dcData.put("rdf:type", "http://www.w3.org/2006/vcard/ns#Organization");
		org2.dcData.put("vcard:fn", "Arne Andersson2");
		org2.dcData.put("vcard:hasEmail", "mailto:open@skovde.se");
		DataClass phone = new DataClass();
		phone.dcData.put("vcard:hasValue", "tel:+46771416416");
		org2.phone.add(phone);
		phone = new DataClass();
		phone.dcData.put("vcard:hasValue", "tel:+46771417417");
		org2.phone.add(phone);
		dataSet1.organizations.add(org2);
		
		
		Distribution distribution1 = new Distribution();
		distribution1.about = "https://example.com/distribution1";
		distribution1.dcData.put("dcterms:title", "en¤Distribution title 1");
		distribution1.dcData.put("dcterms:description", "oeo¤Црна Гора 1");
		distribution1.dcData.put("dcat:accessURL", "https://example.com/accessURL1");
		distribution1.dcData.put("dcterms:format", "text/csv+zip");
		distribution1.dcData.put("dcatap:availability", "http://data.europa.eu/r5r/availability/experimental");
		
		//Add documents
		distribution1.documents.add(createDataClass("https://documentlink_2.com", List.of("dcterms:title", "dcterms:description"), List.of("en¤Document title 2", "sv¤Document beskrivning 2")));

		
		//Add rights
		DataClass license1 = createDataClass("http://wwww.licenseURI_3.com", List.of("dcterms:title", "dcterms:description"),List.of("en¤License title 7", "en¤License description 7"));
		DataClass license2 = createDataClass("http://wwww.licenseURI_4.com", List.of("dcterms:title", "dcterms:description"), List.of("rom¤rromani ćhib 8", "en¤License description 8"));

		DataClass copyrightHolder1 = createDataClass("http://www.af22457101.se", List.of("foaf:name", "dcterms:type"), List.of("Aktör namn 3", "http://purl.org/adms/publishertype/SupraNationalAuthority"));	
		DataClass copyrightHolder2 = createDataClass("http://www.af22457032.se", List.of("foaf:name", "dcterms:type"), List.of("Aktör namn 4", "http://purl.org/adms/publishertype/StandardisationBody"));
				
		distribution1.rights = createRights(List.of(copyrightHolder1, copyrightHolder2), List.of(license1, license2),
				List.of("odrs:attributionText", "odrs:attributionURL", "odrs:copyrightNotice", "odrs:copyrightYear", "odrs:jurisdiction", "odrs:reuserGuidelines"),
				List.of("Erkännandetext 2", "http://www.attributionURL2.com", "Meddelande om upphovsrätt 2", "2099", "http://www.jurisdictionURL2.com", "http://www.reuserGuidelinesURL2.com"));
						
		

		//Add 2 license documents
		distribution1.licenseDocuments.add(createDataClass("http://wwww.licenseURI_5.com", List.of("dcterms:title", "dcterms:description"), List.of("en¤License title 1", "en¤License description 1")));
		distribution1.licenseDocuments.add(createDataClass("http://wwww.licenseURI_6.com", List.of("dcterms:title", "dcterms:description"), List.of("en¤License title 2", "en¤License description 2")));		
		
		dataSet1.dcat_distribution.add(distribution1);
		
		Distribution distribution2 = new Distribution();
		distribution2.about = "https://example.com/distribution2";
		distribution2.dcData.put("dcterms:title", "en¤Distribution title 2");
		distribution2.dcData.put("dcterms:description", "en¤Distribution description 2");
		distribution2.dcData.put("dcat:accessURL", "https://example.com/accessURL2");
		distribution2.dcData.put("dcterms:format", "text/egedefinierad-mediatyp");
		
		//Add Standard
		distribution2.conformsTo.add(createDataClass("https://conformsToLink_2.com", List.of("dcterms:title", "dcterms:description"), List.of("en¤Conforms to title 2", "sv¤Conforms to beskrivning 2")));

		dataSet1.dcat_distribution.add(distribution2);
		
		DataClass offer = new DataClass();
		offer.dcData.put("schema:description", "en¤Offer description 2");
		offer.dcData.put("schema:mainEntityOfPage", "https://schema.com/mainEntityOfPage");
		dataSet1.offers.add(offer);
		
		DataService dataService1 = new DataService();
		dataService1.about = "https://example.com/dataservice1";
		dataService1.dcData.put("dcterms:title", "is¤Dataservice titulur 1");
		dataService1.dcData.put("dcterms:description", "en¤Dataservice description 1");
		dataService1.dcData.put("dcat:endpointURL", "https://example.com/endpointURL1");
		dataService1.dcData.put("dcterms:type", "http://www.wikidata.org/entity/Q749568");
		dataService1.dcData.putAll( "dcat:keyword", List.of("ba¤башҡорт теле", "zn¤中文 (Zhōngwén) 汉语 漢語", "cr¤ᓀᐦᐃᔭᐍᐏᐣ"));

		//Add Standard
		dataService1.conformsTo.add(createDataClass("https://conformsToLink_3.com", List.of("dcterms:title", "dcterms:description"), List.of("en¤Conforms to title 3", "sv¤Conforms to beskrivning 3")));

		
		distribution1.dataServices.add(dataService1);

		DataService dataService2 = new DataService();
		dataService2.about = "https://example.com/dataservice2";
		dataService2.dcData.put("dcterms:title", "en¤Dataservice title 2");
		dataService2.dcData.put("dcterms:description", "is¤Dataservisur beskrivningur 2");
		dataService2.dcData.put("dcat:endpointURL", "https://example.com/endpointURL2");
		dataService2.dcData.put("dcat:endpointDescription", "https://example.com/endpointDescriptionURL1");
		dataService2.dcData.put("dcterms:license", "http://creativecommons.org/licenses/by-sa/4.0/");

		//Add documents
		dataService2.documents.add(createDataClass("https://documentlink_3.com", List.of("dcterms:title", "dcterms:description"), List.of("en¤Document title 3", "sv¤Document beskrivning 3")));

		distribution2.dataServices.add(dataService2);
		
		DataService dataService3 = new DataService();
		dataService3.about = "https://example.com/dataservice3";
		dataService3.dcData.put("dcterms:title", "en¤Dataservice title 3");
		dataService3.dcData.put("dcterms:description", "sv¤Dataservice beskrivning 3");
		dataService3.dcData.put("dcat:endpointURL", "https://example.com/endpointURL3");
		dataService3.dcData.put("dcterms:accessRights", "http://publications.europa.eu/resource/authority/access-right/NON_PUBLIC");
		dataService3.dcData.put( "dcat:landingPage", "http://www.landingpage2.org");
		dataService3.dcData.put("dcterms:type", "http://www.wikidata.org/entity/Q62270");
		dataService3.dcData.putAll( "dcat:keyword", List.of("ba¤башҡорт теле", "cs¤čeština, český jazyk", "he¤עברית"));
		
		//Add 2 license documents
		dataService3.licenseDocuments.add(createDataClass("http://wwww.licenseURI_7.com", List.of("dcterms:title", "dcterms:description"), List.of("is¤License titulur 3", "is¤Licensur beskrivningur 3")));
		dataService3.licenseDocuments.add(createDataClass("http://wwww.licenseURI_8.com", List.of("dcterms:title", "dcterms:description"), List.of("ba¤башҡорт теле 4", "ba¤башҡорт ҡорт баш теле 4")));	

		distribution2.dataServices.add(dataService3);
		
		Organization org3 = new Organization();
		org3.about = "https://example.com/contactpoint3";
		org3.dcData.put("rdf:type", "http://www.w3.org/2006/vcard/ns#Organization");
		org3.dcData.put("vcard:fn", "Arne Andersson3");
		org3.dcData.put("vcard:hasEmail", "mailto:open@skovde.se");
		dataSet2.organizations.add(org3);
		
		Organization org4 = new Organization();
		org4.about = "https://example.com/contactpoint4";
		org4.dcData.put("rdf:type", "http://www.w3.org/2006/vcard/ns#Organization");
		org4.dcData.put("vcard:fn", "Arne Andersson4");
		org4.dcData.put("vcard:hasEmail", "mailto:open@skovde.se");
		dataService3.organizations.add(org4);
		
		Organization org5 = new Organization();
		org5.about = "https://example.com/contactpoint5";
		org5.dcData.put("rdf:type", "http://www.w3.org/2006/vcard/ns#Organization");
		org5.dcData.put("vcard:fn", "Arne Andersson5");
		org5.dcData.put("vcard:hasEmail", "mailto:open@skovde.se");
		dataService3.organizations.add(org5);

		DataClass agent2 = new DataClass();
		agent2.about = "https://example.com/publisher2";
		agent2.dcData.putAll("foaf:name", List.of("Exampel organization", "Exampel organization 2"));
		agent2.dcData.put("dcterms:type", "http://purl.org/adms/publishertype/LocalAuthority/");
		dataService1.agents.add(agent2);

		DataClass agent3 = new DataClass();
		agent3.about = "https://example.com/publisher3";
		agent3.dcData.putAll("foaf:name", List.of("Exampel organization", "Exampel organization 3"));
		agent3.dcData.put("dcterms:type", "http://purl.org/adms/publishertype/LocalAuthority/");
		dataService1.agents.add(agent3);
		
		List<FileStorage> fileStorageList = new ArrayList<FileStorage>();
		fileStorageList.add(fileStorage);
		return fileStorageList;
	}

	private Catalog createTestCatalog2() {
		Catalog catalog = new Catalog();
		catalog.fileName = catalogFileName;
		catalog.about = "http://www.foo";
		catalog.dcData.put("dcterms:title", "Katalogtitel");
		catalog.dcData.putAll( "dcterms:description", List.of("sv¤Katalogeskrivning_1", "en¤Catalog description2"));
		catalog.dcData.putAll( "dcterms:license", List.of("http://creativecommons.org/publicdomain/zero/1.0/"));
		catalog.dcData.put("dcterms:issued", "2021-03-10T12:04:00");

		DataClass agent = new DataClass();
		agent.about = "https://example.com/publisher1";
		agent.dcData.putAll("foaf:name", List.of("Exampel organization 1", "Exampel organization 2"));
		agent.dcData.put("dcterms:type", "http://purl.org/adms/publishertype/LocalAuthority/");
		catalog.publisher = agent;
		return catalog;
	}
	
	private List<FileStorage> createTestFileStorageList2() {
		
		FileStorage fileStorage = new FileStorage();
		
		fileStorage.fileName = "apispecifikation2.raml";
	
		DataSet dataSet1 = new DataSet();
		dataSet1.about = "https://example.com/dataset101";
		dataSet1.dcData.put( "dcterms:title", "en¤Dataset title");
		dataSet1.dcData.putAll( "dcterms:description", List.of("bh¤भोजपुरी", "en¤Dataset description1"));
		dataSet1.dcData.put( "dcat:distribution", "https://example.com/distribution101");
		dataSet1.dcData.put( "dcat:contactPoint", "https://example.com/contactpoint101");
		dataSet1.dcData.put("dcterms:issued", "2021-03-10");
		dataSet1.dcData.put("dcat:temporalResolution", "P5Y2M10D");
		dataSet1.dcData.put("adms:versionNotes", "Version notes 1");
		dataSet1.dcData.put("dcterms:isReferencedBy", "http://purl.org/dc/terms/isReferencedBy1");
		dataSet1.dcData.put("dcterms:isReferencedBy", "http://purl.org/dc/terms/isReferencedBy2");
		dataSet1.dcData.put("dcterms:relation", "http://purl.org/dc/terms/relation1");
		dataSet1.dcData.put("dcterms:relation", "http://purl.org/dc/terms/relation2");
		dataSet1.dcData.put("dcterms:accrualPeriodicity", "http://publications.europa.eu/resource/authority/frequency/ANNUAL_3");
		
		//Add 2 qualified relations
		dataSet1.qualifiedRelations.add(createDataClass(null, List.of("dcat:hadRole", "dcterms:relation"),
        	List.of("https://www.dataportal.se/terminology/resource-role/visualization-image","https://relation1.com")));
		
		dataSet1.qualifiedRelations.add(createDataClass(null, List.of("dcat:hadRole", "dcterms:relation"),
        	List.of("https://www.dataportal.se/terminology/resource-role/image", "https://relation2.com")));


		// dataSet1.qualifiedRelations.add(createDataClass(null, List.of("dcat:hadRole", "dcterms:relation"), List.of("https://role1.com", "https://relation1.com")));
		// dataSet1.qualifiedRelations.add(createDataClass(null, List.of("dcat:hadRole", "dcterms:relation"), List.of("https://role2.com", "https://relation2.com")));
		
		fileStorage.dcat_dataset.add(dataSet1);
		
		DataSet dataSet2 = new DataSet();
		dataSet2.about = "https://example.com/dataset102";
		dataSet2.dcData.put( "dcterms:title", "en¤Dataset2 title");
		dataSet2.dcData.putAll( "dcterms:description", List.of("en¤Dataset description2_1", "en¤Dataset description2_2"));
		dataSet2.dcData.put( "dcat:distribution", "https://example.com/distribution102");
		dataSet2.dcData.put( "dcat:contactPoint", "https://example.com/contactpoint102");
		dataSet2.dcData.put("dcterms:issued", "1989");
		dataSet2.dcData.put("dcat:spatialResolutionInMeters", "10.134");
		dataSet2.dcData.put("dcterms:isReferencedBy", "http://purl.org/dc/terms/isReferencedBy3");
		dataSet2.dcData.put("dcterms:relation", "http://purl.org/dc/terms/relation3");
		
		//Add provenance
		dataSet2.provenances.add(createDataClass(null, List.of("dcterms:description"), List.of("Provenance description 1")));
		
		//Add 1 qualified relation
		dataSet2.qualifiedRelations.add(createDataClass(null, List.of("dcat:hadRole", "dcterms:relation"), List.of("https://www.dataportal.se/concepts/resource-role/visualization-image", "https://www.dataportal.se/concepts/resource-role/example-data")));

		fileStorage.dcat_dataset.add(dataSet2);
		
		//Add some crosslinks between datasets
		dataSet1.dcData.put("dcterms:source", dataSet2.about);
		dataSet2.dcData.put("dcat:hasVersion", dataSet1.about);
		
		Organization org1 = new Organization();
		org1.about = "https://example.com/contactpoint101";
		org1.dcData.put("rdf:type", "http://www.w3.org/2006/vcard/ns#Organization");
		org1.dcData.put("vcard:fn", "Arne Andersson1");
		org1.dcData.put("vcard:hasEmail", "mailto:open@skovde.se");
		dataSet1.organizations.add(org1);

		Organization org2 = new Organization();
		org2.about = "https://example.com/contactpoint102";
		org2.dcData.put("rdf:type", "http://www.w3.org/2006/vcard/ns#Organization");
		org2.dcData.put("vcard:fn", "Arne Andersson2");
		org2.dcData.put("vcard:hasEmail", "mailto:open@skovde.se");
		dataSet1.organizations.add(org2);
		
		Distribution distribution1 = new Distribution();
		distribution1.about = "https://example.com/distribution101";
		distribution1.dcData.put("dcterms:title", "en¤Distribution title 1");
		distribution1.dcData.put("dcterms:description", "sv¤Distribution beskrivning 1");
		distribution1.dcData.put("dcat:accessURL", "https://example.com/accessURL101");
		distribution1.dcData.put("dcat:spatialResolutionInMeters", "0.001");
		distribution1.dcData.put("adms:status", "http://publications.europa.eu/resource/authority/distribution-status/DEVELOP");
	
		dataSet1.dcat_distribution.add(distribution1);
		
		Distribution distribution2 = new Distribution();
		distribution2.about = "https://example.com/distribution102";
		distribution2.dcData.put("dcterms:title", "en¤Distribution title 2");
		distribution2.dcData.put("dcterms:description", "en¤Distribution description 2");
		distribution2.dcData.put("dcat:accessURL", "https://example.com/accessURL102");
		distribution2.dcData.put("dcat:temporalResolution", "P1Y");
		distribution2.dcData.put("dcat:byteSize", "65536");
		
		//Add checksum
		DataClass checkSum = new DataClass();
		checkSum.dcData.put("spdx:checksumValue", "Checksum value 1");
		checkSum.dcData.put("spdx:algorithm", "http://spdx.org/rdf/terms#checksumAlgorithm_sha224");
		distribution2.checksum = checkSum;
		
		dataSet1.dcat_distribution.add(distribution2);
		
		DataService dataService1 = new DataService();
		dataService1.about = "https://example.com/dataservice101";
		dataService1.dcData.put("dcterms:title", "is¤Dataservice titulur 1");
		dataService1.dcData.put("dcterms:description", "en¤Dataservice description 1");
		dataService1.dcData.put("dcat:endpointURL", "https://example.com/endpointURL101");
		distribution1.dataServices.add(dataService1);

		DataService dataService2 = new DataService();
		dataService2.about = "https://example.com/dataservice102";
		dataService2.dcData.put("dcterms:title", "en¤Dataservice title 2");
		dataService2.dcData.put("dcterms:description", "is¤Dataservisur beskrivningur 2");
		dataService2.dcData.put("dcat:endpointURL", "https://example.com/endpointURL102");
		distribution2.dataServices.add(dataService2);
		
		DataService dataService3 = new DataService();
		dataService3.about = "https://example.com/dataservice103";
		dataService3.dcData.put("dcterms:title", "en¤Dataservice title 3");
		dataService3.dcData.put("dcterms:description", "sv¤Dataservice beskrivning 3");
		dataService3.dcData.put("dcat:endpointURL", "https://example.com/endpointURL103");
		distribution2.dataServices.add(dataService3);
		
		Organization org3 = new Organization();
		org3.about = "https://example.com/contactpoint103";
		org3.dcData.put("rdf:type", "http://www.w3.org/2006/vcard/ns#Organization");
		org3.dcData.put("vcard:fn", "Arne Andersson3");
		org3.dcData.put("vcard:hasEmail", "mailto:open@skovde.se");
		dataSet2.organizations.add(org3);
		
		Organization org4 = new Organization();
		org4.about = "https://example.com/contactpoint104";
		org4.dcData.put("rdf:type", "http://www.w3.org/2006/vcard/ns#Organization");
		org4.dcData.put("vcard:fn", "Arne Andersson4");
		org4.dcData.put("vcard:hasEmail", "mailto:open@skovde.se");
		dataService3.organizations.add(org4);
		
		Organization org5 = new Organization();
		org5.about = "https://example.com/contactpoint105";
		org5.dcData.put("rdf:type", "http://www.w3.org/2006/vcard/ns#Organization");
		org5.dcData.put("vcard:fn", "Arne Andersson5");
		org5.dcData.put("vcard:hasEmail", "mailto:open@skovde.se");
		dataService3.organizations.add(org5);

		DataClass agent2 = new DataClass();
		agent2.about = "https://example.com/publisher102";
		agent2.dcData.putAll("foaf:name", List.of("Exampel organization", "Exampel organization 2"));
		agent2.dcData.put("dcterms:type", "http://purl.org/adms/publishertype/LocalAuthority/");
		dataService1.agents.add(agent2);

		DataClass agent3 = new DataClass();
		agent3.about = "https://example.com/publisher103";
		agent3.dcData.putAll("foaf:name", List.of("Exampel organization", "Exampel organization 3"));
		agent3.dcData.put("dcterms:type", "http://purl.org/adms/publishertype/LocalAuthority/");
		dataService1.agents.add(agent3);
		
		return List.of(fileStorage);
	}

	private DataClass createSpatial() {
		DataClass spatial = new DataClass();
		spatial.dcData.put("dcat:centroid", "POINT(18.07 59.33)");
		spatial.dcData.put("dcat:bbox", "POLYGON((10.9 55.3, 24.2 55.3, 24.2 69.1, 10.9 69.1, 10.9 55.3))");
		spatial.dcData.put("locn:geometry", "LINESTRING(18.07 59.33, 17.64 59.86, 16.19 58.59)");
		return spatial;
	}

	private DataClass createOtherAgent(List<String> key, List<String> value) {
		DataClass otherActor = createDataClass(null, List.of(key.get(3)), List.of(value.get(3)));
		otherActor.agent = createDataClass(value.get(0), List.of(key.get(1), key.get(2)), List.of(value.get(1),value.get(2)));
		return otherActor;
	}
	
	private DataClass createDataClass(String uriLink, List<String> keys, List<String> values) {
		DataClass dataClass = new DataClass();
		
		if(uriLink != null) {
			dataClass.about = uriLink;
		}
		
		for (int i = 0; i < keys.size(); i++) {
			dataClass.dcData.put(keys.get(i), values.get(i));		
		}
		return dataClass;	
	}
			
	private DataClass createRights(List<DataClass> copyRightHolders, List<DataClass> licenseDocuments, List<String> keys, List<String> values) {
		DataClass rights = new DataClass();
		rights.agents.addAll(copyRightHolders);		
		rights.licenseDocuments.addAll(licenseDocuments);
		
		for (int i = 0; i < keys.size(); i++) {
			rights.dcData.put(keys.get(i), values.get(i));
		}
		
		return rights;
	}
}
