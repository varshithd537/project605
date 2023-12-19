/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.mmtk.utility.heap;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mmtk.harness.Harness;

public class SpaceDescriptorTest64 extends AbstractSpaceDescriptorTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    Harness.initArchitecture(Arrays.asList("bits=64","timeout=600"));
    Harness.initOnce();
 }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

}
