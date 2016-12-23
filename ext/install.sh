#!/bin/sh

# pkcs11-wrapper
mvn install:install-file -Dfile=sunpkcs11-wrapper-1.3.0.jar -DgroupId=org.xipki.iaik \
  -DartifactId=sunpkcs11-wrapper -Dversion=1.3.0 -Dpackaging=jar

