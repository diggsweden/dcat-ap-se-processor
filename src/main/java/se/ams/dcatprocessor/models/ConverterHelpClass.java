// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.PROV;

import se.ams.dcatprocessor.rdf.namespace.SCHEMA;
import se.ams.dcatprocessor.rdf.namespace.SPDX;

public class ConverterHelpClass {

    public static String uriToDcat = "to_dcat_files/to_dcat/";
    public static String uriToDcatCatalog = "to_dcat_files/to_catalog/";
    public static String uriToDcatSupportive = "to_dcat_files/supportive_files/";
    public static String mandatoryFileName = "mandatory.json";
    public static String convertFileName = "convertmapToDcat.json";
    public static String uriToLanguageDcat = uriToDcatSupportive+"languages.json";
    public static String catalogFileName = "catalog.json";

    public static String toDcatString = "metadata";
    public static String toDcatMandatoryString = "isMandatory";
    public static String toDcatAboutString = "about";
    public static String languages = "dcterms:language";

    public static HashMap<String, String> supportiveFile;
    static {
        supportiveFile = new HashMap<>();
        supportiveFile.put("dcterms:accessRights", "accessRights.json");
        supportiveFile.put("dcatap:availability", "availability.json");
        supportiveFile.put("rdf:type", "contactType.json");
        supportiveFile.put("dcterms:license", "license.json");
        supportiveFile.put("formatMedia", "media.json");
        supportiveFile.put("formatGeo", "geographicmedia.json");
        supportiveFile.put("dcat:theme", "theme.json");
        supportiveFile.put("publisher-dcterms:type", "type.json");
        supportiveFile.put("creator-dcterms:type", "type.json");
        supportiveFile.put("Attribution-dcterms:type", "type.json");
        supportiveFile.put("DataService-dcterms:type", "arcitectureStyle.json");
        supportiveFile.put("Attribution-dcat:hadRole", "userRole.json");
        supportiveFile.put("qualifiedRelation-dcat:hadRole", "resourceRole.json");
        supportiveFile.put("adms:status", "status.json");
        supportiveFile.put("dcterms:accrualPeriodicity", "accrualPeriodicity.json");
    }

    public static HashMap<String, String> addressObject;
    static {
        addressObject = new HashMap<>();
        addressObject.put("StreetAddress", "vcard:street-address");
        addressObject.put("PostalCode", "vcard:postal-code");
        addressObject.put("Locality", "vcard:locality");
        addressObject.put("Country", "vcard:country-name");
    }

    public static List<String> tagWithUri = Arrays.asList("about","foaf:homepage", "license", "homepage", "dcat:accessURL", "dcat:downloadURL", "dcat:landingPage",
            "dcterms:isReferencedBy", "dcterms:relation", "dcat:endpointURL", "dcat:endpointDescription", "dcat:accessService", "dcterms:relation", "dcat:qualifiedRelation",
            "dcat:landingPage", "odrs:attributionURL", "odrs:jurisdiction", "odrs:reuserGuidelines", "schema:mainEntityOfPage", "dcat:servesDataset", "dcat:hasVersion",
            "dcterms:source", "dcterms:spatial");
    public static List<String> tagWithUriMail = Arrays.asList("foaf:mbox", "vcard:hasEmail");

    public static Boolean isNestedObjects(String objectKeyName) {
        if (objectKeyName.equals(DCAT.CONTACT_POINT.getLocalName()) ||
                objectKeyName.equals(DCTERMS.TEMPORAL.getLocalName()) ||
                objectKeyName.equals(DCTERMS.CREATOR.getLocalName()) ||
                objectKeyName.equals(DCTERMS.PUBLISHER.getLocalName()) ||
                objectKeyName.equals(DCTERMS.RIGHTS_STATEMENT.getLocalName()) ||
                objectKeyName.equals(PROV.ATTRIBUTION.getLocalName()) ||
                objectKeyName.equals(SPDX.CHECKSUM.getLocalName()) ||
                objectKeyName.equals(SCHEMA.OFFERS.getLocalName()) ||
                objectKeyName.equals(FOAF.DOCUMENT.getLocalName()) ||
                objectKeyName.equals(DCTERMS.STANDARD.getLocalName()) ||
                objectKeyName.equals(DCTERMS.LICENSE_DOCUMENT.getLocalName()) ||
                objectKeyName.equals(DCAT.QUALIFIED_RELATION.getLocalName()) ||
                objectKeyName.equals(DCTERMS.SPATIAL.getLocalName())) {
            return true;
        }
        else {
            return false;
        }
    }

    public static Boolean isNestedLanguageObjects(String objectKeyName) {
        if (objectKeyName.equals(DCTERMS.LICENSE_DOCUMENT.getLocalName()) ||
                objectKeyName.equals(SPDX.CHECKSUM.getLocalName()) ||
                objectKeyName.equals(SCHEMA.OFFERS.getLocalName()) ||
                objectKeyName.equals(FOAF.DOCUMENT.getLocalName()) ||
                objectKeyName.equals(DCTERMS.STANDARD.getLocalName()) ||
                objectKeyName.equals(DCTERMS.RIGHTS_STATEMENT.getLocalName()) ||
                objectKeyName.equals(DCTERMS.PUBLISHER.getLocalName()) ||
                objectKeyName.equals(DCTERMS.CREATOR.getLocalName()) ||
                objectKeyName.equals(PROV.ATTRIBUTION.getLocalName()))
                {
            return true;
        }
        else {
            return false;
        }
    }
}


