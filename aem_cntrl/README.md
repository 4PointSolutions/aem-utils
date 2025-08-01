# AEM Control Application

An application that allows you to control AEM (start, stop, monitor and install).  

_TODO: Include more detailed instructions on AEM Control usage._

## Pre-requisites

### JBang
`aem_cntrl` makes use of jbang in order to manage the Java runtimes 
(to ensure that Java 11 is used for AEM 6.5, regardless of the default
Java version on the target system).

JBang must be installed and on the PATH in order for `aem_cntrl` to work.

### Permissions (Linux only)
On a Linux system, the default location for installing AEM is in a directory the installer
creates under `/opt/adobe`.

`/opt/adobe` must be created ahead of time and the user running the installer must have
write access to it.

## Running `aem_cntrl`
`aem_cntrl` is a Spring stand alone "fat jar" which contains all the necessary dependencies
inside it.  It requires Java 21. 

An easy way to ensure the correct version of Java is used is to invoke the .jar using JBang
(which is already a prerequisite and should be on the PATH).  The following command can be used
to run AEM Control `jbang run --java=21 aem_cntrl-0.0.1-SNAPSHOT.jar`.

It should generate a USAGE message.

## Installing AEM and/or AEM Forms

`aem_cntrl` installs AEM into a directory called AEM_mm_SPpp where mm is the major version (typically 65) and pp is the service pack version (e.g. 21). 
This directory is located under `\Adobe` on Windows and `/opt/adobe` on Linux. 
For example, it will install AEM with SP 21 into `/opt/adobe/AEM_65_SP21` on Linux and `\Adobe\AEM_65_SP21` on Windows.

### AEM Installation Files
The AEM installation files must be obtained from Adobe via their Software Licensing Site 
(for the base AEM install .jar) and from the Adobe Software Distribution site (for the
Service Pack and Forms Add-on .jar files).

All files should be copied into a single directory before starting the install.  This typically means:
* The base AEM install .jar (`AEM_6.5_Quickstart.jar` or `cq-quickstart-6.6.0.jar`)
* The license.properties file containing your license information (`license.properties`)
* The AEM Service Pack .zip (e.g. `aem-service-pkg-6.5.21.0.zip`) (this is optional)
* The corresponding Forms Add-on .zip (e.g. `adobe-aemfd-win-pkg-6.0.1244.zip` or `adobe-aemfd-linux-pkg-6.0.1244.zip`)
* The FluentForms libraries (e.g. `fluentforms.core-0.0.3-SNAPSHOT.jar` and `rest-services.server-0.0.3-SNAPSHOT.jar`) (this is optional)
* The `aem_cntrl` .jar (`aem_cntrl-0.0.1-SNAPSHOT.jar`)

### Running the installer
After all the installation files are copied into a single directory, To run the installer execute the following command from that directory `jbang run --java=21 aem_cntrl-0.0.1-SNAPSHOT.jar install`

### Oracle Java Installation
Adobe only officially supports the Oracle Java runtime.  If installing into a production environment 
(or an environment that needs to mirror production), then the Oracle Java runtime must be installed
ahead of time (usually from a download from the Adobe Software Distribution site).  JBang must
also be configured to use the Oracle installation.

_TODO: Include instructions on how to configure JBang to use Oracle Java installation._

