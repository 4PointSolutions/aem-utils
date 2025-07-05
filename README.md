# 4Point AEM Utilities

A collection of 4Point AEM and AEM Forms Utilities.  This currently consists of the following projects:

## AEM Control (aem_cntrl)

This is a command line utility that can be used to install and (eventually) control AEM.  It knows about AEM and when installing, it will set 
up the correct Java environment before installing.  It installs to a AEM standard location based on the version and service pack being installed.

It is still under development, but in the future it will be used to control AEM in a way that is useful for scripted actions (e.g. start AEM and wait until it is available for accepting transactions before returning).

## AEM Container (aem_container)

This project contains dockerfiles that are useful for creating a containerized instance of AEM.  The container is not ideal for all purposes. 
It is large (8-9GBs) and it is slow (it takes several minutes from start to when it can accept requests) but it is still useful for doing things like development and automated testing.
