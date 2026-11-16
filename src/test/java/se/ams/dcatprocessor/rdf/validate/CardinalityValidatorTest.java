// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.rdf.validate;

import java.util.ArrayList;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.ams.dcatprocessor.rdf.CardinalityHandler;
import se.ams.dcatprocessor.rdf.DcatClass;
import se.ams.dcatprocessor.rdf.DcatException;
import se.ams.dcatprocessor.specification.DcatSpecification;
import se.ams.dcatprocessor.specification.SpecificationLoader;
import se.ams.dcatprocessor.testutil.TestHelper;

class CardinalityValidatorTest {

	CardinalityValidator cardinalityValidator;
	private static CardinalityHandler handler;

	@BeforeEach
	public void loadSpecification() throws Exception{
		SpecificationLoader specificationLoader = new SpecificationLoader(TestHelper.bundlePathFromApplicationProperties());
		DcatSpecification dcatSpecification = new DcatSpecification(specificationLoader);
		handler = new CardinalityHandler(dcatSpecification);
	}

	@BeforeEach
	public void setup(){
		cardinalityValidator = new CardinalityValidator(handler);
	}

	@Test
	void testThatValidateFailsWhenFilenameNotSet() throws Exception {
		try {
			cardinalityValidator.validate(DcatClass.CATALOG, new ArrayListValuedHashMap<>(), new ArrayList<>(), "Dataset.title");
			fail("Expected exception when current filename is not set in CardinalityValidator");
		} catch (DcatException e) {
			assertEquals(CardinalityValidator.class + " Error validating input data. Reason: Filename for the file being validated is not set", e.getMessage());
		}
	}
}