/*
 * Copyright (c) 1996, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.jikesrvm.classlibrary.openjdk.replacements;

import java.lang.reflect.Field;

import org.jikesrvm.classloader.RVMClass;
import org.jikesrvm.classloader.RVMField;
import org.vmmagic.pragma.ReplaceClass;
import org.vmmagic.pragma.ReplaceMember;

@ReplaceClass(className = "java.lang.reflect.Field")
public class java_lang_reflect_Field {

  @ReplaceMember
  Field copy() {
    // "copy" field
    Field source = (Field) (Object) this;
    RVMField rvmField = java.lang.reflect.JikesRVMSupport.getFieldOf(source);
    Field newField = java.lang.reflect.JikesRVMSupport.createField(rvmField);
    // set rest of fields from old field
    RVMClass typeForClass = java.lang.JikesRVMSupport.getTypeForClass(Field.class).asClass();

    // set root field
    RVMField rootField = java.lang.reflect.JikesRVMHelpers.findFieldByName(typeForClass, "root");
    rootField.setObjectValueUnchecked(newField, source);

    // copy fieldAccessor and overrideFieldAccessor from this
    RVMField fieldAccessorField = java.lang.reflect.JikesRVMHelpers.findFieldByName(typeForClass, "fieldAccessor");
    Object sourceFieldAccessor = fieldAccessorField.getObjectUnchecked(source);
    fieldAccessorField.setObjectValueUnchecked(newField, sourceFieldAccessor);

    RVMField overrideFieldAccessorField = java.lang.reflect.JikesRVMHelpers.findFieldByName(typeForClass, "overrideFieldAccessor");
    Object sourceOverrideFieldAccessor = overrideFieldAccessorField.getObjectUnchecked(source);
    overrideFieldAccessorField.setObjectValueUnchecked(newField, sourceOverrideFieldAccessor);

    return newField;
  }



}
