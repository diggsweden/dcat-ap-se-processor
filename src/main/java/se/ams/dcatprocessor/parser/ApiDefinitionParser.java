// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import se.ams.dcatprocessor.models.ApiSource;
import se.ams.dcatprocessor.models.ApiSpecFile;
import se.ams.dcatprocessor.rdf.DcatException;

public class ApiDefinitionParser {
    
    public enum ApiSpecSyntax { JSON, YAML_RAML, UNKNOWN }

    // Top-level blocks that can appear directly in an input document.
    public static final List<String> TOP_LEVEL_DCAT_BLOCKS  = List.of(
        "dcat-catalog",
        "dcat-dataset",
        "dcat-dataservice",
        "dcat-datasetseries"
    );
    

    static ApiSpecSyntax detectSyntax(String fileString) {
        String trimmed = fileString.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return ApiSpecSyntax.JSON;
        }
        String firstLine = trimmed.lines().findFirst().orElse("");
        if (firstLine.contains("openapi") || firstLine.contains("RAML")) {
            return ApiSpecSyntax.YAML_RAML;
        }
        return ApiSpecSyntax.UNKNOWN;
    }

    public static ApiSpecFile getApiSpecFile(ApiSource source) throws DcatException {
        String fileString = source.content();

        String apiJsonString = switch (detectSyntax(fileString)) {
            case JSON -> fileString;
            case YAML_RAML -> getFileApiYamlRaml(fileString);
            case UNKNOWN -> throw new DcatException("Failed to read API definition: unrecognized format, expected an OpenAPI specification (YAML or JSON) or a RAML specification");
        };

        JSONObject jsonObjectFile;
        try {
            jsonObjectFile = new JSONObject(apiJsonString);
        } catch (JSONException e) {
            throw new DcatException("Failed to parse JSON, invalid format.");
        }
        return extractDcatMetadata(jsonObjectFile, source.name());
    }

    private static ApiSpecFile extractDcatMetadata(JSONObject document, String fileName) throws DcatException {
        if (document.has("info")) {
            JSONObject info = document.getJSONObject("info");
            if (!info.has("x-dcat")) {
                throw new DcatException("Failed to extract DCAT metadata: no x-dcat block under info in the API specification");
            }
            return new ApiSpecFile(fileName, info.getJSONObject("x-dcat"));
        }
    
        if (document.has("openapi")) {
            throw new DcatException("Failed to extract DCAT metadata: no info block found in the OpenAPI specification");
        }

        // Not an API specification — the document should be DCAT metadata itself
        if (!hasTopLevelDcatBlock(document)) {
            throw new DcatException("Could not identify a valid API specification or DCAT metadata in the provided input");
        }

        return new ApiSpecFile(fileName, document);
    }

    private static String getFileApiYamlRaml(String apiSpec) throws DcatException {
        JsonFactory factory = new JsonFactory();

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
            JsonGenerator generator = factory.createGenerator(output, JsonEncoding.UTF8)) {

            Yaml yamlParser = new Yaml();
            yamlParser.addImplicitResolver(Tag.YAML, Pattern.compile("^(!)$"), "!");
            Node compose = yamlParser.compose(new StringReader(apiSpec));
            
            build(compose, generator);
            generator.flush();
            
            String jsonString = output.toString(StandardCharsets.UTF_8);
            return new JSONObject(jsonString).toString();
            
        } catch (IOException | YAMLException | JSONException e) {
            throw new DcatException("Invalid format, failed to parse YAML/RAML");
        }
    }

    /*
     * Checks whether the document contains at least one recognized top-level DCAT block.
     * Matching is done with contains rather than equals because RAML annotations appear
     * as "(dcat-dataset)" while JSON uses the bare key.
     */
    private static boolean hasTopLevelDcatBlock(JSONObject document) {
        for (String documentKey : document.keySet()) {
            for (String blockKey : TOP_LEVEL_DCAT_BLOCKS) {
                if (documentKey.contains(blockKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void build(Node yaml, JsonGenerator generator) throws IOException {
        if (yaml instanceof MappingNode) {
            final MappingNode mappingNode = (MappingNode) yaml;
            generator.writeStartObject();
            for (NodeTuple tuple : mappingNode.getValue()) {
                if (tuple.getKeyNode() instanceof ScalarNode) {
                    generator.writeFieldName(((ScalarNode) tuple.getKeyNode()).getValue());
                }
                build(tuple.getValueNode(), generator);
            }
            generator.writeEndObject();
        } else if (yaml instanceof SequenceNode) {
            generator.writeStartArray();
            for (Node node : ((SequenceNode) yaml).getValue()) {
                build(node, generator);
            }
            generator.writeEndArray();
        } else if (yaml instanceof ScalarNode) {
            ScalarNode scalarNode = (ScalarNode) yaml;
            String className;

            if (!scalarNode.getTag().startsWith("!")) {
                className = scalarNode.getTag().getClassName();
                if ("null".equals(className)) {
                    generator.writeNull();
                } else {
                    generator.writeString(scalarNode.getValue());
                }
            } else {
                generator.writeString("!" + scalarNode.getValue());
            }
        }
    }
}