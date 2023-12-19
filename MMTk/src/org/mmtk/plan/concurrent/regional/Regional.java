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
package org.mmtk.plan.concurrent.regional;

import org.mmtk.plan.Phase;
import org.mmtk.plan.Plan;
import org.mmtk.plan.Trace;
import org.mmtk.plan.TransitiveClosure;
import org.mmtk.plan.concurrent.Concurrent;
import org.mmtk.policy.RegionSpace;
import org.mmtk.policy.Space;
import org.mmtk.utility.Constants;
import org.mmtk.utility.HeaderByte;
import org.mmtk.utility.deque.SharedDeque;
import org.mmtk.utility.heap.VMRequest;
import org.mmtk.utility.options.*;
import org.mmtk.utility.sanitychecker.SanityChecker;
import org.mmtk.vm.VM;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.Interruptible;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.AddressArray;
import org.vmmagic.unboxed.ObjectReference;

/**
 * This class implements a simple region-based collector.
 */
@Uninterruptible
public class Regional extends Concurrent {

  public static final RegionSpace regionSpace = new RegionSpace("region", VMRequest.discontiguous());
  public static final int RS = regionSpace.getDescriptor();
  public final Trace markTrace = new Trace(metaDataSpace);
  public final Trace forwardTrace = new Trace(metaDataSpace);
  public static AddressArray blocksSnapshot, relocationSet;
  public final SharedDeque modbufPool = new SharedDeque("modBufs", metaDataSpace, 1);

  static {
    Options.concurrentCleanup = new ConcurrentCleanup();
    Options.concurrentCollectionSetSelection = new ConcurrentCollectionSetSelection();
    Options.concurrentEagerCleanup = new ConcurrentEagerCleanup();
    Options.g1ReservePercent = new G1ReservePercent();
    Options.g1InitiatingHeapOccupancyPercent = new G1InitiatingHeapOccupancyPercent();
    Options.g1GCLiveThresholdPercent = new G1GCLiveThresholdPercent();
    Options.g1MaxNewSizePercent = new G1MaxNewSizePercent();
    Options.g1NewSizePercent = new G1NewSizePercent();
    Options.g1HeapWastePercent = new G1HeapWastePercent();
    regionSpace.makeAllocAsMarked();
    smallCodeSpace.makeAllocAsMarked();
    nonMovingSpace.makeAllocAsMarked();
  }

  public static final int ALLOC_RS = Plan.ALLOC_DEFAULT;
  public static final int SCAN_MARK = 0;
  public static final int SCAN_FORWARD = 1;

  /* Phases */
  public static final short EVACUATE = Phase.createSimple("evacuate");
  public static final short FORWARD_PREPARE = Phase.createSimple("forward-prepare");
  public static final short FORWARD_CLOSURE = Phase.createSimple("forward-closure");
  public static final short FORWARD_RELEASE = Phase.createSimple("forward-release");
  // Relocation set selection phases
  public static final short RELOCATION_SET_SELECTION = Phase.createSimple("relocation-set-selection");
  public static final short preemptConcurrentRelocationSetSelection = Phase.createComplex("preempt-relocation-set-selection", null,
      Phase.scheduleCollector(RELOCATION_SET_SELECTION));
  public static final short CONCURRENT_RELOCATION_SET_SELECTION = Phase.createConcurrent("concurrent-relocation-set-selection",
      Phase.scheduleComplex(preemptConcurrentRelocationSetSelection));
  // Eager cleanup phases
  public static final short EAGER_CLEANUP = Phase.createSimple("eager-cleanup");
  public static final short preemptConcurrentEagerCleanup = Phase.createComplex("preempt-concurrent-eager-cleanup", null,
      Phase.scheduleCollector(EAGER_CLEANUP));
  public static final short CONCURRENT_EAGER_CLEANUP = Phase.createConcurrent("concurrent-eager-cleanup",
      Phase.scheduleComplex(preemptConcurrentEagerCleanup));
  // Cleanup phases
  public static final short CLEANUP = Phase.createSimple("cleanup");
  public static final short preemptConcurrentCleanup = Phase.createComplex("preempt-concurrent-cleanup", null,
      Phase.scheduleCollector(CLEANUP));
  public static final short CONCURRENT_CLEANUP = Phase.createConcurrent("concurrent-cleanup",
      Phase.scheduleComplex(preemptConcurrentCleanup));


