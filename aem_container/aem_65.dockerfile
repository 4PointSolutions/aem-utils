FROM oraclelinux:9-slim AS aem-base

ENV container="aem-6.5-quickstart,aem-author,ubuntu,java11"
#Container Size: 280MB

# Install AEM Forms documented pre-requisites, documented here:
# https://experienceleague.adobe.com/docs/experience-manager-learn/forms/adaptive-forms/installing-aem-form-on-linux.html
RUN microdnf -y install \
  expat \
  fontconfig \
  freetype \
  glibc \
  libcurl \
  libICE \
  libicu \
  libSM \
  libuuid \
  libX11 \
  libXau \
  libxcb \
  libXext \
  libXinerama \
  libXrandr \
  libXrender \
  nss-softokn-freebl \
  zlib

#
# PDFG Tasks 
#
# Perform symbolic links that AEM requires
#ln -s /usr/lib/libcurl.so.4.5.0 /usr/lib/libcurl.so
#ln -s /usr/lib/libcrypto.so.1.1.1c /usr/lib/libcrypto.so
#ln -s /usr/lib/libssl.so.1.1.1c /usr/lib/libssl.so
#
# add the following property to crx-quickstart/conf/sling.properties
# sling.bootdelegation.class.com.rsa.jsafe.provider.JsafeJCE=com.rsa.*
#
# Install OpenOffice
#
# Install Java 32-bit JDK
#
# Add to profile
# export JAVA_HOME_32=<jdk1.8.0_311 location>
# export OpenOffice_PATH=<openoffice4 location>
#
# Add fonts to the /usr/share/fonts
# ln -s /usr/share/fonts /usr/share/X11/fonts


# Install handy utilities
RUN microdnf -y install \
  less \
  gzip \
  sudo \
  vi

RUN microdnf install dnf ; dnf install dnf-plugins-core ; dnf copr enable @jbangdev/jbang ; dnf install jbang

RUN adduser aem_user ; mkdir /opt/adobe ; chown aem_user /opt/adobe ; chgrp aem_user /opt/adobe ; mkdir /opt/aem_software ; chown aem_user /opt/aem_software ; chgrp aem_user /opt/aem_software

# Switch to AEM user 
USER aem_user

# Install now to get them into a layer so we don't have to install later
#   JDK 25 is installed for aem_cntrl.
#   JDK 11 is required for AEM 6.5
#  Set JBang default java version to 25 (which forces the download of JDK 25) and then install JDK 11
RUN export JBANG_DEFAULT_JAVA_VERSION=25 ; jbang jdk install 11 
#
# End of aem-base image
#


#
# Build image used to install AEM
#
FROM aem-base AS aem-install

# Switch to AEM user 
USER aem_user

COPY AemSoftware/* /opt/aem_software
# TODO:
#   Create volume at /opt/adobe/AemSoftware
#   Copy installation files there

# install JBang
# RUN curl -Ls https://sh.jbang.dev | bash -s - app setup ; . ~/.bashrc

# RUN /bin/bash -c 'export PATH="$HOME/.jbang/bin:$HOME/.jbang/currentjdk/bin:$PATH" ; export "JAVA_HOME=$HOME/.jbang/currentjdk" ;  cd /opt/aem_software ; jbang run --java=21 aem-installer-0.0.1-SNAPSHOT.jar'

# RUN /bin/bash -c 'cd /opt/aem_software ; jbang run --java=25 aem-installer-0.0.2-SNAPSHOT.jar'
RUN cd /opt/aem_software ; jbang run --java=25 aem_cntrl-0.0.2-SNAPSHOT.jar install
#
# End of aem-install image


#
# Build final image
#
FROM aem-base AS aem

# Switch to AEM user 
USER aem_user

# Copy the AEM installed directory to the new image
COPY --from=aem-install /opt/adobe /opt/adobe
# Copy the aem_cntrl jar file to the /opt/adobe directory in the new image so we can use it later.
COPY --from=aem-install /opt/aem_software/aem_cntrl-0.0.2-SNAPSHOT.jar /opt/adobe

#NOTE: make sure to copy admin.password.file and license.properties files to the /opt/aem-config folder.
# VOLUME ["/opt/aem-config/"]
#VOLUME ["/opt/adobe/???/crx-quickstart/logs"]
EXPOSE 4502

#Command below is executed at runtime, instead of build
# CMD /bin/bash
# To set the admin password to something other than default, add -Dadmin.password.file=adminpassword.properties and to the java command line below
# and place admin.password = <password> in a file called adminpassword.properties within the aem directory.
# see https://experienceleague.adobe.com/docs/experience-manager-65/administering/security/security-configure-admin-password.html?lang=en
# CMD /bin/bash -c "cp -v /opt/aem-config/* /opt/aem; cd /opt/aem/ ;  java -Xms1024m -Xmx2048m --add-opens=java.desktop/com.sun.imageio.plugins.jpeg=ALL-UNNAMED --add-opens=java.base/sun.net.www.protocol.jrt=ALL-UNNAMED --add-opens=java.naming/javax.naming.spi=ALL-UNNAMED --add-opens=java.xml/com.sun.org.apache.xerces.internal.dom=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED -Dnashorn.args=--no-deprecation-warning -jar ./AEM_6.5_Quickstart.jar -v -nointeractive"
CMD /bin/bash -c "cd /opt/adobe/AEM* ; eval '$(jbang jdk java-env 11)' ;  \
java -Xms1024m -Xmx2048m \
--add-opens=java.desktop/com.sun.imageio.plugins.jpeg=ALL-UNNAMED \
--add-opens=java.base/sun.net.www.protocol.jrt=ALL-UNNAMED \
--add-opens=java.naming/javax.naming.spi=ALL-UNNAMED \
--add-opens=java.xml/com.sun.org.apache.xerces.internal.dom=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED \
--add-opens=java.base/java.net=ALL-UNNAMED \
-Dnashorn.args=--no-deprecation-warning \
-Djava.awt.headless=true \
-Dsling.run.modes=author,crx3,crx3tar \
-Djava.locale.providers=CLDR,JRE,SPI \
-jar crx-quickstart/app/cq-quickstart-6.5.0-standalone-quickstart.jar \
start \
-c crx-quickstart \
-i launchpad \
-p 4502 \
-Dsling.properties=conf/sling.properties"
