cp app\build\outputs\apk\release\AOSPXpert.apk MagiskModBase\system\priv-app\AOSPXpert

cd MagiskModBase

rm -Rf ../AOSPXpert.zip

zip -r -9 -q ..\AOSPXpert.zip *.*

rm -Rf system\priv-app\AOSPXpert\AOSPXpert.apk