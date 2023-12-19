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
package java.lang;

import java.security.ProtectionDomain;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.jikesrvm.VM;
import org.jikesrvm.classlibrary.ClassLibraryHelpers;
import org.jikesrvm.classlibrary.ClassLoaderSupport;
import org.jikesrvm.classlibrary.JavaLangInstrument;
import org.jikesrvm.classloader.Atom;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.RVMField;

import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.Offset;

import org.jikesrvm.scheduler.RVMThread;

/**
 * Library support interface of Jikes RVM
 */
public class JikesRVMSupport {

  private static final Atom OFFSET_ATOM = Atom.findAsciiAtom("offset");
  private static final Atom VALUE_ATOM = Atom.findAsciiAtom("value");
  private static final RVMField JavaLangStringCharsField = RVMType.JavaLangStringType.findDeclaredField(VALUE_ATOM);
  private static final RVMField JavaLangStringOffsetField = RVMType.JavaLangStringType.findDeclaredField(OFFSET_ATOM);
  private static final Offset STRING_CHARS_OFFSET = JavaLangStringCharsField.getOffset();
  private static final Offset STRING_OFFSET_OFFSET = JavaLangStringOffsetField.getOffset();

  /**
   * Call the Object finalize method on the given object
   */
  public static void invokeFinalize(Object o)  throws Throwable {
    o.finalize();
  }

  public static Instrumentation createInstrumentation() throws Exception {
    // FIXME OPENJDK/ICEDTEA initializeInstrumentation isn't implemented yet.
    // OpenJDK 6 doesn't seem to provide any suitable hooks for implementation of instrumentation.
    // The instrumentation in OpenJDK is done via native code which doesn't seem to be called automatically.
    // That means we have to (re-)implement instrumentation ourselves and do it during classloading.
    // Some relevant code in OpenJDK 6:
    // JPLISAgent.c (openjdk/jdk/src/share/instrument)
    // JPLISAgent.h (openjdk/jdk/src/share/instrument)
    // sun.instrument.InstrumentationImpl (openjdk/jdk/src/share/classes/sun/instrument)
    Class<?> instrumentationClass = Class.forName("sun.instrument.InstrumentationImpl");
    Class[] constructorParameters = {long.class, boolean.class, boolean.class};
    Constructor<?> constructor = instrumentationClass.getDeclaredConstructor(constructorParameters);
    constructor.setAccessible(true);
    Object[] parameter = {Long.valueOf(0L), Boolean.FALSE, Boolean.FALSE};
    Instrumentation instrumenter = (Instrumentation)constructor.newInstance(parameter);
    constructor.setAccessible(false);
    Method m = instrumentationClass.getDeclaredMethod("transform", ClassLoader.class, String.class, Class.class, ProtectionDomain.class, byte[].class, boolean.class);
    m.setAccessible(true);
    JavaLangInstrument.setTransformMethod(m);
    return instrumenter;
  }

  public static void initializeInstrumentation(Instrumentation instrumenter) {
    JavaLangInstrument.setInstrumenter(instrumenter);
  }

  public static Class<?>[] getAllLoadedClasses() {
    return ClassLoaderSupport.getAllLoadedClasses();
  }

  public static Class<?>[] getInitiatedClasses(ClassLoader classLoader) {
    return ClassLoaderSupport.getInitiatedClasses(classLoader);
  }

  public static Class<?> createClass(RVMType type) {
    Class<?> createdClass = ClassLibraryHelpers.allocateObjectForClassAndRunNoArgConstructor(java.lang.Class.class);
    RVMField rvmTypeField = ClassLibraryHelpers.rvmTypeField;
    rvmTypeField.setObjectValueUnchecked(createdClass, type);
    return createdClass;
  }

  public static Class<?> createClass(RVMType type, ProtectionDomain pd) {
    Class<?> c = createClass(type);
    setClassProtectionDomain(c, pd);
    return c;
  }

