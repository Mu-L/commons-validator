/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>
 * Performs tests for extension in form definitions. Performs the same tests RequiredNameTest does but with an equivalent validation definition with extension
 * definitions (validator-extension.xml), plus an extra check on overriding rules and another one checking it maintains correct order when extending.
 * </p>
 */
class ExtensionTest {

    /**
     * The key used to retrieve the set of validation rules from the xml file.
     */
    protected static final String FORM_KEY = "nameForm";

    /**
     * The key used to retrieve the set of validation rules from the xml file.
     */
    protected static final String FORM_KEY2 = "nameForm2";

    /**
     * The key used to retrieve the set of validation rules from the xml file.
     */
    protected static final String CHECK_MSG_KEY = "nameForm.lastname.displayname";

    /**
     * The key used to retrieve the validator action.
     */
    protected static final String ACTION = "required";

    /**
     * Resources used for validation tests.
     */
    private ValidatorResources resources;

    /**
     * Load {@code ValidatorResources} from validator-extension.xml.
     */
    @BeforeEach
    protected void setUp() throws Exception {
        // Load resources
        try (InputStream in = this.getClass().getResourceAsStream("ExtensionTest-config.xml")) {
            resources = new ValidatorResources(in);
        }
    }

    @AfterEach
    protected void tearDown() {
    }

    /**
     * Tests if the order is maintained when extending a form. Parent form fields should preceed self form fields, except if we override the rules.
     */
    @Test
    void testOrder() {

        final Form form = resources.getForm(ValidatorResources.defaultLocale, FORM_KEY);
        final Form form2 = resources.getForm(ValidatorResources.defaultLocale, FORM_KEY2);

        assertNotNull(form, FORM_KEY + " is null.");
        assertEquals(2, form.getFields().size(), "There should only be 2 fields in " + FORM_KEY);

        assertNotNull(form2, FORM_KEY2 + " is null.");
        assertEquals(2, form2.getFields().size(), "There should only be 2 fields in " + FORM_KEY2);

        // get the first field
        Field fieldFirstName = form.getFields().get(0);
        // get the second field
        Field fieldLastName = form.getFields().get(1);
        assertEquals("firstName", fieldFirstName.getKey(), "firstName in " + FORM_KEY + " should be the first in the list");
        assertEquals("lastName", fieldLastName.getKey(), "lastName in " + FORM_KEY + " should be the first in the list");

//     get the second field
        fieldLastName = form2.getFields().get(0);
        // get the first field
        fieldFirstName = form2.getFields().get(1);
        assertEquals("firstName", fieldFirstName.getKey(), "firstName in " + FORM_KEY2 + " should be the first in the list");
        assertEquals("lastName", fieldLastName.getKey(), "lastName in " + FORM_KEY2 + " should be the first in the list");

    }

    /**
     * Tests if we can override a rule. We "can" override a rule if the message shown when the firstName required test fails and the lastName test is null.
     */
    @Test
    void testOverrideRule() throws ValidatorException {

        // Create bean to run test on.
        final NameBean name = new NameBean();
        name.setLastName("Smith");

        // Construct validator based on the loaded resources
        // and the form key
        final Validator validator = new Validator(resources, FORM_KEY2);
        // add the name bean to the validator as a resource
        // for the validations to be performed on.
        validator.setParameter(Validator.BEAN_PARAM, name);

        // Get results of the validation.
        final ValidatorResults results = validator.validate();

        assertNotNull(results, "Results are null.");

        final ValidatorResult firstNameResult = results.getValidatorResult("firstName");
        final ValidatorResult lastNameResult = results.getValidatorResult("lastName");
        assertNotNull(firstNameResult, "First Name ValidatorResult should not be null.");
        assertEquals(CHECK_MSG_KEY, firstNameResult.field.getArg(0).getKey(),
                "First Name ValidatorResult for the '" + ACTION + "' action should have '" + CHECK_MSG_KEY + " as a key.");

        assertNull(lastNameResult, "Last Name ValidatorResult should be null.");
    }

