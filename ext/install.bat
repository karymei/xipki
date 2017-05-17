rem  pkcs11-wrapper
call mvn install:install-file -Dfile=sunpkcs11-wrapper-1.3.1.jar ^
  -DgroupId=org.xipki.iaik -DartifactId=sunpkcs11-wrapper -Dversion=1.3.1 ^
  -Dpackaging=jar

call mvn install:install-file -Dfile=sunpkcs11-wrapper-1.3.1-sources.jar ^
  -DgroupId=org.xipki.iaik -DartifactId=sunpkcs11-wrapper -Dversion=1.3.1 ^
  -Dpackaging=jar -Dclassifier=sources
