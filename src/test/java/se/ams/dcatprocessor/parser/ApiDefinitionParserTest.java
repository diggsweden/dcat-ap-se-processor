// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.parser;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import se.ams.dcatprocessor.models.ApiSource;
import se.ams.dcatprocessor.models.ApiSpecFile;
import se.ams.dcatprocessor.parser.ApiDefinitionParser.ApiSpecSyntax;
import se.ams.dcatprocessor.rdf.DcatException;

public class ApiDefinitionParserTest {

    private static String ramlApi = "#%RAML 1.0\n"
            + "title: Annotation exempel api\n"
            + "description: Annotation exempel api är ett påhittat api som använder sig av annotations för att skapa metadata för DCAT-AP-SE.\n"
            + "version: 1.0.0\n"
            + "annotationTypes:\n"
            + "dcat-catalog:\n"
            + "  properties:\n"
            + "    about: string\n"
            + "    title: string\n"
            + "    description: string\n"
            + "    publisher:\n"
            + "      name: string\n"
            + "      type: string\n"
            + "      homepage: string\n"
            + "      mbox: string\n"
            + "    license: string\n"
            + "    issued: string\n"
            + "    language: string\n"
            + "    modified: string\n"
            + "    homepage: string\n"
            + "    location:\n"
            + "      centroid: string\n"
            + "      bbox: string\n"
            + "      geometry: string\n"
            + "    rights: string\n"
            + "    hasPart: string\n"
            + "    isPartOf: string\n"
            + "    themeTaxonomy: string\n"
            + "dcat-dataset:\n"
            + "  properties:\n"
            + "    title: string\n"
            + "    description: string\n"
            + "    publisher:\n"
            + "      name: string\n"
            + "      type: string\n"
            + "      homepage: string\n"
            + "      mbox: string\n"
            + "    creator: string\n"
            + "    qualifiedAttribution: string\n"
            + "    contactPoint:\n"
            + "      type: string\n"
            + "      name: string\n"
            + "      email: string\n"
            + "      phone: string\n"
            + "      adress: string\n"
            + "    distribution:\n"
            + "      belongsTo: string\n"
            + "      title: string\n"
            + "      description: string\n"
            + "      accessURL: string\n"
            + "      downloadURL: string\n"
            + "      format: string\n"
            + "      accessService: string\n"
            + "      temporalResolution: string\n"
            + "      spatialResolutionInMeters: string\n"
            + "      byteSize: string\n"
            + "      language: string\n"
            + "      issued: string\n"
            + "      modified: string\n"
            + "      status: string\n"
            + "      availability: string\n"
            + "      license: string\n"
            + "      rights: string\n"
            + "      checksum: string\n"
            + "      page: string\n"
            + "      conformsTo: string\n"
            + "      keyword: string\n"
            + "      theme: string\n"
            + "      identifier: string\n"
            + "      adms: string\n"
            + "      landingPage: string\n"
            + "    location:\n"
            + "      centroid: string\n"
            + "      bbox: string\n"
            + "      geometry: string\n"
            + "    temporal:\n"
            + "      startDate: string\n"
            + "      endDate: string\n"
            + "      temporalResolution: string\n"
            + "      spatialResolutionInMeters: string\n"
            + "      accrualPeriodicity: string\n"
            + "      versionInfo: string\n"
            + "      versionNotes: string\n"
            + "      source: string\n"
            + "      accessRights: string\n"
            + "      offers: string\n"
            + "      hasVersion: string\n"
            + "      isReferencedBy: string\n"
            + "      relation: string\n"
            + "      qualifiedRelation: string\n"
            + "      page: string\n"
            + "      provenance: string\n"
            + "dcat-dataservice:\n"
            + "  properties:\n"
            + "    title: string\n"
            + "    description: string\n"
            + "    endpointURL: string\n"
            + "    endpointDescription: string\n"
            + "    publisher:\n"
            + "      name: string\n"
            + "      type: string\n"
            + "      homepage: string\n"
            + "      mbox: string\n"
            + "    contactPoint:\n"
            + "      type: string\n"
            + "      name: string\n"
            + "      email: string\n"
            + "      phone: string\n"
            + "      adress: string\n"
            + "      keyword: string\n"
            + "      theme: string\n"
            + "      conformsTo: string\n"
            + "      servesDataset: string\n"
            + "      license: string\n"
            + "      accessRights: string\n"
            + "      landingPage: string\n"
            + "      page: string\n"
            + "(dcat-catalog):\n"
            + "  about: www.af.se\n"
            + "  title-sv: Annotation exempel RAML 1.0\n"
            + "  title-en: Annotation example RAML 1.0\n"
            + "  description: Annotation exempel från arbetsförmedlingen\n"
            + "  publisher:\n"
            + "    name: Redpill Linpro AB Catalog\n"
            + "  license: https://www.apache.org/licenses/LICENSE-2.0\n"
            + "  dataset: https://www.example.se/result.rdf#dataset1\n"
            + "(dcat-dataset 1):\n"
            + "  about: https://www.example.se/result.rdf#dataset1\n"
            + "  title-sv: Datamängd 1\n"
            + "  description: Exempel beskrivning 1\n"
            + "  publisher:\n"
            + "    name: Redpill Linpro AB 1\n";

