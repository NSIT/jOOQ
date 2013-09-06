/**
 * Copyright (c) 2009-2013, Data Geekery GmbH (http://www.datageekery.com)
 * All rights reserved.
 *
 * This work is triple-licensed under ASL 2.0, AGPL 3.0, and jOOQ EULA
 * =============================================================================
 * You may choose which license applies to you:
 *
 * - If you're using this work with Open Source databases, you may choose
 *   ASL 2.0 or jOOQ EULA.
 * - If you're using this work with at least one commercial database, you may
 *   choose AGPL 3.0 or jOOQ EULA.
 *
 * For more information, please visit http://www.jooq.org/licenses
 *
 * Apache Software License 2.0:
 * -----------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * AGPL 3.0
 * -----------------------------------------------------------------------------
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this library.
 * If not, see http://www.gnu.org/licenses.
 *
 * jOOQ End User License Agreement:
 * -----------------------------------------------------------------------------
 * This library is commercial software; you may not redistribute it nor
 * modify it.
 *
 * This library is distributed with a LIMITED WARRANTY. See the jOOQ End User
 * License Agreement for more details: http://www.jooq.org/eula
 */
package org.jooq.impl;

import java.util.Arrays;
import java.util.List;

import org.jooq.ArrayRecord;
import org.jooq.Attachable;
import org.jooq.AttachableInternal;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * A common base class for {@link Record} and {@link ArrayRecord}
 * <p>
 * This base class takes care of implementing similar {@link Attachable} and
 * {@link Object#equals(Object)}, {@link Object#hashCode()} behaviour.
 *
 * @author Lukas Eder
 */
abstract class AbstractStore implements AttachableInternal {

    /**
     * Generated UID
     */
    private static final long serialVersionUID = -2989496800221194411L;

    private Configuration     configuration;

    AbstractStore() {
        this(null);
    }

    AbstractStore(Configuration configuration) {
        this.configuration = configuration;
    }

    // -------------------------------------------------------------------------
    // The Attachable API
    // -------------------------------------------------------------------------

    abstract List<Attachable> getAttachables();

    @Override
    public final void attach(Configuration c) {
        configuration = c;

        for (Attachable attachable : getAttachables()) {
            attachable.attach(c);
        }
    }

    @Override
    public final void detach() {
        attach(null);
    }

    @Override
    public final Configuration configuration() {
        return configuration;
    }

    /**
     * This method is used in generated code!
     */
    protected final DSLContext create() {
        return DSL.using(configuration());
    }

    // -------------------------------------------------------------------------
    // equals and hashCode
    // -------------------------------------------------------------------------

    /**
     * This method coincides with {@link Record#size()} and
     * {@link ArrayRecord#size()}
     */
    abstract int size();

    /**
     * This method coincides with {@link Record#getValue(int)} and
     * <code>ArrayRecordImpl.getValue(int)</code>
     */
    abstract Object getValue(int index);

    @Override
    public int hashCode() {
        int hashCode = 1;

        for (int i = 0; i < size(); i++) {
            final Object obj = getValue(i);

            if (obj == null) {
                hashCode = 31 * hashCode;
            }

            // [#985] [#2045] Don't use obj.hashCode() on arrays, but avoid
            // calculating it as byte[] (BLOBs) can be quite large
            else if (obj.getClass().isArray()) {
                hashCode = 31 * hashCode;
            }
            else {
                hashCode = 31 * hashCode + obj.hashCode();
            }
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        // Note: keep this implementation in-sync with AbstractRecord.compareTo()!
        if (obj instanceof AbstractStore) {
            final AbstractStore that = (AbstractStore) obj;

            if (size() == that.size()) {
                for (int i = 0; i < size(); i++) {
                    final Object thisValue = getValue(i);
                    final Object thatValue = that.getValue(i);

                    // [#1850] Only return false early. In all other cases,
                    // continue checking the remaining fields
                    if (thisValue == null && thatValue == null) {
                        continue;
                    }

                    else if (thisValue == null || thatValue == null) {
                        return false;
                    }

                    // [#985] Compare arrays too.
                    else if (thisValue.getClass().isArray() && thatValue.getClass().isArray()) {

                        // Might be byte[]
                        if (thisValue.getClass() == byte[].class && thatValue.getClass() == byte[].class) {
                            if (!Arrays.equals((byte[]) thisValue, (byte[]) thatValue)) {
                                return false;
                            }
                        }

                        // Other primitive types are not expected
                        else if (!thisValue.getClass().getComponentType().isPrimitive() &&
                                 !thatValue.getClass().getComponentType().isPrimitive()) {
                            if (!Arrays.equals((Object[]) thisValue, (Object[]) thatValue)) {
                                return false;
                            }
                        }

                        else {
                            return false;
                        }
                    }
                    else if (!thisValue.equals(thatValue)) {
                        return false;
                    }
                }

                // If we got through the above loop, the two records are equal
                return true;
            }
        }

        return false;
    }
}