  public short _collection = Phase.createComplex("_collection", null,
    Phase.scheduleComplex  (initPhase),
    // Mark
    Phase.scheduleComplex  (rootClosurePhase),
    Phase.scheduleComplex  (refTypeClosurePhase),
    Phase.scheduleComplex  (completeClosurePhase),
    // Select relocation sets
    Phase.scheduleCollector(RELOCATION_SET_SELECTION),
    Phase.scheduleCollector(EAGER_CLEANUP),
    // Evacuate
    Phase.scheduleCollector(EVACUATE),
    // Update refs
    Phase.scheduleMutator  (FORWARD_PREPARE),
    Phase.scheduleCollector(FORWARD_PREPARE),
    Phase.scheduleGlobal   (FORWARD_PREPARE),
    Phase.scheduleMutator  (PREPARE_STACKS),
    Phase.scheduleGlobal   (PREPARE_STACKS),
    Phase.scheduleCollector(STACK_ROOTS),
    Phase.scheduleGlobal   (STACK_ROOTS),
    Phase.scheduleCollector(ROOTS),
    Phase.scheduleGlobal   (ROOTS),
    Phase.scheduleCollector(FORWARD_CLOSURE),
    Phase.scheduleCollector(SOFT_REFS),
    Phase.scheduleCollector(FORWARD_CLOSURE),
    Phase.scheduleCollector(WEAK_REFS),
    Phase.scheduleCollector(FINALIZABLE),
    Phase.scheduleCollector(FORWARD_CLOSURE),
    Phase.scheduleCollector(PHANTOM_REFS),
//    Phase.scheduleComplex  (forwardPhase),
    Phase.scheduleMutator  (FORWARD_RELEASE),
    Phase.scheduleCollector(FORWARD_RELEASE),
    Phase.scheduleGlobal   (FORWARD_RELEASE),

    // Cleanup
    Phase.scheduleCollector(CLEANUP),
    Phase.scheduleComplex  (finishPhase)
  );

  /**
   * Constructor
   */
  public Regional() {
    collection = _collection;
  }

