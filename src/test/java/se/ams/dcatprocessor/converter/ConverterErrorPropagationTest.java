// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.converter;

import java.io.IOException;
import java.nio.file.Path;

import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import se.ams.dcatprocessor.processor.DcatResult;
import se.ams.dcatprocessor.processor.Manager;
import se.ams.dcatprocessor.testutil.TestHelper;

@SpringBootTest
public class ConverterErrorPropagationTest {

    @Autowired
    private ObjectProvider<Manager> managerProvider;

    private Manager manager;
    private final String API_DEF_FILE = "src/test/resources/apidef/json_v3/json_oas_301.json";

    @BeforeEach
	public void setup() throws Exception {
		manager = managerProvider.getObject();
	}

    @Test
    void testThatUnmodifiedSpecProducesNoErrors() throws Exception {
        DcatResult result = manager.createDcatFromFile(API_DEF_FILE);
        assertTrue(result.rdf().contains("<rdf:RDF"));
    }

    @Test
    void testThatMissingAccessUrlInDistributionGeneratesError(@TempDir Path tempDir) throws Exception {
        Path modified = TestHelper.copyWith(Path.of(API_DEF_FILE), tempDir,
            json -> json.getJSONObject("info")
                        .getJSONObject("x-dcat")
                        .getJSONObject("dcat-dataset")
                        .getJSONObject("distribution").remove("accessURL"));

        DcatResult result = manager.createDcatFromFile(modified.toString());
        String report = result.errorReport();

        assertTrue(report.contains("The key dcat:accessURL is mandatory"));
    }

    @Test
    void testThatDatasetAndDistributionErrorsAreBothReported(@TempDir Path tempDir) throws Exception {
        Path modified = TestHelper.copyWith(Path.of(API_DEF_FILE), tempDir,
            json -> {
                JSONObject dataset = json.getJSONObject("info")
                                        .getJSONObject("x-dcat")
                                        .getJSONObject("dcat-dataset");
                dataset.getJSONObject("distribution").remove("accessURL");
                dataset.remove("title-sv");
            });

        DcatResult result = manager.createDcatFromFile(modified.toString());
        String report = result.errorReport();

        assertTrue(report.contains("The key dcat:accessURL is mandatory"));
        assertTrue(report.contains("The key dcterms:title is mandatory"));
    }

    @Test
    void testThatErrorInNestedBlockUnderDistributionIsReported(@TempDir Path tempDir) throws Exception {
        Path modified = TestHelper.copyWith(Path.of(API_DEF_FILE), tempDir,
            json -> json.getJSONObject("info")
                        .getJSONObject("x-dcat")
                        .getJSONObject("dcat-dataset")
                        .getJSONObject("distribution")
                        .getJSONObject("page").remove("about"));

        DcatResult result = manager.createDcatFromFile(modified.toString());
        String report = result.errorReport();

        assertTrue(report.contains("The key about is mandatory"));
    }

    @Test
    void testThatEachNestedBlockUnderDataServiceIsNamedInTheReport(@TempDir Path tempDir) throws IOException {
        Path modified = TestHelper.copyWith(Path.of(API_DEF_FILE), tempDir,
            json -> {
                JSONObject dataService = json.getJSONObject("info")
                                            .getJSONObject("x-dcat")
                                            .getJSONObject("dcat-dataservice");
                dataService.getJSONObject("conformsTo").remove("about");
                dataService.getJSONObject("licensedoc").remove("about");
                dataService.getJSONObject("page").remove("about");
            });

        DcatResult result = manager.createDcatFromFile(modified.toString());
        String report = result.errorReport();

        assertTrue(report.contains("DataService.conformsTo"), report);
        assertTrue(report.contains("DataService.licensedoc"), report);
        assertTrue(report.contains("DataService.page"), report);
    }

    @Test
    void testThatErrorInDeeplyNestedBlockIsReportedWithFullPath(@TempDir Path tempDir) throws IOException {
        Path modified = TestHelper.copyWith(Path.of(API_DEF_FILE), tempDir,
            json -> json.getJSONObject("info")
                        .getJSONObject("x-dcat")
                        .getJSONObject("dcat-dataset")
                        .getJSONObject("distribution")
                        .getJSONObject("rights")
                        .getJSONObject("copyrightHolder").put("about", ""));

        DcatResult result = manager.createDcatFromFile(modified.toString());
        String report = result.errorReport();

        assertTrue(report.contains("Dataset.distribution.rights.copyrightHolder: about"), report);
    }
}
