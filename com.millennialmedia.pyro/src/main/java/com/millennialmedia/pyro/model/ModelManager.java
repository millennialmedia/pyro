package com.millennialmedia.pyro.model;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.millennialmedia.pyro.internal.parser.Parser;

/**
 * The public entry point to the Robot file parser. The singleton manager object
 * is responsible for parsing and caching models, removing modified files'
 * models from the cache, and reacting to explicit reparse requests.
 * 
 * @author spaxton
 */
public class ModelManager {
	private static ModelManager instance;

	private ModelManager() {
		// startup a resource listener to invalidate cached models on resource
		// changes upon the filesystem
		ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					event.getDelta().accept(new IResourceDeltaVisitor() {
						@Override
						public boolean visit(IResourceDelta delta) throws CoreException {
							if (fileToModelMap.containsKey(delta.getResource())) {
								fileToModelMap.remove(delta.getResource());
								return false;
							}
							return true;
						}
					});
				} catch (CoreException e) {
					// ignore
				}
			}
		});
	}

	public static final synchronized ModelManager getManager() {
		if (instance == null) {
			instance = new ModelManager();
		}
		return instance;
	}

	private Map<IFile, RobotModel> fileToModelMap = new WeakHashMap<IFile, RobotModel>();
	private Map<IFile, RobotModel> fileToEditedModelMap = new WeakHashMap<IFile, RobotModel>();

	/**
	 * Parses a static file into a model. This is used in scenarios where
	 * dependencies of an edited file need to be loaded (ex. hyperlinking into
	 * an included library).
	 */
	public RobotModel getModel(IFile file) {
		if (fileToModelMap.containsKey(file)) {
			return fileToModelMap.get(file);
		} else {
			Parser parser = new Parser(file);
			RobotModel model = parser.parse();
			fileToModelMap.put(file, model);
			return model;
		}
	}

	/**
	 * Parses the given file contents into a Robot model. The provided file is
	 * used to determine the parser mode based upon file extension and the model
	 * is cached using this file as a key. The client will continue to manually
	 * reparse() any new contents as-needed.
	 */
	public RobotModel getModel(IFile file, String bufferContents) {
		Parser parser = new Parser(file);
		RobotModel model = parser.parse(bufferContents);
		fileToEditedModelMap.put(file, model);
		return model;
	}

	/**
	 * Replaces the contents of the existing model with the parser output for
	 * the new given input string.
	 */
	public void reparse(RobotModel model, String contents) {
		for (Map.Entry<IFile, RobotModel> entry : fileToEditedModelMap.entrySet()) {
			if (entry.getValue() == model) {
				IFile file = entry.getKey();
				Parser parser = new Parser(file);

				// parse a new model for the given contents
				RobotModel newModel = parser.parse(contents);

				// the client will continue to reuse the same model object
				// transparently,
				// so just swap in the newly-parsed information into that model
				model.setTables(newModel.getTables());
				model.setFirstLine(newModel.getFirstLine());
				model.getCustomProperties().clear();
				model.getCustomProperties().putAll(newModel.getCustomProperties());
			}
		}
	}

}