  @Override
  @Interruptible
  public void processOptions() {
    super.processOptions();
    /* Set up the concurrent marking phase */
//    if (Options.concurrentCollectionSetSelection.getValue())
//      replacePhase(Phase.scheduleCollector(RELOCATION_SET_SELECTION), Phase.scheduleConcurrent(CONCURRENT_RELOCATION_SET_SELECTION));
//    if (Options.concurrentEagerCleanup.getValue())
//      replacePhase(Phase.scheduleCollector(EAGER_CLEANUP), Phase.scheduleConcurrent(CONCURRENT_EAGER_CLEANUP));
//    if (Options.concurrentCleanup.getValue())
//      replacePhase(Phase.scheduleCollector(CLEANUP), Phase.scheduleConcurrent(CONCURRENT_CLEANUP));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Inline
  public void collectionPhase(short phaseId) {
    if (phaseId == INITIATE) {
      super.collectionPhase(INITIATE);
      RegionalCollector.concurrentEagerCleanupExecuted = false;
      RegionalCollector.concurrentCleanupExecuted = false;
      RegionalCollector.concurrentRelocationSetSelectionExecuted = false;
      return;
    }

    if (phaseId == PREPARE) {
      super.collectionPhase(PREPARE);
      regionSpace.prepare();
      markTrace.prepareNonBlocking();
//      modbufPool.clearDeque(1);
//      modbufPool.reset();
//      if (VM.VERIFY_ASSERTIONS) modbufPool.assertExhausted();
      modbufPool.prepareNonBlocking();
      modbufPool.clearDeque(1);
//      HeaderByte.flip();
      return;
    }

    if (phaseId == CLOSURE) {
      return;
    }

    if (phaseId == RELEASE) {
      markTrace.release();
      modbufPool.reset();
      super.collectionPhase(RELEASE);
      return;
    }

    if (phaseId == FORWARD_PREPARE) {
      super.collectionPhase(PREPARE);
      forwardTrace.prepare();
      regionSpace.prepare();
      return;
    }

    if (phaseId == FORWARD_RELEASE) {
      forwardTrace.release();
      regionSpace.release();

//      modbufPool.clearDeque(1);
      super.collectionPhase(RELEASE);
      return;
    }

    super.collectionPhase(phaseId);
  }

  /****************************************************************************
   *
   * Accounting
   */

  final int BOOT_PAGES = VM.AVAILABLE_START.diff(VM.HEAP_START).toInt() / Constants.BYTES_IN_PAGE;
  final float RESERVE_PERCENT = Options.g1ReservePercent.getValue() / 100f;

  @Override
  protected boolean collectionRequired(boolean spaceFull, Space space) {
//    int totalPages = getTotalPages();
//    if (getPagesAvail() - BOOT_PAGES < totalPages * RESERVE_PERCENT) {
//      return true;
//    }
//    return super.collectionRequired(spaceFull, space);
    final int totalPages = getTotalPages();
    final boolean heapFull = ((totalPages - getPagesReserved()) * 10) < totalPages;
    return (spaceFull || heapFull);
  }

//  @Override
//  protected boolean concurrentCollectionRequired() {
//    int totalPages = getTotalPages();
//    int availPages = getPagesAvail() - BOOT_PAGES;
//    return !Phase.concurrentPhaseActive() && (availPages < (totalPages * INIT_HEAP_OCCUPANCY_PERCENT));
//  }

  final float INIT_HEAP_OCCUPANCY_PERCENT = Options.g1InitiatingHeapOccupancyPercent.getValue();
  @Override
  protected boolean concurrentCollectionRequired() {
    return !Phase.concurrentPhaseActive() &&
        ((getPagesReserved() * 100) / (getTotalPages())) > INIT_HEAP_OCCUPANCY_PERCENT;
//    return !Phase.concurrentPhaseActive() &&
//        ((getPagesReserved() * 100) / getTotalPages()) > INIT_HEAP_OCCUPANCY_PERCENT;
  }

//  @Override
//  protected boolean collectionRequired(boolean spaceFull, Space space) {
//    int usedPages = getPagesUsed() - metaDataSpace.reservedPages();
//    int totalPages = getTotalPages() - metaDataSpace.reservedPages();
//    if ((totalPages - usedPages) < (totalPages * RESERVE_PERCENT)) {
//      return true;
//    }
//    return super.collectionRequired(spaceFull, space);
//  }
//
//  @Override
//  protected boolean concurrentCollectionRequired() {
//    int usedPages = getPagesUsed() - metaDataSpace.reservedPages();
//    int totalPages = getTotalPages() - metaDataSpace.reservedPages();
//    return !Phase.concurrentPhaseActive() && ((usedPages * 100) > (totalPages * INIT_HEAP_OCCUPANCY_PERCENT));
//  }
//
//  final float RESERVE_PERCENT = Options.g1ReservePercent.getValue() / 100;
//  final float INIT_HEAP_OCCUPANCY_PERCENT = Options.g1InitiatingHeapOccupancyPercent.getValue();

  /**
   * Return the number of pages reserved for copying.
   */
  @Override
  public final int getCollectionReserve() {
    // we must account for the number of pages required for copying,
    // which equals the number of semi-space pages reserved
    return regionSpace.getCollectionReserve() + super.getCollectionReserve(); // TODO: Fix this
  }

  @Override
  public int sanityExpectedRC(ObjectReference object, int sanityRootRC) {
    Space space = Space.getSpaceForObject(object);
    // Nursery
    if (space == regionSpace) {
      // We are never sure about objects in RS.
      // This is not very satisfying but allows us to use the sanity checker to
      // detect dangling pointers.
      return SanityChecker.UNSURE;
    }
    return super.sanityExpectedRC(object, sanityRootRC);
  }

  @Override
  @Inline
  public int getPagesUsed() {
    return super.getPagesUsed() + regionSpace.reservedPages();
  }

  @Override
  public boolean willNeverMove(ObjectReference object) {
    if (Space.isInSpace(RS, object)) return false;
    return super.willNeverMove(object);
  }

  @Override
  @Interruptible
  protected void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_MARK, org.mmtk.plan.concurrent.regional.RegionalMarkTraceLocal.class);
    TransitiveClosure.registerSpecializedScan(SCAN_FORWARD, org.mmtk.plan.concurrent.regional.RegionalForwardTraceLocal.class);
    super.registerSpecializedMethods();
  }
}
