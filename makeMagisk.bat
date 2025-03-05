cp app\release\AOSPXpert.apk MagiskModBase\system\priv-app\AOSPXpert

cd MagiskModBase

zip -r -9 -q ..\AOSPXpert.zip *.*

rm -Rf system\priv-app\AOSPXpert\AOSPXpert.apk