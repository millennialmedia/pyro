package com.millennialmedia.pyro.ui.pydev.internal;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class LibraryInfo {

	private List<String> orderedLibraries = new ArrayList<String>();
	private Multimap<String, ModuleInfo> moduleMap = ArrayListMultimap.create();
	
	public Multimap<String, ModuleInfo> getModuleMap() {
		return moduleMap;
	}

	public List<String> getOrderedLibraries() {
		return orderedLibraries;
	}
	
}
