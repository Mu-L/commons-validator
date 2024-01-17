/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.validator.routines;

import java.text.Format;
import java.util.Locale;

/**
 * <p><b>Byte Validation</b> and Conversion routines (<code>java.lang.Byte</code>).</p>
 *
 * <p>This validator provides a number of methods for
 *    validating/converting a <code>String</code> value to
 *    a <code>Byte</code> using <code>java.text.NumberFormat</code>
 *    to parse either:</p>
 *    <ul>
 *       <li>using the default format for the default <code>Locale</code></li>
 *       <li>using a specified pattern with the default <code>Locale</code></li>
 *       <li>using the default format for a specified <code>Locale</code></li>
 *       <li>using a specified pattern with a specified <code>Locale</code></li>
 *    </ul>
 *
 * <p>Use one of the <code>isValid()</code> methods to just validate or
 *    one of the <code>validate()</code> methods to validate and receive a
 *    <i>converted</i> <code>Byte</code> value.</p>
 *
 * <p>Once a value has been successfully converted the following
 *    methods can be used to perform minimum, maximum and range checks:</p>
 *    <ul>
 *       <li><code>minValue()</code> checks whether the value is greater
 *           than or equal to a specified minimum.</li>
 *       <li><code>maxValue()</code> checks whether the value is less
 *           than or equal to a specified maximum.</li>
 *       <li><code>isInRange()</code> checks whether the value is within
 *           a specified range of values.</li>
 *    </ul>
 *
 * <p>So that the same mechanism used for parsing an <i>input</i> value
 *    for validation can be used to format <i>output</i>, corresponding
 *    <code>format()</code> methods are also provided. That is you can
 *    format either:</p>
 *    <ul>
 *       <li>using the default format for the default <code>Locale</code></li>
 *       <li>using a specified pattern with the default <code>Locale</code></li>
 *       <li>using the default format for a specified <code>Locale</code></li>
 *       <li>using a specified pattern with a specified <code>Locale</code></li>
 *    </ul>
 *
 * @since 1.3.0
 */
public class ByteValidator extends AbstractNumberValidator {

    private static final long serialVersionUID = 7001640945881854649L;

    private static final ByteValidator VALIDATOR = new ByteValidator();

    /**
     * Gets the singleton instance of this validator.
     * @return A singleton instance of the ByteValidator.
     */
    public static ByteValidator getInstance() {
        return VALIDATOR;
    }

    /**
     * Constructs a <i>strict</i> instance.
     */
    public ByteValidator() {
        this(true, STANDARD_FORMAT);
    }

    /**
     * <p>Construct an instance with the specified strict setting
     *    and format type.</p>
     *
     * <p>The <code>formatType</code> specified what type of
     *    <code>NumberFormat</code> is created - valid types
     *    are:</p>
     *    <ul>
     *       <li>AbstractNumberValidator.STANDARD_FORMAT -to create
     *           <i>standard</i> number formats (the default).</li>
     *       <li>AbstractNumberValidator.CURRENCY_FORMAT -to create
     *           <i>currency</i> number formats.</li>
     *       <li>AbstractNumberValidator.PERCENT_FORMAT -to create
     *           <i>percent</i> number formats (the default).</li>
     *    </ul>
     *
     * @param strict {@code true} if strict
     *        <code>Format</code> parsing should be used.
     * @param formatType The <code>NumberFormat</code> type to
     *        create for validation, default is STANDARD_FORMAT.
     */
    public ByteValidator(final boolean strict, final int formatType) {
        super(strict, formatType, false);
    }

    /**
     * Check if the value is within a specified range.
     *
     * @param value The <code>Number</code> value to check.
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     * @return {@code true} if the value is within the
     *         specified range.
     */
    public boolean isInRange(final byte value, final byte min, final byte max) {
        return value >= min && value <= max;
    }

