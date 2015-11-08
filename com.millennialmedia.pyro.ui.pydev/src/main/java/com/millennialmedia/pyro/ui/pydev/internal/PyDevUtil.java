package com.millennialmedia.pyro.ui.pydev.internal;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.structure.Tuple;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.millennialmedia.pyro.model.ModelManager;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;

/**
 * Utility methods for PyDev integration.
 * 
 * @author spaxton
 */
public class PyDevUtil {
	private static final String PROPSKEY_NON_BUILTIN_MODULEMAP_CACHE = "PYDEV_NON_BUILTIN_MODULE_MAP_CACHE";
	private static final String PROPSKEY_BUILTIN_MODULEMAP_CACHE = "PYDEV_BUILTIN_MODULE_MAP_CACHE";
	
	private static List<String> standardLibsList = Arrays.asList(new String[] { 
		"BuiltIn", 
		"Collections", 
		"Dialogs", 
		"OperatingSystem", 
		"Process", 
		"Screenshot", 
		"String", 
		"Telnet",
		"XML"
	});

	/**
	 * Collects and returns a multimap containing all python modules referenced directly
	 * or indirectly from the current editor's robot file.  This map omits the Robot
	 * Framework's built-in standard libraries, which can be separately retrieved from
	 * getBuiltInLibraryModules() 
	 */
	@SuppressWarnings("unchecked")
	public static Multimap<String, ModuleInfo> getNonBuiltInLibraryModules(RobotFrameworkEditor editor) {
		// first try to retrieve a cached module map from the robot model.
		// any editing changes at all (including changing library references) will force
		// a model re-parse, so the cache can never be stale unless the pythonpath was 
		// changed.  in that case a simple close/reopen or single character change in the
		// editor will catch up, so I will skip extra listener/notification code here 
		RobotModel model = editor.getModel();
		Multimap<String, ModuleInfo> cachedMap = (Multimap<String, ModuleInfo>) model.getCustomProperties().get(PROPSKEY_NON_BUILTIN_MODULEMAP_CACHE);
		if (cachedMap != null) {
			return cachedMap;
		}
		
		IFile sourceFile = PathUtil.getEditorFile(editor);
		List<IFile> filesVisited = new ArrayList<IFile>();

		// load all the library modules directly referenced from the source file
		List<String> libraries = ModelUtil.getLibraries(ModelManager.getManager().getModel(sourceFile));
		Multimap<String, ModuleInfo> libraryModuleMap = PyDevUtil.findModules(libraries, sourceFile);

		filesVisited.add(sourceFile);
		
		// traverse all resource file dependencies recursively and pull in additional library references from them too
		addTransitiveReferencedLibraryModules(libraryModuleMap, filesVisited, sourceFile);

		// cache for subsequent requests
		model.getCustomProperties().put(PROPSKEY_NON_BUILTIN_MODULEMAP_CACHE, libraryModuleMap);
		return libraryModuleMap;
	}
	
	/**
	 * Collects and returns a module map for any standard Robot Framework libraries that are
	 * referenced by the current editor's file. 
	 */
	@SuppressWarnings("unchecked")
	public static Multimap<String, ModuleInfo> getBuiltInLibraryModules(RobotFrameworkEditor editor) {
		RobotModel model = editor.getModel();
		Multimap<String, ModuleInfo> cachedMap = (Multimap<String, ModuleInfo>) model.getCustomProperties().get(PROPSKEY_BUILTIN_MODULEMAP_CACHE);
		if (cachedMap != null) {
			return cachedMap;
		}

		Multimap<String, ModuleInfo> moduleMap = ArrayListMultimap.create();
		
		List<String> referencedLibraries = PyDevUtil.getReferencedLibraries(PathUtil.getEditorFile(editor));
		referencedLibraries.add("BuiltIn");

		for (String libraryName : referencedLibraries) {
			if (!standardLibsList.contains(libraryName)) {
				continue;
			}

			List<ModuleInfo> moduleInfos = PyDevUtil.findStandardLibModule(libraryName, PathUtil.getEditorFile(editor));
			if (moduleInfos != null) {
				moduleMap.putAll(libraryName, moduleInfos);
			}
		}

		// cache for subsequent requests
		model.getCustomProperties().put(PROPSKEY_BUILTIN_MODULEMAP_CACHE, moduleMap);
		return moduleMap;
	}
	