    private static String jsonApi = "{\n" +
            "  \"dcat-catalog\": {\n" +
            "    \"about\": \"https://www.af.se\",\n" +
            "    \"title-sv\": \"Annotation exempel RAML 1.0\",\n" +
            "    \"title-en\": \"Annotation example RAML 1.0\",\n" +
            "    \"description-sv\": \"Annotation exempel från arbetsförmedlingen\",\n" +
            "    \"publisher\": {\n" +
            "      \"about\": \"https://www.example.se/result.rdf#publisher\",\n" +
            "      \"name\": \"Redpill Linpro AB Catalog\"\n" +
            "    },\n" +
            "    \"license\": \"https://www.apache.org/licenses/LICENSE-2.0\"\n" +
            "  },\n" +
            "  \"dcat-dataset\": {\n" +
            "    \"about\": \"https://www.example.se/result.rdf#dataset1\",\n" +
            "    \"title-sv\": \"Datamängd 1\",\n" +
            "    \"description-sv\": \"Exempel beskrivning 1\",\n" +
            "    \"publisher\": {\n" +
            "      \"about\": \"https://www.example.se/result.rdf#publisher\",\n" +
            "      \"name\": \"Redpill Linpro AB 1\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static String ramlResult = "{\"(dcat-dataset 1)\":{\"title-sv\":\"Datamängd 1\",\"about\":\"https://www.example.se/result.rdf#dataset1\",\"description\":\"Exempel beskrivning 1\",\"publisher\":{\"name\":\"Redpill Linpro AB 1\"}},\"dcat-dataservice\":{\"properties\":{\"contactPoint\":{\"landingPage\":\"string\",\"adress\":\"string\",\"type\":\"string\",\"license\":\"string\",\"phone\":\"string\",\"name\":\"string\",\"theme\":\"string\",\"conformsTo\":\"string\",\"accessRights\":\"string\",\"page\":\"string\",\"keyword\":\"string\",\"servesDataset\":\"string\",\"email\":\"string\"},\"endpointURL\":\"string\",\"description\":\"string\",\"publisher\":{\"name\":\"string\",\"type\":\"string\",\"mbox\":\"string\",\"homepage\":\"string\"},\"title\":\"string\",\"endpointDescription\":\"string\"}},\"description\":\"Annotation exempel api är ett påhittat api som använder sig av annotations för att skapa metadata för DCAT-AP-SE.\",\"dcat-dataset\":{\"properties\":{\"creator\":\"string\",\"contactPoint\":{\"phone\":\"string\",\"name\":\"string\",\"adress\":\"string\",\"type\":\"string\",\"email\":\"string\"},\"description\":\"string\",\"publisher\":{\"name\":\"string\",\"type\":\"string\",\"mbox\":\"string\",\"homepage\":\"string\"},\"location\":{\"centroid\":\"string\",\"bbox\":\"string\",\"geometry\":\"string\"},\"title\":\"string\",\"distribution\":{\"byteSize\":\"string\",\"accessURL\":\"string\",\"downloadURL\":\"string\",\"description\":\"string\",\"language\":\"string\",\"availability\":\"string\",\"title\":\"string\",\"temporalResolution\":\"string\",\"accessService\":\"string\",\"spatialResolutionInMeters\":\"string\",\"rights\":\"string\",\"checksum\":\"string\",\"modified\":\"string\",\"theme\":\"string\",\"issued\":\"string\",\"keyword\":\"string\",\"identifier\":\"string\",\"adms\":\"string\",\"landingPage\":\"string\",\"format\":\"string\",\"license\":\"string\",\"page\":\"string\",\"conformsTo\":\"string\",\"belongsTo\":\"string\",\"status\":\"string\"},\"qualifiedAttribution\":\"string\",\"temporal\":{\"offers\":\"string\",\"endDate\":\"string\",\"hasVersion\":\"string\",\"versionInfo\":\"string\",\"source\":\"string\",\"relation\":\"string\",\"isReferencedBy\":\"string\",\"provenance\":\"string\",\"temporalResolution\":\"string\",\"spatialResolutionInMeters\":\"string\",\"qualifiedRelation\":\"string\",\"accrualPeriodicity\":\"string\",\"versionNotes\":\"string\",\"accessRights\":\"string\",\"page\":\"string\",\"startDate\":\"string\"}}},\"title\":\"Annotation exempel api\",\"version\":\"1.0.0\",\"annotationTypes\":null,\"dcat-catalog\":{\"properties\":{\"themeTaxonomy\":\"string\",\"hasPart\":\"string\",\"about\":\"string\",\"description\":\"string\",\"language\":\"string\",\"title\":\"string\",\"isPartOf\":\"string\",\"license\":\"string\",\"rights\":\"string\",\"publisher\":{\"name\":\"string\",\"type\":\"string\",\"mbox\":\"string\",\"homepage\":\"string\"},\"modified\":\"string\",\"location\":{\"centroid\":\"string\",\"bbox\":\"string\",\"geometry\":\"string\"},\"issued\":\"string\",\"homepage\":\"string\"}},\"(dcat-catalog)\":{\"title-sv\":\"Annotation exempel RAML 1.0\",\"license\":\"https://www.apache.org/licenses/LICENSE-2.0\",\"title-en\":\"Annotation example RAML 1.0\",\"about\":\"www.af.se\",\"description\":\"Annotation exempel från arbetsförmedlingen\",\"publisher\":{\"name\":\"Redpill Linpro AB Catalog\"},\"dataset\":\"https://www.example.se/result.rdf#dataset1\"}}";
    private static String jsonResult = "{\"dcat-dataset\":{\"title-sv\":\"Datamängd 1\",\"about\":\"https://www.example.se/result.rdf#dataset1\",\"publisher\":{\"about\":\"https://www.example.se/result.rdf#publisher\",\"name\":\"Redpill Linpro AB 1\"},\"description-sv\":\"Exempel beskrivning 1\"},\"dcat-catalog\":{\"title-sv\":\"Annotation exempel RAML 1.0\",\"license\":\"https://www.apache.org/licenses/LICENSE-2.0\",\"title-en\":\"Annotation example RAML 1.0\",\"about\":\"https://www.af.se\",\"publisher\":{\"about\":\"https://www.example.se/result.rdf#publisher\",\"name\":\"Redpill Linpro AB Catalog\"},\"description-sv\":\"Annotation exempel från arbetsförmedlingen\"}}";

    // Compact JSON on ONE line, containing the substring "openapi"
    private static final String COMPACT_JSON = "{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"t\",\"version\":\"1\",\"x-dcat\":{\"priority\":1,\"active\":true,\"name\":\"abc\"}}}";

    @Test
    void testThatUnsupportedFormatThrowsException() {
        ApiSource source = new ApiSource("invalid.json", "invalid api spec");

        DcatException exception = assertThrows(DcatException.class, () -> ApiDefinitionParser.getApiSpecFile(source));

        assertTrue(exception.getMessage().contains("Failed to read API definition: unrecognized format"));
    }

    @Test
    void testThatRamlApiIsParsedToExpectedJson() throws JSONException, IOException {
        ApiSource source = new ApiSource("dataset.raml", ramlApi);
        ApiSpecFile actual = ApiDefinitionParser.getApiSpecFile(source);
        JSONObject expected = new JSONObject(ramlResult);
        assertEquals(expected.toMap(), actual.content().toMap());
    }

    @Test
    void testThatJsonApiIsParsedToExpectedJson() throws JSONException, IOException {
        ApiSource source = new ApiSource("dataset.json", jsonApi);
        ApiSpecFile actual = ApiDefinitionParser.getApiSpecFile(source);
        JSONObject expected = new JSONObject(jsonResult);
        assertEquals(expected.toMap(), actual.content().toMap());
    }

    @Test
    void testThatCompactJsonIsParsedAsJson_notYaml() throws Exception {
        ApiSpecFile result = ApiDefinitionParser.getApiSpecFile(new ApiSource("compact.json", COMPACT_JSON));

        // Parsed as JSON the types are preserved.
        assertInstanceOf(Integer.class, result.content().get("priority"));
        assertInstanceOf(Boolean.class, result.content().get("active"));
        assertEquals("abc", result.content().get("name"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        COMPACT_JSON,                                       // single line, contains "openapi"
        "{\n  \"openapi\": \"3.0.3\",\n  \"info\": {}\n}",  // multiline
        "\n  \t{\"a\":1}  \n\n"                             // surrounding whitespace is ignored
    })
    void testThatJsonObjectsAreDetectedAsJson(String input) {
        assertEquals(ApiSpecSyntax.JSON, ApiDefinitionParser.detectSyntax(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "openapi: 3.0.3\ninfo:\n  title: t\n",              // OpenAPI in YAML
        "#%RAML 1.0\ntitle: t\n",                           // RAML header
        "\n\nopenapi: 3.0.3\n"                              // leading blank lines are ignored
    })
    void testThatOpenapiAndRamlAreDetectedAsYamlRaml(String input) {
        assertEquals(ApiSpecSyntax.YAML_RAML, ApiDefinitionParser.detectSyntax(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",                                                 // empty
        "  \n\t ",                                          // only whitespace
        "just some text",                                   // no recognizable marker
        "title: t\nopenapi: 3.0.3\n",                       // "openapi" only on a later line, not the first
        "[{\"a\":1}]"                                       // JSON array: only objects are recognised
    })
    void testThatUnrecognizedInputIsDetectedAsUnknown(String input) {
        assertEquals(ApiSpecSyntax.UNKNOWN, ApiDefinitionParser.detectSyntax(input));
    }
}
