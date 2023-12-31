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
package org.mmtk.plan.tutorial;

import org.mmtk.plan.*;
import org.mmtk.policy.CopySpace;
import org.mmtk.policy.MarkSweepSpace;
import org.mmtk.policy.Space;
import org.mmtk.utility.heap.VMRequest;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.ObjectReference;


/**
 * This class implements the global state of a a simple allocator
 * without a collector.
 */
@Uninterruptible
public class Tutorial extends StopTheWorld {

  /*****************************************************************************
   * Class variables
   */

  /**
   *
   */
	public static final int SCAN_MARK = 0;
  // public static final MarkSweepSpace msSpace = new MarkSweepSpace("mark-sweep", VMRequest.discontiguous());
  public static final MarkSweepSpace msSpace = new MarkSweepSpace("ms", VMRequest.discontiguous());
  //public static final ImmortalSpace msSpace = new ImmortalSpace("mark-sweep", VMRequest.create());
  public static final int MARK_SWEEP = msSpace.getDescriptor();

  public static final CopySpace nurserySpace = new CopySpace("nursery", false, VMRequest.highFraction(0.15f));
  public static final int NURSERY = nurserySpace.getDescriptor();
  


  /*****************************************************************************
   * Instance variables
   */

  /**
   *
   */
  public final Trace msTrace = new Trace(metaDataSpace);


  /*****************************************************************************
   * Collection
   */

  /**
   * {@inheritDoc}
   */
  @Inline
  @Override
  public final void collectionPhase(short phaseId) {
    //if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(false);
    if (phaseId == PREPARE) {
    	super.collectionPhase(phaseId);
    	nurserySpace.prepare(true);
    	msTrace.prepare();
    	msSpace.prepare(true);
    	return;
    }
    if (phaseId == CLOSURE) {
    	msTrace.prepare();
    	return;
    }
    if (phaseId == RELEASE) {
    	msTrace.release();
    	nurserySpace.release();
    	msSpace.release();super.collectionPhase(phaseId);
    	return;
    }
    super.collectionPhase(phaseId);
  }
  
  

  /*****************************************************************************
   * Accounting
   */

  /**
   * {@inheritDoc}
   * The superclass accounts for its spaces, we just
   * augment this with the default space's contribution.
   */
  @Override
  public int getPagesUsed() {
    return (msSpace.reservedPages() + super.getPagesUsed() + nurserySpace.reservedPages());
  }


  /*****************************************************************************
   * Miscellaneous
   */

  /**
   * {@inheritDoc}
   */
  @Interruptible
  @Override
  protected void registerSpecializedMethods() {
    super.registerSpecializedMethods();
    TransitiveClosure.registerSpecializedScan(SCAN_MARK, TutorialTraceLocal.class);
  }

  @Override
  public boolean willNeverMove(ObjectReference object) {
    if (Space.isInSpace(MARK_SWEEP, object))
      return true;
    return super.willNeverMove(object);
  }
  

  @Override
  public int getCollectionReserve() {
	  return nurserySpace.reservedPages() + super.getCollectionReserve();
  }
  // @Override
  // public int getPagesAvail(){
	//   return super.getPagesAvail()/2;
  // }

  @Override
  public int getPagesAvail(){
    return (getTotalPages() - getPagesReserved()) >> 1;
  }
  
}
