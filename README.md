# HSSD
An editor for Highly Structured Static Data.

### Status

 * Almost all editor features should work on Windows and Mac.
 * The default ID allocator does not allocate ID. Take a look at [exec_id_alloc.demo](https://github.com/insweat/hssd/blob/master/sample_static_data/exec_id_alloc.demo) and implement your own. For example, you can have your `exec_id_alloc` to communicate with a SQL database.
 * The default code generator does not generate code. Take a look at [exec_code_gen.demo](https://github.com/insweat/hssd/blob/master/sample_static_data/exec_code_gen.demo) if you need it.
 * The default data exporter exports data in JSON (`entries.json`). That is not of much use in production. It is to show you how you can iterate over the entries and their value nodes so that you can implement your own exporter. An implementation can also be based on some transformation of the exported `entries.json` (say with [msgpack](http://msgpack.org)), but it would be a good optimization if values of each entry in `entries.json` were presented in a tree structure rather than in a flat one.
 * The publish process is not well worked on. Currently, making a build requires a lot of interaction. See [Making Builds Manually](#MakingBuildsManually)
 * We need icon and spash screen of our own.

### Prerequisites for Use

You need [Java JRE or JDK 8](http://www.oracle.com/technetwork/cn/java/javase/downloads/index.html). Earlier versions of Java do NOT work.

### Try

Grab a [release](https://github.com/insweat/hssd/releases).

### Prerequisites for Development

 1. You need [Java JDK 8](http://www.oracle.com/technetwork/cn/java/javase/downloads/index.html). Earlier versions of JDK do NOT work.
 2. [Download](www.eclipse.org/downloads) the `Eclipse for RCP and RAP Developers` bundle, and [install](http://scala-ide.org/download/current.html) the corresponding `Scala IDE` plugin (via the update site). As of the writing of this tutorial, `Eclipse Mars.1 (4.5.1)` and `Scala IDE 4.2.0` are used. If it asks you to configure your Scala Plugin, do as it instructs you to, and the defaults should work.
 
Note, if you are on Mac and get issues starting either Eclipse or `HSSD`, take a look at [Eclipse Kepler for OS X Mavericks request Java SE 6](http://stackoverflow.com/questions/19563766/eclipse-kepler-for-os-x-mavericks-request-java-se-6). The post might be about different Java and Eclipse versions, but the instructions should still work.

### Making Changes<a name="MakingChanges"></a>
 
 1. Git clone this project.
 2. In Eclipse, import the projects `com.insweat.hssd.editor`, `com.insweat.hssd.export`, `com.insweat.hssd.lib`, `org.apache.poi-3.12` and `org.eclipse.platform` into your workspace. As of this writing, the workspace is created at the `hssd` folder that you cloned, i.e., those projects should be right in the workspace. But it should also work if you create the workspace elsewhere.
 3. The `com.insweat.hssd.tests` project may contain some compiler errors. You can close it for now. Occasionally, there may be some error indicators in editors or errors in the `Problems` view. See [Known Issues](#KnownIssues).
 4. Open `com.insweat.hssd.editor/plugin.xml`, and go to the `Overview` page. Click `Launch an Eclipse application` under the `Testing` section on the middle-right. You should see another eclipse instance launched. That instance is our `HSSD` editor. If you see a dialog at the begining telling you `JDT Weaving is disabled` and it recommends you to perform a restart, click `Cancel`.
 5. Now you should see an empty workspace in the `HSSD` editor. Import the `sample_static_data` project you cloned along with the others.
 6. Open the project and click `samples.hssd`.
 7. Contratulations! Now you should see the `EntryTree` pane in the editor area.

### Making Builds Manually<a name="MakingBuildsManually"></a>

 1. Make sure all relevant projects compiles without error.
 2. Open `com.insweat.hssd.editor/hssd.product`, and select the `Overview` page. Click the `Eclipse Product export wizard` link under the `Exporting` section on the right.
 3. On the popping wizard,
    * `Configuration` - /com.insweat.hssd.editor/hssd.product
    * `Root directory` - hssd
    * `Synchronize before exporting` - CHECKED
    * `Destination - Directory` - Click `Browse` and choose yours
    * `Export source` - UNCHECKED
    * `Generate p2 repository` - UNCHECKED
    * `Export for multiple platforms` - CHECKED (You usually do not see this or need this, see http://wiki.eclipse.org/Building#Preferred_way_of_doing_multi-platform_builds)
    * `Allow for binary cycles in target platform` - CHECKED
 4. Click `Finish` to export product
 5. Open `com.insweat.hssd.editor/plugins.xml`, and select the `Overview` page. Click the `Export Wizard` link under the `Exporting` section on the right.
 6. On the popping wizard,
    * Select `com.insweat.hssd.editor`, `com.insweat.hssd.export` and `com.insweat.hssd.lib`
    * Specify `Destination - Directory`
    * Click `Options`, and make sure `Package plug-ins as individual JAR archives`, `Allow for binary cycles in target platform` and `Use class files compiled in the workspace` are CHECKED, and the rest are UNCHECKED.
 7. Click `Finish` to export plugins.
 8. Go to the `plugins` folder in the product folder (`hssd`) you just exported, delete the folder `com.insweat.hssd.lib-<version>`, then copy over the plugins you just exported as jars, to replace the existing ones.
 9. If you ever started the product before, check the `configuration` folder, and make sure only one file exists - `config.ini`.
 10. For `Eclipse Mars.1`, the product export does not seem to work correctly. If you see files `Info.plist`, `MacOS`, `Resources` under the product folder, you MUST move them into `hssd.app/Contents`, and **MERGE** them with their counterparts.
 
### Known Issues<a name="KnownIssues"></a>
 1. API baseline does not exist. Right click on the issue in the `Problems` view, and choose `Quick Fix`. Follow instructions to create an `API Baseline` targeting the current running environment.
 2. Occasionally, errors show up in the `Problems` view, even if you revert all your local changes. That is usually caused by the `com.insweat.hssd.lib` project, which is in Scala. There seem to be some incompatibilities somewhere. You can try some or all of the following:
    * Close all projects and delete all generated / cache files (`bin`, `.cache-main`, etc) in each project and then reopen projects
    * Refresh all projects
    * `Clean` (and rebuild) `com.insweat.hssd.lib`
    * `Clean` (and rebuild) all projects
    * Uncheck `Build Automatically` under the `Project` menu, then `Clean`, and then re-check it to issue builds.
 3. Error indicators show up in editors, but corresponding errors cannot be found in the `Problems` view. That is an issue of Eclipse itself. Those indicators are annoying, but you can safely ignore them.
