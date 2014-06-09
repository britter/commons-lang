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

package org.apache.commons.lang3.time;

/**
 * <p>
 * <code>StopWatch</code> provides a convenient API for timings.
 * </p>
 * 
 * <p>
 * To start the watch, call {@link #start()}. At this point you can:
 * </p>
 * <ul>
 * <li>{@link #split()} the watch to get the time whilst the watch continues in the background. {@link #unsplit()} will
 * remove the effect of the split. At this point, these three options are available again.</li>
 * <li>{@link #suspend()} the watch to pause it. {@link #resume()} allows the watch to continue. Any time between the
 * suspend and resume will not be counted in the total. At this point, these three options are available again.</li>
 * <li>{@link #stop()} the watch to complete the timing session.</li>
 * </ul>
 * 
 * <p>
 * It is intended that the output methods {@link #toString()} and {@link #getTime()} should only be called after stop,
 * split or suspend, however a suitable result will be returned at other points.
 * </p>
 * 
 * <p>
 * NOTE: As from v2.1, the methods protect against inappropriate calls. Thus you cannot now call stop before start,
 * resume before suspend or unsplit before split.
 * </p>
 * 
 * <p>
 * 1. split(), suspend(), or stop() cannot be invoked twice<br>
 * 2. unsplit() may only be called if the watch has been split()<br>
 * 3. resume() may only be called if the watch has been suspend()<br>
 * 4. start() cannot be called twice without calling reset()
 * </p>
 * 
 * <p>This class is not thread-safe</p>
 * 
 * @since 2.0
 * @version $Id$
 */
public class StopWatch {

    private static final long NANO_2_MILLIS = 1000000L;

    private static abstract class State {

        public void reset(StopWatch watch) {
            watch.state = UNSTARTED;
            watch.splitState = SplitState.UNSPLIT;
        }

        public void start(StopWatch watch) {
            throw new IllegalStateException("Stopwatch already started. ");
        }

        public void stop(StopWatch watch) {
            throw new IllegalStateException("Stopwatch is not running. ");
        }

        public void split(StopWatch watch) {
            throw new IllegalStateException("Stopwatch is not running. ");
        }

        public void unsplit(StopWatch watch) {
            if (watch.splitState != SplitState.SPLIT) {
                throw new IllegalStateException("Stopwatch has not been split. ");
            }
            watch.splitState = SplitState.UNSPLIT;
        }

        public void suspend(StopWatch watch) {
            throw new IllegalStateException("Stopwatch must be running to suspend. ");
        }

        public void resume(StopWatch watch) {
            throw new IllegalStateException("Stopwatch must be suspended to resume. ");
        }

        public abstract long getNanoTime(StopWatch watch);
    }

    private static class UnstartedState extends State {
        @Override
        public void start(StopWatch watch) {
            watch.startTime = System.nanoTime();
            watch.startTimeMillis = System.currentTimeMillis();
            watch.state = RUNNING;
        }
        @Override
        public long getNanoTime(StopWatch watch) {
            return 0;
        }
    }

    private static class RunningState extends State {
        @Override
        public void stop(StopWatch watch) {
            watch.stopTime = System.nanoTime();
            watch.state = STOPPED;
        }
        @Override
        public void split(StopWatch watch) {
            watch.stopTime = System.nanoTime();
            watch.splitState = SplitState.SPLIT;
        }
        @Override
        public void suspend(StopWatch watch) {
            watch.stopTime = System.nanoTime();
            watch.state = SUSPENDED;
        }
        @Override
        public long getNanoTime(StopWatch watch) {
            return System.nanoTime() - watch.startTime;
        }
    }

    private static class SuspendedState extends State {
        @Override
        public void stop(StopWatch watch) {
            watch.state = STOPPED;
        }
        @Override
        public void resume(StopWatch watch) {
            watch.startTime += System.nanoTime() - watch.stopTime;
            watch.state = RUNNING;
        }
        @Override
        public long getNanoTime(StopWatch watch) {
            return watch.stopTime - watch.startTime;
        }
    }