	private static void addTransitiveReferencedLibraryModules(Multimap<String, ModuleInfo> libraryModuleMap, List<IFile> filesVisited, IFile referencedFile) {
		// add library references from the local file
		List<String> libraries = ModelUtil.getLibraries(ModelManager.getManager().getModel(referencedFile));
		Multimap<String, ModuleInfo> additionalLibrariesModuleMap = PyDevUtil.findModules(libraries, referencedFile);

		libraryModuleMap.putAll(additionalLibrariesModuleMap);

		filesVisited.add(referencedFile);
		
		// now check any referenced resource files
		RobotModel targetModel = ModelManager.getManager().getModel(referencedFile);
		List<String> resourceFilePaths = ModelUtil.getResourceFilePaths(targetModel);

		for (String resourceFilePath : resourceFilePaths) {
			IResource resource = PathUtil.getResourceForPath(referencedFile, resourceFilePath);
			if (resource != null && resource instanceof IFile) {
				IFile targetFile = (IFile) resource;
				// avoid cycles, but otherwise recursively add additional library modules
				if (!filesVisited.contains(targetFile)) {
					addTransitiveReferencedLibraryModules(libraryModuleMap, filesVisited, targetFile);
				}
			}
		}
	}
	
	public static List<String> getReferencedLibraries(IFile sourceFile) {
		List<String> referencedLibraries = new ArrayList<String>();
		List<IFile> filesVisited = new ArrayList<IFile>();
		collectReferencedLibraryNames(referencedLibraries, filesVisited, sourceFile);
		return referencedLibraries;
	}
	
	private static void collectReferencedLibraryNames(List<String> libraryNames, List<IFile> filesVisited, IFile sourceFile) {
		filesVisited.add(sourceFile);
		RobotModel model = ModelManager.getManager().getModel(sourceFile);
		libraryNames.addAll(ModelUtil.getLibraries(model));
		
		// now check any referenced resource files
		List<String> resourceFilePaths = ModelUtil.getResourceFilePaths(model);

		for (String resourceFilePath : resourceFilePaths) {
			IResource resource = PathUtil.getResourceForPath(sourceFile, resourceFilePath);
			if (resource != null && resource instanceof IFile) {
				IFile targetFile = (IFile) resource;
				// avoid cycles, but otherwise recursively add additional library modules
				if (!filesVisited.contains(targetFile)) {
					collectReferencedLibraryNames(libraryNames, filesVisited, targetFile);
				}
			}
		}
	}
	
