// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.converter;

import java.io.IOException;
import java.nio.file.Path;

import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import se.ams.dcatprocessor.processor.DcatResult;
import se.ams.dcatprocessor.processor.Manager;
import se.ams.dcatprocessor.testutil.TestHelper;

@SpringBootTest
public class ConverterEmptyUriTest {

    @Autowired
    private ObjectProvider<Manager> managerProvider;

    private Manager manager;
    private final String API_DEF_FILE = "src/test/resources/apidef/json_v3/json_oas_301.json";

    @BeforeEach
    public void setup() throws Exception {
        manager = managerProvider.getObject();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void testThatEmptyCatalogAboutIsReportedAsValidationError(String emptyValue, @TempDir Path tempDir) throws Exception {
        Path modified = TestHelper.copyWith(Path.of(API_DEF_FILE), tempDir,
            json -> json.getJSONObject("info")
                        .getJSONObject("x-dcat")
                        .getJSONObject("dcat-catalog").put("about", emptyValue));

        DcatResult result = manager.createDcatFromFile(modified.toString());

        assertTrue(result.hasErrors());
        assertTrue(result.errorReport().contains("about must not be empty"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void testThatEmptyPageAboutIsReportedAsValidationError(String emptyValue, @TempDir Path tempDir) throws Exception {
        Path modified = modifyPageAbout(tempDir, emptyValue);

        DcatResult result = manager.createDcatFromFile(modified.toString());

        assertTrue(result.hasErrors());
        assertTrue(result.errorReport().contains("about must not be empty"));
    }

    @Test
    void testThatNullPageAboutIsReportedAsValidationError(@TempDir Path tempDir) throws Exception {
        Path modified = modifyPageAbout(tempDir, JSONObject.NULL);
    
        DcatResult result = manager.createDcatFromFile(modified.toString());
    
        assertTrue(result.hasErrors(), "null about must be reported");
        assertTrue(result.errorReport().contains("about"));
        assertTrue(result.errorReport().contains("empty"));
        assertFalse(result.rdf() != null && result.rdf().contains("http://null"));
    }

    @Test
    void testThatValidAboutWithoutSchemeGetsPrefix(@TempDir Path tempDir) throws Exception {
        String noPrefix = "example.org/example-page";
        Path modified = modifyPageAbout(tempDir, noPrefix);

        DcatResult result = manager.createDcatFromFile(modified.toString());
        
        assertFalse(result.hasErrors());
        assertTrue(result.rdf().contains("http://" + noPrefix));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void testThatEmptySpatialUrlIsReportedAsValidationError(String emptyValue, @TempDir Path tempDir) throws Exception {
        Path modified = TestHelper.copyWith(Path.of(API_DEF_FILE), tempDir,
            json -> json.getJSONObject("info")
                        .getJSONObject("x-dcat")
                        .getJSONObject("dcat-dataset").put("spatialUrl", emptyValue));

        DcatResult result = manager.createDcatFromFile(modified.toString());

        assertTrue(result.hasErrors());
        assertTrue(result.errorReport().contains("dcterms:spatial must not be empty"));
    }

    private Path modifyPageAbout(Path tempDir, Object aboutValue) throws IOException {
        return TestHelper.copyWith(Path.of(API_DEF_FILE), tempDir,
            json -> json.getJSONObject("info")
                        .getJSONObject("x-dcat")
                        .getJSONObject("dcat-dataset")
                        .getJSONObject("distribution")
                        .getJSONObject("page").put("about", aboutValue));
    }
}