  public static RVMType getTypeForClass(Class<?> c) {
    RVMField rvmTypeField = ClassLibraryHelpers.rvmTypeField;
    return (RVMType) rvmTypeField.getObjectUnchecked(c);
  }

  @Uninterruptible
  public static RVMType getTypeForClassUninterruptible(Class<?> c) {
    RVMField rvmTypeField = ClassLibraryHelpers.rvmTypeField;
    return (RVMType) rvmTypeField.getObjectValueUnchecked(c);
  }

  public static void setClassProtectionDomain(Class<?> c, ProtectionDomain pd) {
    RVMField protectionDomainField = ClassLibraryHelpers.protectionDomainField;
    protectionDomainField.setObjectValueUnchecked(c, pd);
  }

  /***
   * String stuff
   * */

  @Uninterruptible
  public static char[] getBackingCharArray(String str) {
    RVMType typeForClass = JikesRVMSupport.getTypeForClassUninterruptible(String.class);
    RVMField[] instanceFields = typeForClass.getInstanceFields();
    for (RVMField f : instanceFields) {
      if (f.getName() == VALUE_ATOM) {
        return  (char[]) f.getObjectValueUnchecked(str);
      }
    }
    VM.sysFail("field 'value' not found");
    return null;
  }

  @Uninterruptible
  public static int getStringLength(String str) {
    // Note: made uninterruptible via AnnotationAdder
    return str.length();
  }

  @Uninterruptible
  public static int getStringOffset(String str) {
    RVMType typeForClass = JikesRVMSupport.getTypeForClassUninterruptible(String.class);
    RVMField[] instanceFields = typeForClass.getInstanceFields();
    for (RVMField f : instanceFields) {
      if (f.getName() == OFFSET_ATOM) {
        return f.getIntValueUnchecked(str);
      }
    }
    VM.sysFail("field offset not found");
    return -1;
  }

  public static String newStringWithoutCopy(char[] data, int offset, int count) {
    //No back door
    return new String(data, offset, count);
  }


  /***
   * Thread stuff
   * */
  public static Thread createThread(RVMThread vmdata, String myName) {
    ThreadGroup tg = null;
    if (vmdata.isSystemThread()) {
      tg = RVMThread.getThreadGroupForSystemThreads();
    } else if (vmdata.getJavaLangThread() != null) {
      tg = vmdata.getJavaLangThread().getThreadGroup();
    }
    if (VM.VerifyAssertions) VM._assert(tg != null);
    Thread thread = new Thread(tg, myName);
    setThread(vmdata, thread);
    return thread;
  }

  public static void setThread(RVMThread vmdata, Thread thread) {
    RVMField rvmThreadField = ClassLibraryHelpers.rvmThreadField;
    rvmThreadField.setObjectValueUnchecked(thread, vmdata);
  }

  public static RVMThread getThread(Thread thread) {
    if (thread == null)
      return null;
    else {
      RVMField rvmThreadField = ClassLibraryHelpers.rvmThreadField;
      RVMThread realRvmThread = (RVMThread) rvmThreadField.getObjectUnchecked(thread);
      return realRvmThread;
    }
  }

  public static void threadDied(Thread thread) {
    ThreadGroup threadGroup = thread.getThreadGroup();
    if (threadGroup != null) {
      threadGroup.remove(thread);
    } else {
      if (VM.VerifyAssertions) {
        VM._assert(threadGroup != null, "Every thread must have a threadGroup in OpenJDK");
      }
    }
  }
  public static Throwable getStillBorn(Thread thread) {
    return null;
  }
  public static void setStillBorn(Thread thread, Throwable stillborn) {
  }
  /***
   * Enum stuff
   */
   @Uninterruptible
  public static int getEnumOrdinal(Enum<?> e) {
     return e.ordinal();
   }
  @Uninterruptible
  public static String getEnumName(Enum<?> e) {
    return e.name();
  }
}
