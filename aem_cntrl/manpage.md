aem_cntrl Version 0.0.1-SNAPSHOT | Install and control AEM from the command line

NAME
====

**aem_cntrl** — Installs and controls AEM from the command line.

SYNOPSIS
========

| **aem-cntrl** **<command>** **[options]**

DESCRIPTION
===========

`aem_cntrl` is a program that can install and control an AEM instance.  

**NOTE** This application requires the [jbang](https://www.jbang.dev/) be installed and on the executable PATH in order for it to function.

Commands
-------
**install** \[\[**-d**|**--destDir**]=*destDir*] \[\[**-s**|**--srcDir**]=*srcDir*]  

:  Installs an on-prem version of AEM with Service Packs and Forms Add-on. 

#### Options
&nbsp;&nbsp;&nbsp;&nbsp;*destDir* is the destination directory for the AEM installation. 
If omitted, the default for Windows, is `\Adobe`. The default for Linux, is `/opt/adobe`. 

&nbsp;&nbsp;&nbsp;&nbsp;*srcDir* is the source directory containing the AEM installation files.  It defaults to the current directory if not specified.

&nbsp;

**waitforlog** | **wflog** \[\[**-ad** | **--aemdir**]=*aemDirSpec*] 
\[\[**-fe** | **--fromEnd**] | [**-fs** | **--fromStart**]] \[\[**-su** | **--startup**] | \[**-sd** | **--shutdown**] | \[\[**-re** | **--regex**]=*regEx*]] 
\[\[**-t** | **--timeout**]=*timeout*]

:  Waits for a specific message to appear in the AEM error.log file.  It can start looking for the line from the start of the log (in which case, it will terminate immediately if the line already appears in the log when the command is invoked), or from the end of the log (in which case, it will only terminate after when the line appears *after* the command is invoked).  It can wait for standard startup and shutdown messages or a custom regular expression provided on the command line.  A custom timeout value can also be specified.

#### Options

:  If the *aemDirSpec* is omitted, then it will default to the looking for a single AEM directory under the standard location.  The standard location for Windows is `\Adobe`. The standard location for Linux, is `/opt/adobe`.

: If neither **--fromStart** nor **--fromEnd** options are provided, it defaults to **--fromEnd**.

: One of **--startup**, **--shutdown**, or **--regex** options must be provided.

: If no timeout is provided, it defaults to PT10M, i.e. a 10 minute timeout

&nbsp;&nbsp;&nbsp;&nbsp;*aemDirSpec* is an Aem Directory Specification.  See *aemDirSpec* under [Specifying the AEM Directory](specifying_the_aem_directory).

&nbsp;&nbsp;&nbsp;&nbsp;*regEx* is a [Java regular expression](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html).

&nbsp;&nbsp;&nbsp;&nbsp;*timeout* is a [Java Duration expression](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence))

&nbsp;

Specifying the AEM Directory
-------

When an existing AEM directory must be provided, it is provided as an *aemDirSpec*.  This follows the following rules:
* If an absolute directory path is provided then it is used as the source directory.
* If an explicit relative directory path is provided (i.e. one that starts with `.` or `..`) then it is used as the source directory.
* If an implied relative directory (i.e. does not start with a drive letter, file separator, `.`, or `..`) then it is assumed to be relative to 
the standard AEM location (i.e `\Adobe` for Windows, `/opt/adobe` for Linux).

The source directory can be either an explicit AEM directory (i.e. the directory that contains the AEM quickstart jar and a crx-quickstart subdirectory) 
or a parent directory with a single AEM subdirectory.  If a parent directory is specified and the application is unable to locate a single AEM 
subdirectory (i.e. it is unable to find an AEM subdirectory or it locates multiple AEM subdirectories) it will fail with an appropriate error message.   


EXAMPLES
====

**NOTE** These examples use [jbang](https://www.jbang.dev/) to ensure that the correct version of Java is run.  Since jbang must be installed and on the executable PATH in order for `aem_cntrl` to function, we can use it to run the .jar.


`jbang run --java=21 aem_cntrl-0.0.1-SNAPSHOT.jar install`

Installs AEM using the installation files in the current directory and creating an standard directory structure under `C:\Adobe` or `/opt/adobe` (depending on the platform).

`jbang run --java=21 aem_cntrl-0.0.1-SNAPSHOT.jar install -s /opt/AemInstallationFiles -d /opt/aem`

Installs AEM using the installation files under `/opt/AemInstallationFiles` into a directory created under `/opt/aem`.

`jbang run --java=21 aem_cntrl-0.0.1-SNAPSHOT.jar wflog --startup`

Starts monitoring the AEM error.log file in `crx-quickstart/logs` within the Aem directory under `C:\Adobe` or `/opt/adobe` (depending on the platform).  Returns when the AEM startup is complete.


BUGS
====

See GitHub Issues: <https://github.com/4PointSolutions/aem-utils/issues>

AUTHOR
======

4Point Solutions Ltd.

SEE ALSO
========