    /**
     * Tests the required validation failure.
     */
    @Test
    void testRequired() throws ValidatorException {
        // Create bean to run test on.
        final NameBean name = new NameBean();

        // Construct validator based on the loaded resources
        // and the form key
        final Validator validator = new Validator(resources, FORM_KEY);
        // add the name bean to the validator as a resource
        // for the validations to be performed on.
        validator.setParameter(Validator.BEAN_PARAM, name);

        // Get results of the validation.
        // throws ValidatorException,
        // but we aren't catching for testing
        // since no validation methods we use
        // throw this
        final ValidatorResults results = validator.validate();

        assertNotNull(results, "Results are null.");

        final ValidatorResult firstNameResult = results.getValidatorResult("firstName");
        final ValidatorResult lastNameResult = results.getValidatorResult("lastName");

        assertNotNull(firstNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(firstNameResult.containsAction(ACTION), "First Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertFalse(firstNameResult.isValid(ACTION), "First Name ValidatorResult for the '" + ACTION + "' action should have failed.");

        assertNotNull(lastNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(lastNameResult.containsAction(ACTION), "Last Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertFalse(lastNameResult.isValid(ACTION), "Last Name ValidatorResult for the '" + ACTION + "' action should have failed.");
    }

    /**
     * Tests the required validation for first name.
     */
    @Test
    void testRequiredFirstName() throws ValidatorException {
        // Create bean to run test on.
        final NameBean name = new NameBean();
        name.setFirstName("Joe");

        // Construct validator based on the loaded resources
        // and the form key
        final Validator validator = new Validator(resources, FORM_KEY);
        // add the name bean to the validator as a resource
        // for the validations to be performed on.
        validator.setParameter(Validator.BEAN_PARAM, name);

        // Get results of the validation.
        final ValidatorResults results = validator.validate();

        assertNotNull(results, "Results are null.");

        final ValidatorResult firstNameResult = results.getValidatorResult("firstName");
        final ValidatorResult lastNameResult = results.getValidatorResult("lastName");

        assertNotNull(firstNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(firstNameResult.containsAction(ACTION), "First Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertTrue(firstNameResult.isValid(ACTION), "First Name ValidatorResult for the '" + ACTION + "' action should have passed.");

        assertNotNull(lastNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(lastNameResult.containsAction(ACTION), "Last Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertFalse(lastNameResult.isValid(ACTION), "Last Name ValidatorResult for the '" + ACTION + "' action should have failed.");
    }

    /**
     * Tests the required validation for first name if it is blank.
     */
    @Test
    void testRequiredFirstNameBlank() throws ValidatorException {
        // Create bean to run test on.
        final NameBean name = new NameBean();
        name.setFirstName("");

        // Construct validator based on the loaded resources
        // and the form key
        final Validator validator = new Validator(resources, FORM_KEY);
        // add the name bean to the validator as a resource
        // for the validations to be performed on.
        validator.setParameter(Validator.BEAN_PARAM, name);

        // Get results of the validation.
        final ValidatorResults results = validator.validate();

        assertNotNull(results, "Results are null.");

        final ValidatorResult firstNameResult = results.getValidatorResult("firstName");
        final ValidatorResult lastNameResult = results.getValidatorResult("lastName");

        assertNotNull(firstNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(firstNameResult.containsAction(ACTION), "First Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertFalse(firstNameResult.isValid(ACTION), "First Name ValidatorResult for the '" + ACTION + "' action should have failed.");

        assertNotNull(lastNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(lastNameResult.containsAction(ACTION), "Last Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertFalse(lastNameResult.isValid(ACTION), "Last Name ValidatorResult for the '" + ACTION + "' action should have failed.");
    }

    /**
     * Tests the required validation for last name.
     */
    @Test
    void testRequiredLastName() throws ValidatorException {
        // Create bean to run test on.
        final NameBean name = new NameBean();
        name.setLastName("Smith");

        // Construct validator based on the loaded resources
        // and the form key
        final Validator validator = new Validator(resources, FORM_KEY);
        // add the name bean to the validator as a resource
        // for the validations to be performed on.
        validator.setParameter(Validator.BEAN_PARAM, name);

        // Get results of the validation.
        final ValidatorResults results = validator.validate();

        assertNotNull(results, "Results are null.");

        final ValidatorResult firstNameResult = results.getValidatorResult("firstName");
        final ValidatorResult lastNameResult = results.getValidatorResult("lastName");

        assertNotNull(firstNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(firstNameResult.containsAction(ACTION), "First Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertFalse(firstNameResult.isValid(ACTION), "First Name ValidatorResult for the '" + ACTION + "' action should have failed.");

        assertNotNull(lastNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(lastNameResult.containsAction(ACTION), "Last Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertTrue(lastNameResult.isValid(ACTION), "Last Name ValidatorResult for the '" + ACTION + "' action should have passed.");

    }

    /**
     * Tests the required validation for last name if it is blank.
     */
    @Test
    void testRequiredLastNameBlank() throws ValidatorException {
        // Create bean to run test on.
        final NameBean name = new NameBean();
        name.setLastName("");

        // Construct validator based on the loaded resources
        // and the form key
        final Validator validator = new Validator(resources, FORM_KEY);
        // add the name bean to the validator as a resource
        // for the validations to be performed on.
        validator.setParameter(Validator.BEAN_PARAM, name);

        // Get results of the validation.
        final ValidatorResults results = validator.validate();

        assertNotNull(results, "Results are null.");

        final ValidatorResult firstNameResult = results.getValidatorResult("firstName");
        final ValidatorResult lastNameResult = results.getValidatorResult("lastName");

        assertNotNull(firstNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(firstNameResult.containsAction(ACTION), "First Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertFalse(firstNameResult.isValid(ACTION), "First Name ValidatorResult for the '" + ACTION + "' action should have failed.");

        assertNotNull(lastNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(lastNameResult.containsAction(ACTION), "Last Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertFalse(lastNameResult.isValid(ACTION), "Last Name ValidatorResult for the '" + ACTION + "' action should have failed.");
    }

    /**
     * Tests the required validation for first and last name.
     */
    @Test
    void testRequiredName() throws ValidatorException {
        // Create bean to run test on.
        final NameBean name = new NameBean();
        name.setFirstName("Joe");
        name.setLastName("Smith");

        // Construct validator based on the loaded resources
        // and the form key
        final Validator validator = new Validator(resources, FORM_KEY);
        // add the name bean to the validator as a resource
        // for the validations to be performed on.
        validator.setParameter(Validator.BEAN_PARAM, name);

        // Get results of the validation.
        final ValidatorResults results = validator.validate();

        assertNotNull(results, "Results are null.");

        final ValidatorResult firstNameResult = results.getValidatorResult("firstName");
        final ValidatorResult lastNameResult = results.getValidatorResult("lastName");

        assertNotNull(firstNameResult, "First Name ValidatorResult should not be null.");
        assertTrue(firstNameResult.containsAction(ACTION), "First Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertTrue(firstNameResult.isValid(ACTION), "First Name ValidatorResult for the '" + ACTION + "' action should have passed.");

        assertNotNull(lastNameResult, "Last Name ValidatorResult should not be null.");
        assertTrue(lastNameResult.containsAction(ACTION), "Last Name ValidatorResult should contain the '" + ACTION + "' action.");
        assertTrue(lastNameResult.isValid(ACTION), "Last Name ValidatorResult for the '" + ACTION + "' action should have passed.");
    }
}