# HSSD
An editor for Highly Structured Static Data.

### Status

 * Almost all editor features should work on Windows and Mac. However, you need to implement your own ID allocator to allow features such as entry creation and entry duplication to work, because those features depend on ID allocation. An ID allocator can be very easily implemented using an auto-incerement primary key in a rational DB such as MySQL. See [exec_id_alloc.demo](https://github.com/insweat/hssd/blob/master/sample_static_data/exec_id_alloc.demo) for more information.
 * The code export and the data export implementations involve details of a private project, and thus have been removed.
 * The publish process is not well worked on, and it may or may not work. Currently, you can launch `HSSD` from Eclipse (see [Getting Started](#GettingStarted)) for evaluation.

### Prerequisites

 1. For now, you need [Java JDK 8](http://www.oracle.com/technetwork/cn/java/javase/downloads/index.html). Earlier JDK versions do not work. JDK is required to evaluate / develop `HSSD` in Eclipse. For a production build of `HSSD` a proper JRE should suffice.
 2. [Download](www.eclipse.org/downloads) the `Eclipse for RCP and RAP Developers` bundle, and [install](http://scala-ide.org/download/current.html) the corresponding `Scala IDE` plugin (via the update site). As of the writing of this tutorial, `Eclipse Mars.1 (4.5.1)` and `Scala IDE 4.2.0` are used. If it asks you to configure your Scala Plugin, do as it instructs you to, and the defaults should work.
 
Note, if you are on Mac and get issues starting either Eclipse or `HSSD`, take a look at [Eclipse Kepler for OS X Mavericks request Java SE 6](http://stackoverflow.com/questions/19563766/eclipse-kepler-for-os-x-mavericks-request-java-se-6). The post might be about different Java and Eclipse versions, but the instructions should still work.

### Getting Started<a name="GettingStarted"></a>
 
 1. Git clone this project.
 2. In Eclipse, import the projects `com.insweat.hssd.editor`, `com.insweat.hssd.lib`, `com.insweat.hssd.poi-3.12` and `org.eclipse.platform` into your workspace. As of this writing, the workspace is created at the `hssd` folder that you cloned, i.e., those projects should be right in the workspace. But it should also work if you create the workspace elsewhere.
 3. The `com.insweat.hssd.tests` project may contain some compiler errors. You can close it for now. Other than that, make a few refreshes / rebuilds as necessary to clear any error. The `com.insweat.hssd.lib` is implemented in scala, and compiling that project takes some time, which sometimes causes temporary compiler errors on dependent projects.
 4. Open `com.insweat.hssd.editor/plugin.xml`, and go to the `Overview` page. Click `Launch an Eclipse application` under the `Testing` section on the middle-right. You should see another eclipse instance launched. That instance is our `HSSD` editor. If you see a dialog at the begining telling you `JDT Weaving is disabled` and it recommends you to perform a restart, click `Cancel`.
 5. Now you should see an empty workspace in the `HSSD` editor. Import the `sample_static_data` project you cloned along with the others.
 6. Open the project and click `samples.hssd`.
 7. Contratulations! Now you should see the `EntryTree` pane in the editor area.
