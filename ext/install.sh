#!/bin/sh

# pkcs11-wrapper
mvn install:install-file -Dfile=sunpkcs11-wrapper-1.3.0.jar -DgroupId=org.xipki.iaik \
  -DartifactId=sunpkcs11-wrapper -Dversion=1.3.0 -Dpackaging=jar

# jscep (the current release 2.3.0 contains bug that sends incorrect issuer DN
mvn install:install-file -Dfile=jscep-2.4.0-SNAPSHOT.jar -DgroupId=com.google.code.jscep \
  -DartifactId=jscep -Dversion=2.4.0-SNAPSHOT -Dpackaging=jar

# bouncycastle (version 1.55 cannot parse OCSP CertStatus with unknown status)
mvn install:install-file -Dfile=bc/bcprov-jdk15on-156b03.jar -DpomFile=bc/bcprov-jdk15on.pom

mvn install:install-file -Dfile=bc/bcpkix-jdk15on-156b03.jar -DpomFile=bc/bcpkix-jdk15on.pom

