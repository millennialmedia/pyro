package com.millennialmedia.pyro.model.util;

import com.millennialmedia.pyro.model.RobotModel;

/**
 * Required base class for contributions to the
 * {@code com.millennialmedia.pyro.modelPostProcessor} extension point.
 * 
 * Model post-processors are executed in sequence by the parser right after the
 * file has been completed parsed. Post-processors are given a chance to
 * "fix up" some aspects of the model before it's returned to clients.
 * 
 * Contributed classes are executed in priority order (lowest goes first) and
 * priority values must be unique across all plugin contributions.
 * 
 * @author spaxton
 */
public abstract class AbstractModelPostProcessor {

	/**
	 * Modifies model state immediately after initial file parsing.
	 */
	public abstract void postProcess(RobotModel model);

}
