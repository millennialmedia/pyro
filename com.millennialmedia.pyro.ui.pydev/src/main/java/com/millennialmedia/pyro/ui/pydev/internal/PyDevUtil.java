package com.millennialmedia.pyro.ui.pydev.internal;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.structure.Tuple;

import com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;

/**
 * Utility methods for PyDev integration.
 * 
 * @author spaxton
 */
public class PyDevUtil {
	private static Map<String, List<String>> standardLibsMap = new HashMap<String, List<String>>();

	static {
		standardLibsMap.put(
				"BuiltIn",
				Arrays.asList(new String[] { "BuiltIn", "_Verify", "_Converter", "_Variables", "_RunKeyword",
						"_Control", "_Misc" }));
		standardLibsMap.put("Collections", Arrays.asList(new String[] { "Collections", "_List", "_Dictionary" }));
		standardLibsMap.put("Dialogs", Arrays.asList(new String[] { "Dialogs" }));
		standardLibsMap.put("OperatingSystem", Arrays.asList(new String[] { "OperatingSystem" }));
		standardLibsMap.put("Process", Arrays.asList(new String[] { "Process" }));
		standardLibsMap.put("Screenshot", Arrays.asList(new String[] { "Screenshot" }));
		standardLibsMap.put("String", Arrays.asList(new String[] { "String" }));
		standardLibsMap.put("Telnet", Arrays.asList(new String[] { "Telnet" }));
		standardLibsMap.put("XML", Arrays.asList(new String[] { "XML" }));
	}

	public static Map<String, List<String>> getStandardLibsMap() {
		return standardLibsMap;
	}

	public static Map<String, ModuleInfo> findModules(List<String> libraryNames, RobotFrameworkEditor editor) {
		IFile sourceFile = PathUtil.getEditorFile(editor);
		IProject project = sourceFile.getProject();

		Map<String, ModuleInfo> moduleMap = new HashMap<String, ModuleInfo>();
		PythonNature nature = PythonNature.getPythonNature(project);
		if (nature != null) {
			IPythonPathNature pathNature = nature.getPythonPathNature();
			
			for (String libraryName : libraryNames) {
				try {
					if (libraryName.contains("${CURDIR}")) {
						ModuleInfo info = getModuleInfo(libraryName, 
								sourceFile.getLocation().removeLastSegments(1).toOSString(), 
								libraryName.substring(libraryName.lastIndexOf("${CURDIR}") + 9));
						if (info != null) {
							moduleMap.put(libraryName, info);
						}
					} else {
						// search across the in-project source folders from the pythonpath
						if (pathNature != null) {
							Set<IResource> pythonResources = pathNature.getProjectSourcePathFolderSet();
							for (IResource resource : pythonResources) {
								ModuleInfo info = getModuleInfo(libraryName, resource.getLocation().toOSString(), libraryName);
								if (info != null) {
									moduleMap.put(libraryName, info);
									break;
								}
							}
						}
					}

				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}

		return moduleMap;
	}

	public static ModuleInfo findStandardLibModule(String libraryName, RobotFrameworkEditor editor) {
		IFile sourceFile = PathUtil.getEditorFile(editor);
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

				ModuleInfo info = getModuleInfo(libraryName, standardLibPath, libraryName);
				return info;
			} catch (MisconfigurationException e) {
				e.printStackTrace();
			} catch (PythonNatureWithoutProjectException e) {
				e.printStackTrace();
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
				// we didn't find a module after completely checking the full
				// path. do one last check for
				// a class of the correct name defined inside a package
				// initializer
				String initializerPath = modulePathString.substring(0, modulePathString.length() - segment.length())
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
}