    /**
     * Check if the value is within a specified range.
     *
     * @param value The <code>Number</code> value to check.
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     * @return {@code true} if the value is within the
     *         specified range.
     */
    public boolean isInRange(final Byte value, final byte min, final byte max) {
        return isInRange(value.byteValue(), min, max);
    }

    /**
     * Check if the value is less than or equal to a maximum.
     *
     * @param value The value validation is being performed on.
     * @param max The maximum value.
     * @return {@code true} if the value is less than
     *         or equal to the maximum.
     */
    public boolean maxValue(final byte value, final byte max) {
        return value <= max;
    }

    /**
     * Check if the value is less than or equal to a maximum.
     *
     * @param value The value validation is being performed on.
     * @param max The maximum value.
     * @return {@code true} if the value is less than
     *         or equal to the maximum.
     */
    public boolean maxValue(final Byte value, final byte max) {
        return maxValue(value.byteValue(), max);
    }

    /**
     * Check if the value is greater than or equal to a minimum.
     *
     * @param value The value validation is being performed on.
     * @param min The minimum value.
     * @return {@code true} if the value is greater than
     *         or equal to the minimum.
     */
    public boolean minValue(final byte value, final byte min) {
        return value >= min;
    }

    /**
     * Check if the value is greater than or equal to a minimum.
     *
     * @param value The value validation is being performed on.
     * @param min The minimum value.
     * @return {@code true} if the value is greater than
     *         or equal to the minimum.
     */
    public boolean minValue(final Byte value, final byte min) {
        return minValue(value.byteValue(), min);
    }

    /**
     * <p>Perform further validation and convert the <code>Number</code> to
     * a <code>Byte</code>.</p>
     *
     * @param value The parsed <code>Number</code> object created.
     * @param formatter The Format used to parse the value with.
     * @return The parsed <code>Number</code> converted to a
     *   <code>Byte</code> if valid or {@code null} if invalid.
     */
    @Override
    protected Object processParsedValue(final Object value, final Format formatter) {

        // Parsed value will be Long if it fits in a long and is not fractional
        if (value instanceof Long) {
            final long longValue = ((Long)value).longValue();
            if (longValue >= Byte.MIN_VALUE &&
                longValue <= Byte.MAX_VALUE) {
                return Byte.valueOf((byte)longValue);
            }
        }
        return null;
    }

    /**
     * <p>Validate/convert a <code>Byte</code> using the default
     *    <code>Locale</code>.
     *
     * @param value The value validation is being performed on.
     * @return The parsed <code>Byte</code> if valid or {@code null}
     *  if invalid.
     */
    public Byte validate(final String value) {
        return (Byte)parse(value, (String)null, (Locale)null);
    }

    /**
     * <p>Validate/convert a <code>Byte</code> using the
     *    specified <code>Locale</code>.
     *
     * @param value The value validation is being performed on.
     * @param locale The locale to use for the number format, system default if null.
     * @return The parsed <code>Byte</code> if valid or {@code null} if invalid.
     */
    public Byte validate(final String value, final Locale locale) {
        return (Byte)parse(value, (String)null, locale);
    }

    /**
     * <p>Validate/convert a <code>Byte</code> using the
     *    specified <i>pattern</i>.
     *
     * @param value The value validation is being performed on.
     * @param pattern The pattern used to validate the value against.
     * @return The parsed <code>Byte</code> if valid or {@code null} if invalid.
     */
    public Byte validate(final String value, final String pattern) {
        return (Byte)parse(value, pattern, (Locale)null);
    }

    /**
     * <p>Validate/convert a <code>Byte</code> using the
     *    specified pattern and/ or <code>Locale</code>.
     *
     * @param value The value validation is being performed on.
     * @param pattern The pattern used to validate the value against, or the
     *        default for the <code>Locale</code> if {@code null}.
     * @param locale The locale to use for the date format, system default if null.
     * @return The parsed <code>Byte</code> if valid or {@code null} if invalid.
     */
    public Byte validate(final String value, final String pattern, final Locale locale) {
        return (Byte)parse(value, pattern, locale);
    }

}