	private static Multimap<String, ModuleInfo> findModules(List<String> libraryNames, IFile sourceFile) {
		IProject project = sourceFile.getProject();

		Multimap<String, ModuleInfo> moduleMap = ArrayListMultimap.create();
		PythonNature nature = PythonNature.getPythonNature(project);
		if (nature != null) {
			IPythonPathNature pathNature = nature.getPythonPathNature();

			for (String libraryName : libraryNames) {
				boolean foundModule = false;
				ModuleInfo info = null;
				try {
					if (libraryName.contains("${CURDIR}")) {
						info = getModuleInfo(libraryName, 
								sourceFile.getLocation().removeLastSegments(1).toOSString(), 
								libraryName.substring(libraryName.lastIndexOf("${CURDIR}") + 9));
						if (info != null) {
							moduleMap.put(libraryName, info);
							foundModule = true;
						}
					} else {
						// first try to find a module in a relative-path file
						info = getModuleInfo(libraryName, 
								sourceFile.getLocation().removeLastSegments(1).toOSString(), 
								libraryName);
						if (info != null) {
							moduleMap.put(libraryName, info);
							foundModule = true;
						} 
						
						// search across the in-project source folders from the pythonpath
						if (!foundModule && pathNature != null) {
							Set<IResource> pythonResources = pathNature.getProjectSourcePathFolderSet();
							for (IResource resource : pythonResources) {
								info = getModuleInfo(libraryName, resource.getLocation().toOSString(), libraryName);
								if (info != null) {
									moduleMap.put(libraryName, info);
									foundModule = true;
									break;
								}
							}
						}
					}
					
					if (!foundModule) {
						// try searching for a library under the python installation dir
						info = findInstalledPythonLibModule(libraryName, sourceFile);
						if (info != null) {
							moduleMap.put(libraryName, info);
							foundModule = true;
						}
					}
					
					if (info != null) {
						moduleMap.putAll(libraryName, collectBaseClassModules(info));
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}

		return moduleMap;
	}

	private static List<ModuleInfo> findStandardLibModule(String libraryName, IFile sourceFile) {
		IProject project = sourceFile.getProject();

		PythonNature nature = PythonNature.getPythonNature(project);
		if (nature != null) {
			try {
				List<String> interpreterPaths = nature.getProjectInterpreter().getPythonPath();
				String standardLibPath = null;
				for (String path : interpreterPaths) {
					// some hacky stuff here to distinguish how macos and windows include
					// robot in their interpreter paths - this may have also changed a bit
					// across pydev versions, so try several ways
					if (path.contains("robot")) {
						if (path.endsWith(".egg")) {
							standardLibPath = path + "/robot/libraries";
						} else {
							standardLibPath = path + "/libraries";
						}
						break;
					} else if (path.contains("site-packages")) {
						File checkFile = new File(path + "/robot/libraries");
						if (checkFile.exists()) {
							standardLibPath = path + "/robot/libraries";
						}
					}
				}

				List<ModuleInfo> infos = new ArrayList<ModuleInfo>();
				ModuleInfo info = getModuleInfo(libraryName, standardLibPath, libraryName);
				if (info != null) {
					infos.add(info);
					if (info != null) {
						infos.addAll(collectBaseClassModules(info));
					}
					return infos;
				}
			} catch (MisconfigurationException e) {
				// e.printStackTrace();
			} catch (PythonNatureWithoutProjectException e) {
				// e.printStackTrace();
			}
		}
		return null;
	}

	private static ModuleInfo findInstalledPythonLibModule(String libraryName, IFile sourceFile) {
		IProject project = sourceFile.getProject();

		PythonNature nature = PythonNature.getPythonNature(project);
		if (nature != null) {
			try {
				List<String> interpreterPaths = nature.getProjectInterpreter().getPythonPath();
				String libPath = null;
				for (String path : interpreterPaths) {
					if (path.contains("site-packages")) {
						File checkFile = new File(path + "/" + libraryName);
						if (checkFile.exists()) {
							libPath = checkFile.getCanonicalPath();
						}
					}
				}

				ModuleInfo info = getModuleInfo(libraryName, libPath, libraryName);
				return info;
			} catch (IOException e) {
				// e.printStackTrace();
			} catch (MisconfigurationException e) {
				// e.printStackTrace();
			} catch (PythonNatureWithoutProjectException e) {
				// e.printStackTrace();
			}
		}
		return null;
	}

	private static ModuleInfo getModuleInfo(String libraryName, String rootPath, String libraryPath) {
		String pathString = PathUtil.replaceBuiltInSlashVars(libraryPath.replace("\\", "/"));
		String modulePathString = "";

		List<String> pathSegments = PathUtil.getPathSegments(pathString);

		while (!pathSegments.isEmpty()) {
			String segment = pathSegments.remove(0);
			if ("".equals(segment)) {
				continue;
			}

			modulePathString = modulePathString + "/" + segment;
			String candidatePath = modulePathString;
			if (!candidatePath.endsWith(".py")) {
				candidatePath = candidatePath + ".py";
			}

			File file = new File(rootPath + "/" + candidatePath);
			if (file.exists()) {
				IModule module = findModule(file);
				if (module != null) {
					return createModuleInfo(module, libraryName, pathSegments);
				}
			} else if (pathSegments.isEmpty()) {
				// look for a package initializer at this location instead
				String initializerPath = modulePathString + "/__init__.py";
				file = new File(rootPath + "/" + initializerPath);
				if (file.exists()) {
					IModule module = findModule(file);
					if (module != null) {
						for (IToken token : module.getGlobalTokens()) {
							if (segment.equalsIgnoreCase(token.getRepresentation())) {
								return createModuleInfo(module, libraryName, Arrays.asList(new String[] { segment }));
							}
						}
					}
				} else {
					// we didn't find a module after completely checking the full path.
					// do one last check for a class of the correct name defined 
					// inside a package initializer
					initializerPath = 
							modulePathString.substring(0, modulePathString.length() - segment.length())
							+ "__init__.py";
					file = new File(rootPath + "/" + initializerPath);
					if (file.exists()) {
						IModule module = findModule(file);
						if (module != null) {
							for (IToken token : module.getGlobalTokens()) {
								if (segment.equalsIgnoreCase(token.getRepresentation())) {
									// we've found an __init__.py package
									// initializer that contains a class matching
									// the library name
									return createModuleInfo(module, libraryName, Arrays.asList(new String[] { segment }));
								}
							}
						}
					}
					
				}
			}
		}

		return null;
	}

	private static IModule findModule(File file) {
		Tuple<IPythonNature, String> infoForFile = PydevPlugin.getInfoForFile(file);
		if (infoForFile != null) {
			IPythonNature pythonNature = infoForFile.o1;
			String moduleName = infoForFile.o2;
			try {
				return AbstractModule.createModule(moduleName, file, pythonNature, true);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (MisconfigurationException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static ModuleInfo createModuleInfo(IModule module, String libraryName, List<String> pathSegments) {
		StringWriter writer = new StringWriter();
		boolean skipSlash = true;
		for (String seg : pathSegments) {
			if (!skipSlash) {
				writer.append("/");
			} else {
				skipSlash = false;
			}
			writer.append(seg);
		}
		String remainingPath = writer.toString();
		return new ModuleInfo(libraryName, module, remainingPath);
	}
	
	/**
	 * If a module has baseclass types, resolve and return all of them.  They will be added as additional
	 * entries for the parent class in the module multimaps. 
	 */
	private static List<ModuleInfo> collectBaseClassModules(ModuleInfo moduleInfo) {
		List<ModuleInfo> baseModules = new ArrayList<ModuleInfo>();
		List<ModuleInfo> imports = new ArrayList<ModuleInfo>();
		List<String> baseClasses = new ArrayList<String>();
		List<String> localClasses = new ArrayList<String>();
		
		// first find any base types used in the module's class definition
		IToken[] tokens = moduleInfo.getModule().getGlobalTokens();
		for (IToken token : tokens) {
			if (token instanceof SourceToken) {
				if (token.getType() == IToken.TYPE_CLASS) {
					localClasses.add(token.getRepresentation());
					
					if (moduleInfo.getLibraryName().equals(token.getRepresentation())) {
						SourceToken sourceToken = (SourceToken) token;
						SimpleNode node = sourceToken.getAst();
						if (node instanceof ClassDef) {
							ClassDef classDef = (ClassDef) node;
							exprType[] bases = classDef.bases;
							for (exprType type : bases) {
								if (type instanceof Name) {
									baseClasses.add(((Name) type).id);
								}
							}
						}
					}
				}
			}
		}
		
		// if no base types, nothing to do
		if (!baseClasses.isEmpty()) {
			// collect all the tokens for any imports - we'll need to match the base types 
			// against these imports to figure out where to load modules from if they're external
			IToken[] wildImports = moduleInfo.getModule().getWildImportedModules();
			IToken[] tokenImports = moduleInfo.getModule().getTokenImportedModules();
			List<IToken> importTokens = new ArrayList<IToken>();
			importTokens.addAll(Arrays.asList(tokenImports));
			importTokens.addAll(Arrays.asList(wildImports));
			
			// load up every module we can find from these imports
			for (IToken token : importTokens) {
				if (token instanceof SourceToken && token.getType() == IToken.TYPE_IMPORT) {
					SourceToken sourceToken = (SourceToken) token;
					SimpleNode node = sourceToken.getAst();
					if (node instanceof Import) {
						Import importDef = (Import) node;
						aliasType[] aliases = importDef.names;
						for (aliasType alias : aliases) {
							ModuleInfo info = getModuleInfo(((NameTok) alias.name).id, moduleInfo.getModule().getFile().getParent() + File.separator + ((NameTok) alias.name).id, ((NameTok) alias.name).id);
							if (info != null) {
								imports.add(info);
							}
						}
					} else if (node instanceof ImportFrom) {
						ImportFrom importDef = (ImportFrom) node;
						NameTok moduleName = (NameTok) importDef.module;
						aliasType[] aliases = importDef.names;
						if (aliases.length == 0) {
							// import everything from path
							File folder = new File(moduleInfo.getModule().getFile().getParent() + File.separator + moduleName.id);
							if (folder.exists() && folder.isDirectory()) {
								File[] files = folder.listFiles();
								for (File file : files) {
									ModuleInfo info = getModuleInfo(file.getName(), folder.getPath(), file.getName());
									if (info != null) {
										imports.add(info);
									}
								}
							}
							
						} else {
							// form of:  from X import Y,Z
							for (aliasType alias : aliases) {
								ModuleInfo info = getModuleInfo(((NameTok) alias.name).id, moduleInfo.getModule().getFile().getParent() + File.separator + moduleName.id, ((NameTok) alias.name).id);
								if (info != null) {
									imports.add(info);
								}
							}
						}
					}
				}
			}
			
			// now that the modules have been located and loaded, see which ones define the base classes we're looking for
			for (String baseClass : baseClasses) {
				if (localClasses.contains(baseClass)) {
					// locally-defined class in this source module
					baseModules.add(new ModuleInfo(moduleInfo.getLibraryName(), moduleInfo.getModule(), baseClass));
				} else {
					// external imported class
					for (ModuleInfo importInfo : imports) {
						for (IToken token : importInfo.getModule().getGlobalTokens()) {
							if (baseClass.equalsIgnoreCase(token.getRepresentation())) {
								baseModules.add(new ModuleInfo(importInfo.getLibraryName(),  importInfo.getModule(), baseClass));
							}
						}
					}
				}
			}
		}

		return baseModules;
	}
}
