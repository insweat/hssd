# HSSD
An editor for Highly Structured Static Data.

### Status

 * Almost all editor features should work on Windows and Mac. However, entry creation (and features depend on it such as duplication) does not, because the ID service implementation was too specific to MySQL, and thus has been removed.
 * The import / export implementation involves details to a private project, and thus has been removed.
 * The publish process is not well worked on, and it may or may not work. You can launch the app from Eclipse (see [Getting Started](#GettingStarted)).

### Getting Started<a name="GettingStarted"></a>

 1. [Download](www.eclipse.org/downloads) the `Eclipse for RCP and RAP Developers` bundle, and [install](http://scala-ide.org/download/current.html) the corresponding `Scala IDE` plugin (via the update site). As of the writing of this tutorial, `Eclipse Mars.1 (4.5.1)` and `Scala IDE 4.2.0` are used. If it asks you to configure your Scala Plugin, do as it instructs you to, and the defaults should work.
 2. Git clone this project.
 3. In eclipse, import the projects `com.insweat.hssd.editor`, `com.insweat.hssd.lib`, `com.insweat.hssd.poi-3.12` and `org.eclipse.platform` into your workspace. As of this writing, the workspace is created at the `hssd` folder that you cloned, i.e., those projects should be right in the workspace. But it should also work if you create the workspace elsewhere.
 4. The `com.insweat.hssd.tests` project may contain some compiler errors. You can close it for now. Other than that, make a few refreshes / rebuilds as necessary to clear any error. The `com.insweat.hssd.lib` is implemented in scala, and compiling that project takes some time, which sometimes causes temporary compiler errors on dependent projects.
 5. Open `com.insweat.hssd.editor/plugin.xml`, and go to the `Overview` page. Click `Launch an Eclipse application` under the `Testing` section on the middle-right. You should see another eclipse instance launched. That instance is our `HSSD` editor. If you see a dialog at the begining telling you `JDT Weaving is disabled` and it recommends you to perform a restart, click `Cancel`.
 6. Now you should see an empty workspace in the `HSSD` editor. Import the `sample_static_data` project you cloned along with the others.
 7. Open the project and click `samples.hssd`.
 8. Contratulations! Now you should see the `EntryTree` pane in the editor area.