    private static class StoppedState extends State {
        @Override
        public void start(StopWatch watch) {
            throw new IllegalStateException("Stopwatch must be reset before being restarted. ");
        }
        @Override
        public long getNanoTime(StopWatch watch) {
            return watch.stopTime - watch.startTime;
        }
    }

    private static final State UNSTARTED = new UnstartedState();
    private static final State RUNNING = new RunningState();
    private static final State SUSPENDED = new SuspendedState();
    private static final State STOPPED = new StoppedState();

    /**
     * Enumeration type which indicates the split status of stopwatch.
     */    
    private enum SplitState {
        SPLIT,
        UNSPLIT
    }
    /**
     * The current running state of the StopWatch.
     */
    private State state = UNSTARTED;

    /**
     * Whether the stopwatch has a split time recorded.
     */
    private SplitState splitState = SplitState.UNSPLIT;

    /**
     * The start time.
     */
    private long startTime;

    /**
     * The start time in Millis - nanoTime is only for elapsed time so we 
     * need to also store the currentTimeMillis to maintain the old 
     * getStartTime API.
     */
    private long startTimeMillis;

    /**
     * The stop time.
     */
    private long stopTime;

    /**
     * <p>
     * Constructor.
     * </p>
     */
    public StopWatch() {
        super();
    }

    /**
     * <p>
     * Start the stopwatch.
     * </p>
     * 
     * <p>
     * This method starts a new timing session, clearing any previous values.
     * </p>
     * 
     * @throws IllegalStateException
     *             if the StopWatch is already running.
     */
    public void start() {
        state.start(this);
    }


    /**
     * <p>
     * Stop the stopwatch.
     * </p>
     * 
     * <p>
     * This method ends a new timing session, allowing the time to be retrieved.
     * </p>
     * 
     * @throws IllegalStateException
     *             if the StopWatch is not running.
     */
    public void stop() {
        state.stop(this);
    }

    /**
     * <p>
     * Resets the stopwatch. Stops it if need be.
     * </p>
     * 
     * <p>
     * This method clears the internal values to allow the object to be reused.
     * </p>
     */
    public void reset() {
        state.reset(this);
    }

    /**
     * <p>
     * Split the time.
     * </p>
     * 
     * <p>
     * This method sets the stop time of the watch to allow a time to be extracted. The start time is unaffected,
     * enabling {@link #unsplit()} to continue the timing from the original start point.
     * </p>
     * 
     * @throws IllegalStateException
     *             if the StopWatch is not running.
     */
    public void split() {
        state.split(this);
    }

    /**
     * <p>
     * Remove a split.
     * </p>
     * 
     * <p>
     * This method clears the stop time. The start time is unaffected, enabling timing from the original start point to
     * continue.
     * </p>
     * 
     * @throws IllegalStateException
     *             if the StopWatch has not been split.
     */
    public void unsplit() {
        state.unsplit(this);
    }

    /**
     * <p>
     * Suspend the stopwatch for later resumption.
     * </p>
     * 
     * <p>
     * This method suspends the watch until it is resumed. The watch will not include time between the suspend and
     * resume calls in the total time.
     * </p>
     * 
     * @throws IllegalStateException
     *             if the StopWatch is not currently running.
     */
    public void suspend() {
        state.suspend(this);
    }

    /**
     * <p>
     * Resume the stopwatch after a suspend.
     * </p>
     * 
     * <p>
     * This method resumes the watch after it was suspended. The watch will not include time between the suspend and
     * resume calls in the total time.
     * </p>
     * 
     * @throws IllegalStateException
     *             if the StopWatch has not been suspended.
     */
    public void resume() {
        state.resume(this);
    }

