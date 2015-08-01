# Introduction
Pyro is an abbreviation for "**PY**thon-aware **RO**bot" editor.  This is a set of Eclipse plugins for developing automated testing code using the [Robot Framework](http://robotframework.org/).  Python awareness comes from a tight integration into the popular [PyDev](http://pydev.org/) development tools for Eclipse.  Many typical Eclipse capabilities are present in this Robot editor, including: syntax coloring, content assist, hyperlinking to local and external resources, Outline view integration, etc.

# Update site
The Eclipse update site to install the latest editor build (Help->Install New Software... in Eclipse) is found here:
https://github.com/millennialmedia/pyro/raw/updatesite

# Project setup
For a new or existing Eclipse project containing Robot Framework test sources:

* First make sure PyDev is active for the project.  Right-click the project and from the context menu select PyDev->Set as PyDev Project.  (Setting the project type may also happen automatically).

* To find Python libraries that implement Robot keywords, the project may need additional PYTHONPATHs to be set
  * Right-click the project and pick Properties
  * Pick the tab PyDev - PYTHONPATH
  * Click Add Source Folder
  * Add additional source folders within the project or workspace that would be present on the PYTHONPATH during runtime.  Ex. if pybot is launched with a -P argument, that would need to be selected here.

* To fully utilize Pyro features for Robot's built-in libraries, ensure that Robot itself is installed into your local Python runtime.  For more complicated installations, see PyDev documentation for properly configuring which runtime is in use within the tools.
    
   
# Features
Pyro derives from all the standard text editing frameworks in Eclipse so expected behaviors should generally "just work".  Some interesting features of particular note:

### Hyperlinking
###### Keywords
Tracing through keyword invocations across complex libraries was the initial motivation for building this project.  Pyro evaluates possible target keywords using the same order as the Robot runtime:

* Local file
* External Robot files
* User-defined Python libraries
* Robot standard libraries

###### Libraries
Opens a PyDev source editor on user-defined or built-in library files

###### Resource files
Opens an editor on external referenced Robot source files





### Content assist
Press Ctrl/Cmd-Space to display content assist proposals from the current cursor location.

###### Keyword names
Based on partial strings to the caret location, or all available keywords reachable from the current Robot file

###### Setting names
Within a Settings table or inside testcase and keyword definitions

###### Variables
At any given location, variable scopes are evaluated to present possible options based on:

* Keyword arguments
* Locally-defined variables from a previous execution step
* Variables potentially in-scope from possible callers (i.e. other testcases or keywords)
* Built-in standard Robot variables





### Outline view
The Outline view presents a tree view that is synced with the editor selection to help navigate larger Robot source files.  There are two toggle buttons on the view toolbar:
* Tree mode toggle to switch between a flattened view of keywords, settings, testcases, and variables or a Table-centric hierarchy
* A->Z button to toggle alphabetical vs. natural source ordering of elements



# Building from source
The build process uses Eclipse Tycho to enable a simple Maven build (Maven 3.0 minimum).  Create a local update site zip for Pyro from the current plugin codebase by running a Maven build on the root "pyro" project.

* From the pyro folder, run "mvn install"
* After the build completes, the local Eclipse update site can be found at: **com.millennialmedia.pyro.site/target/com.millennialmedia.pyro.site-[version].zip**.  
* Install Pyro into Eclipse via the usual Help->Install New Software... action.
* For full Python support, also make sure to separately install PyDev from the [PyDev update site](http://pydev.org/updates/).
* Restart Eclipse

* Now open a Robot source file using Pyro's editor, simply called _Robot Framework Editor_ within Eclipse.  It is contributed as a default editor for the ".robot", ".txt", and ".tsv" file extensions.  In cases of collisions with other editors, you can always choose from the Open With-> menu option. 


    
# Project structure
Three submodule Eclipse plugins define the runtime editor, each ending in the following suffixes:

    .pyro           - file parser and common model utilities
    .pyro.ui        - core editor code, definitions for extension points, and implementation of basic Robot behaviors
    .pyro.ui.pydev  - the (optional) PyDev integration code - if PyDev is also installed then python linking/searching will work

Additional subprojects are:

    .test       - some basic parser unit tests
    .feature    - definition of Eclipse feature
    .site       - definition of Eclipse updatesite
    

    