/*******************************************************************************
 * Copyright (c) 2010 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.base;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.docfetcher.base.annotations.ForDevelopment;

import org.aspectj.lang.annotation.SuppressAjWarnings;

/**
 * This class can be used for measuring time inside loops, which is useful for
 * finding performance bottlenecks and/or computing averaged execution times.
 * Usage:
 * <ul>
 * <li>Create an instance of this class before the loop.</li>
 * <li>Call {@link #beginLoop()} inside the loop, at the beginning of the loop
 * body. This will reset the internal timer for every iteration of the loop.</li>
 * <li>Place labelled checkpoints across the body of the loop with
 * {@link #addCheckpoint(String)}. The checkpoint labels should be unique.</li>
 * <li>After the end of the loop, retrieve the accumulated time values using
 * {@link #getTimes()} and print them out. The printout shows how much time in
 * milliseconds has passed between the checkpoints when the loop was run.</li>
 * </ul>
 * To reset this class, call {@link #clearTimes()}.
 * 
 * @author Tran Nam Quang
 */
@ForDevelopment
public class LoopTimer {
	
	private long lastTime = System.currentTimeMillis();
	private Map<String, Long> timeMap = new LinkedHashMap<String, Long> ();
	
	/**
	 * This method should be called at the beginning of the loop body.
	 * 
	 * @see LoopTimer
	 */
	public void beginLoop() {
		lastTime = System.currentTimeMillis();
	}
	
	/**
	 * Add a checkpoint with the given label at this execution point. Labels
	 * must be unique for each checkpoint.
	 * 
	 * @see LoopTimer
	 */
	public void addCheckpoint(String checkpointLabel) {
		long timeDiff = System.currentTimeMillis() - lastTime;
		Long sum = timeMap.get(checkpointLabel);
		if (sum == null)
			sum = 0L;
		timeMap.put(checkpointLabel, sum + timeDiff);
		lastTime = System.currentTimeMillis();
	}
	
	/**
	 * Clears the internally accumulated time values.
	 * 
	 * @see LoopTimer
	 */
	public void clearTimes() {
		timeMap.clear();
	}
	
	/**
	 * Returns a printout of how much time in milliseconds passed between each
	 * checkpoint and the previous checkpoint.
	 * 
	 * @see LoopTimer
	 */
	public String getTimes() {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Long> entry : timeMap.entrySet())
			sb.append(entry.getKey() + ": " + entry.getValue() + "\n");
		return sb.toString();
	}
	
	/**
	 * Writes the printout returned by {@link #getTimes()} to the standard
	 * output.
	 * 
	 * @see LoopTimer
	 */
	@SuppressAjWarnings
	public void printTimes() {
		System.out.println(getTimes());
	}

}