    /**
     * <p>
     * Get the time on the stopwatch.
     * </p>
     * 
     * <p>
     * This is either the time between the start and the moment this method is called, or the amount of time between
     * start and stop.
     * </p>
     * 
     * @return the time in milliseconds
     */
    public long getTime() {
        return getNanoTime() / NANO_2_MILLIS;
    }
    /**
     * <p>
     * Get the time on the stopwatch in nanoseconds.
     * </p>
     * 
     * <p>
     * This is either the time between the start and the moment this method is called, or the amount of time between
     * start and stop.
     * </p>
     * 
     * @return the time in nanoseconds
     * @since 3.0
     */
    public long getNanoTime() {
        return state.getNanoTime(this);
    }

    /**
     * <p>
     * Get the split time on the stopwatch.
     * </p>
     * 
     * <p>
     * This is the time between start and latest split.
     * </p>
     * 
     * @return the split time in milliseconds
     * 
     * @throws IllegalStateException
     *             if the StopWatch has not yet been split.
     * @since 2.1
     */
    public long getSplitTime() {
        return getSplitNanoTime() / NANO_2_MILLIS;
    }
    /**
     * <p>
     * Get the split time on the stopwatch in nanoseconds.
     * </p>
     * 
     * <p>
     * This is the time between start and latest split.
     * </p>
     * 
     * @return the split time in nanoseconds
     * 
     * @throws IllegalStateException
     *             if the StopWatch has not yet been split.
     * @since 3.0
     */
    public long getSplitNanoTime() {
        if (this.splitState != SplitState.SPLIT) {
            throw new IllegalStateException("Stopwatch must be split to get the split time. ");
        }
        return this.stopTime - this.startTime;
    }

    /**
     * Returns the time this stopwatch was started.
     * 
     * @return the time this stopwatch was started
     * @throws IllegalStateException
     *             if this StopWatch has not been started
     * @since 2.4
     */
    public long getStartTime() {
        if (this.state == UNSTARTED) {
            throw new IllegalStateException("Stopwatch has not been started");
        }
        // System.nanoTime is for elapsed time
        return this.startTimeMillis;
    }

    /**
     * <p>
     * Gets a summary of the time that the stopwatch recorded as a string.
     * </p>
     * 
     * <p>
     * The format used is ISO 8601-like, <i>hours</i>:<i>minutes</i>:<i>seconds</i>.<i>milliseconds</i>.
     * </p>
     * 
     * @return the time as a String
     */
    @Override
    public String toString() {
        return DurationFormatUtils.formatDurationHMS(getTime());
    }

    /**
     * <p>
     * Gets a summary of the split time that the stopwatch recorded as a string.
     * </p>
     * 
     * <p>
     * The format used is ISO 8601-like, <i>hours</i>:<i>minutes</i>:<i>seconds</i>.<i>milliseconds</i>.
     * </p>
     * 
     * @return the split time as a String
     * @since 2.1
     */
    public String toSplitString() {
        return DurationFormatUtils.formatDurationHMS(getSplitTime());
    }

    /**
     * <p>
     * The method is used to find out if the StopWatch is started. A suspended
     * StopWatch is also started watch.
     * </p>
     *
     * @return boolean
     *             If the StopWatch is started.
     * @since 3.2
     */
    public boolean isStarted() {
        return state == RUNNING || state == SUSPENDED;
    }

    /**
     * <p>
     * This method is used to find out whether the StopWatch is suspended.
     * </p>
     *
     * @return boolean
     *             If the StopWatch is suspended.
     * @since 3.2
     */
    public boolean isSuspended() {
        return state == SUSPENDED;
    }

    /**
     * <p>
     * This method is used to find out whether the StopWatch is stopped. The
     * stopwatch which's not yet started and explicitly stopped stopwatch is
     * considered as stopped.
     * </p>
     *
     * @return boolean
     *             If the StopWatch is stopped.
     * @since 3.2
     */
    public boolean isStopped() {
        return state == STOPPED || state == UNSTARTED;
    }    

}
