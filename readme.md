# kts-tests

Testing KTS stuff

## Host

This is the host application, which actually runs the script.
Run [HostKt](kts-host/src/main/kotlin/be/bluexin/ktstests/host/Host.kt) with argument to a script file to run.
A [sample script](kts-scripts/src/main/kotlin/firstTest.bluescript.kts) shows usage.

## Script Definition

This defines what can actually be used by the script, by defining a [script superclass](kts-scriptdef/src/main/kotlin/be/bluexin/ktstests/scriptdef/BlueScript.kt).
Note that this provides smart IDE features for editing scripts via the [flag in META-INF](kts-scriptdef/src/main/resources/META-INF/kotlin/script/templates/be.bluexin.ktstests.scriptdef.BlueScript.classname).
To see this in action, compile the project and open the [sample script](kts-scripts/src/main/kotlin/firstTest.bluescript.kts).
