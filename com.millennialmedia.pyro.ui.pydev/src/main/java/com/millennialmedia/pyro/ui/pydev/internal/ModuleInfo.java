package com.millennialmedia.pyro.ui.pydev.internal;

import org.python.pydev.core.IModule;

/**
 * A value object containing resolved information about Python modules.
 * 
 * @author spaxton
 */
public class ModuleInfo {
	private String libraryName;
	private IModule module;
	private String remainingPath;

	public ModuleInfo(String libraryName, IModule module, String remainingPath) {
		this.libraryName = libraryName;
		this.module = module;
		this.remainingPath = remainingPath;
	}

	public String getLibraryName() {
		return libraryName;
	}

	public IModule getModule() {
		return module;
	}

	public String getRemainingPath() {
		return remainingPath;
	}

}