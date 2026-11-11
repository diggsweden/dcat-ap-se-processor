// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.converter;

import java.util.Optional;

import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.json.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import se.ams.dcatprocessor.models.ConverterHelpClass;
import se.ams.dcatprocessor.models.DataClass;
import se.ams.dcatprocessor.models.DataSet;
import se.ams.dcatprocessor.models.Distribution;
import se.ams.dcatprocessor.models.Organization;
import se.ams.dcatprocessor.rdf.namespace.SCHEMA;

@Component
@Scope("prototype")
public class ConverterDataSet extends Converter {

    private ConverterDistribution converterDistribution;

    public ConverterDataSet(ConverterDistribution converterDistribution) {
        this.converterDistribution = converterDistribution;
    }

    /* Process the spec to find elements for dcat-ap-se and add them to an Object to return */
    @Override
    void processToDcat(JSONObject subConvert, JSONObject file, Optional<String> subCat, Optional<DataClass> preData, Optional<DataClass> preDist) throws Exception {
        DataSet dataSet = new DataSet();
        Distribution distribution = new Distribution();
        Organization organizationLocal = new Organization();
        DataClass dataClassLocal = new DataClass();
        DataClass otherAgent = new DataClass();

        for (String key : subConvert.keySet()) {

            if (key.equals(ConverterHelpClass.toDcatString) && subCat.isPresent()) {
                continue;
            }

            // Get tag-name for api-spec
            String annotationName = subConvert.getJSONObject(key).getString(ConverterHelpClass.toDcatString);

            // Check if tag is Mandatory
            boolean isMandatory = isKeyMandatory(key, subCat);

            // Do if key is DISTRIBUTION
            if (key.equals(DCAT.DISTRIBUTION.getLocalName())) {
                converterDistribution.sourceFilename = this.sourceFilename;
                converterDistribution.orgConvert = orgConvert;
                converterDistribution.jsonObjectMandatoryDcat = jsonObjectMandatoryDcat;
                converterDistribution.fileHandler = fileHandler;
                converterDistribution.sectionPath.clear();
                converterDistribution.sectionPath.addAll(this.sectionPath);
                converterDistribution.createSubset(file, key, annotationName, Optional.of(dataSet), Optional.of(distribution), isMandatory);
                
                this.errors.addAll(converterDistribution.errors);
                converterDistribution.errors.clear();
            }
            // Do if key is LICENSE_DOCUMENT
            else if (key.equals(DCTERMS.LICENSE_DOCUMENT.getLocalName())) {
                if (subCat.isPresent()) {
                    if (subCat.get().equals(DCTERMS.RIGHTS_STATEMENT.getLocalName())) {
                        createSubset(file, key, annotationName, Optional.of(dataClassLocal), Optional.empty(), isMandatory);
                    }
                }
            }
            // Do if key is A Nested Object
            else if (ConverterHelpClass.isNestedObjects(key)) {
                if (subCat.get().equals(DCAT.DATASET.getLocalName())) {
                    createSubset(file, key, annotationName, Optional.of(dataSet), Optional.empty(), isMandatory);
                } else {
                    createSubset(file, key, annotationName, Optional.empty(), Optional.empty(), isMandatory);
                }
            }
            // Checks for language strings to add them correctly
            else {
                boolean hasLanguages = false;
                if (subCat.isPresent()) {
                    if (subCat.get().equals(DCAT.DATASET.getLocalName())) {
                        hasLanguages = addLanguageValues(file, annotationName, subCat, Optional.of(dataSet), key);
                    } else if (subCat.get().contains(PROV.ATTRIBUTION.getLocalName())) {
                        hasLanguages = addLanguageValues(file, annotationName, subCat, Optional.of(otherAgent), key);
                    } else if (ConverterHelpClass.isNestedLanguageObjects(subCat.get())) {
                        hasLanguages = addLanguageValues(file, annotationName, subCat, Optional.of(dataClassLocal), key);
                    }
                }
                if (!hasLanguages) {
                    if (key.equals(VCARD4.ADDRESS.getLocalName())) {
                        handleAddress(file, key, annotationName, organizationLocal, subCat);
                    } else if (file.has(annotationName)) {
                        String value = String.valueOf(file.get(annotationName));
                        if (subCat.isPresent()) {
                            if (subCat.get().contains(DCAT.DATASET.getLocalName())) {
                                addValues(dataSet, value, key, subCat);
                            } else if (subCat.get().contains(DCAT.CONTACT_POINT.getLocalName())) {
                                addValues(organizationLocal, value, key, subCat);
                            } else if (subCat.get().contains(PROV.ATTRIBUTION.getLocalName())) {
                                if (key.equals("dcat:hadRole")) {
                                    addValues(dataClassLocal, value, key, subCat);
                                }
                                else {
                                    addValues(otherAgent, value, key, subCat);
                                }
                            } else if (ConverterHelpClass.isNestedObjects(subCat.get())) {
                                addValues(dataClassLocal, value, key, subCat);
                            }
                        }
                    } else if (isMandatory) {
                        addMandatoryError(annotationName, subCat, key);
                    }
                }
            }
        }
        if (subCat.isPresent()) {
            attachDataToParent(subCat, preData, dataSet, organizationLocal, dataClassLocal, otherAgent);
        }
    }

    
    // Attaches the locally built objects (dataClassLocal, dataSet, organizationLocal, otherAgent)
    // to the correct field on the parent (preData or fileHandler)
    private void attachDataToParent(Optional<String> subCat, Optional<DataClass> preData, DataSet dataSet,
            Organization organizationLocal, DataClass dataClassLocal, DataClass otherAgent) {

        String subCatValue = subCat.get();

        if (subCatValue.contains(DCAT.DATASET.getLocalName())) {
            fileHandler.dcat_dataset.add(dataSet);
            return;
        }

        if (preData.isEmpty()) return;
        DataClass parentData = preData.get();

        if (subCatValue.contains(DCTERMS.PUBLISHER.getLocalName())) {
            parentData.agent = dataClassLocal;
        } else if (subCatValue.contains(DCTERMS.CREATOR.getLocalName())) {
            ((DataSet) parentData).creator = dataClassLocal;
        } else if (subCatValue.contains(PROV.ATTRIBUTION.getLocalName())) {
            dataClassLocal.agent = otherAgent;
            ((DataSet) parentData).otherAgents.add(dataClassLocal);
        } else if (subCatValue.equals(DCTERMS.TEMPORAL.getLocalName())) {
            ((DataSet) parentData).temporals.add(dataClassLocal);
        } else if (subCatValue.equals(SCHEMA.OFFERS.getLocalName())) {
            ((DataSet) parentData).offers.add(dataClassLocal);
        } else if (subCatValue.equals(DCTERMS.STANDARD.getLocalName())) {
            ((DataSet) parentData).conformsTo.add(dataClassLocal);
        } else if (subCatValue.equals(DCTERMS.SPATIAL.getLocalName())) {
            ((DataSet) parentData).spatial.add(dataClassLocal);
        } else if (subCatValue.contains(DCAT.QUALIFIED_RELATION.getLocalName())) {
            ((DataSet) parentData).qualifiedRelations.add(dataClassLocal);
        } else if (subCatValue.contains(DCAT.CONTACT_POINT.getLocalName())) {
            ((DataSet) parentData).organizations.add(organizationLocal);
        } else if (subCatValue.contains(FOAF.DOCUMENT.getLocalName())) {
            parentData.documents.add(dataClassLocal);
        }
    }

    @Override
    protected String getSectionName() {
        return DCAT.DATASET.getLocalName();
    }
}
