#!/bin/sh

# pkcs11-wrapper
mvn install:install-file -Dfile=sunpkcs11-wrapper-1.4.0.jar \
  -DgroupId=org.xipki.iaik -DartifactId=sunpkcs11-wrapper -Dversion=1.4.0 \
  -Dpackaging=jar

mvn install:install-file -Dfile=sunpkcs11-wrapper-1.4.0-sources.jar \
  -DgroupId=org.xipki.iaik -DartifactId=sunpkcs11-wrapper -Dversion=1.4.0 \
  -Dpackaging=jar -Dclassifier=sources
