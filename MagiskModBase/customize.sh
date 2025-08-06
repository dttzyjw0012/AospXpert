PKGNAME="com.yhc.aospxpert"
PKGPATH="/system/priv-app/AOSPXpert/AOSPXpert.apk"
LSPDDBPATH="/data/adb/lspd/config/modules_config.db"
MAGISKDBPATH="/data/adb/magisk.db"

prepareSQL(){
	unzip $ZIPFILE sqlite3 -d $TMPDIR/ > /dev/null
	chmod +x $TMPDIR/sqlite3

	SQLITEPATH="$TMPDIR/sqlite3"
}

# runSQL "database path" "command" - then you can use $SQLRESULT to read the outcome
runSQL(){
	SQLRESULT=$($SQLITEPATH $DBPATH "$CMD")
}

#grant silent root access to given UID
grantRootUID(){
	DBPATH=$MAGISKDBPATH

	#new record - older magisk compatibility
	CMD="insert into policies (uid, package_name, policy, until, logging, notification) values ($1, '$2', 2, 0, 1, 0);" && runSQL
	#new record
	CMD="insert into policies (uid, policy, until, logging, notification) values ($1, 2, 0, 1, 0);" && runSQL
	#previously present record
	CMD="update policies set policy = 2, until = 0, logging = 1, notification = 0 where uid = $1;" && runSQL
}


#grant root access to given package name
grantRootPkg(){
	ui_print "- 	Granting root access to $1..."
	UID=$(pm list packages -U $1 --user 0 | grep ":$1 " | awk -F 'uid:' '{ print $2 }' | cut -d ',' -f 1)

	grantRootUID $UID $1
}

#grant root access to required apps
grantRootApps(){
	grantRootPkg $PKGNAME
}

migratePrefs(){
  am start -n "$PKGNAME/.ui.activities.SettingsActivity" -e migratePrefs true > /dev/null
}

#activate PKGNAME in Lsposed
activateModuleLSPD()
{
	DBPATH=$LSPDDBPATH

	ui_print '- Trying to activate the module in Lsposed...'

	CMD="select mid from modules where module_pkg_name like \"$PKGNAME\";" && runSQL
	OLDMID=$(echo $SQLRESULT | xargs)


	if [ $(($OLDMID+0)) -gt 0 ]; then
		CMD="select mid from modules where mid = $OLDMID and apk_path like \"$PKGPATH\" and enabled = 1;" && runSQL
		REALMID=$(echo $SQLRESULT | xargs)

		if [ $(($REALMID+0)) = 0 ]; then
			CMD="delete from scope where mid = $OLDMID;" && runSQL
			CMD="delete from modules where mid = $OLDMID;" && runSQL
		fi
	fi

#some commands may fail. It's OK if they do
	CMD="insert into modules (\"module_pkg_name\", \"apk_path\", \"enabled\") values (\"$PKGNAME\",\"$PKGPATH\", 1);" && runSQL

	CMD="select mid as ss from modules where module_pkg_name = \"$PKGNAME\";" && runSQL

	NEWMID=$(echo $SQLRESULT | xargs)

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"android\",0);" && runSQL
	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"system\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.android.systemui\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.google.android.apps.nexuslauncher\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.google.android.dialer\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.android.phone\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.android.settings\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"me.weishu.kernelsu\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.rifsxd.ksunext\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"$PKGNAME\",0);" && runSQL
}

testKernelSU()
{
	if [[ $(ksud -V 2>&1 | grep "not found" | wc -c) -eq 0 ]]; then #KSU installed
    	if [[ $(pm list packages | grep $PKGNAME | wc -c) -eq 0 ]]; then #AospXpert NOT installed yet
    		ui_print ''
    		ui_print '*******************************'
    		ui_print 'KernelSU binaries found!'
    		ui_print ''
    		ui_print '                CAUTION!:'
    		ui_print 'Before installation, you MUST disable'
    		ui_print '"Umount modules by default"'
    		ui_print 'Otherwise, your device will fall into BOOTLOOP!'
    		ui_print ''
    		ui_print 'Do you wish to continue?'
    		ui_print 'Volume Up: Continue'
    		ui_print 'Volume Down: Abort'
    		if [[ "$(getevent -l | grep -m 1 KEY_VOLUME)" == *"VOLUMEDOWN"* ]]; then
    			abort 'Installation cancelled'
    		fi;
    	fi;
    fi;
}

assertPixelRom()
{
	PixelTipsPattern="TipsPrebuilt*"
	PixelTipsParent="/product/priv-app"

  if ! find "$PixelTipsParent" -maxdepth 1 -name "$PixelTipsPattern" -print -quit | grep -q .; then
  	ui_print 'Device does not seem to be a Pixel phone containing an original ROM.'
    abort 'Installation aborted due to incompatibility'
  fi
}

assertAospRom()
{
  ui_print ''
  ui_print '*******************************'
  ui_print 'This is a magisk/ksu module for AOSP-like Android roms'
  ui_print '(except other no-AOSP-like custom roms just like：'
  ui_print 'LineageOS, AOSPA and so on;'
  ui_print 'neither most of the OEM Android OS just like：'
  ui_print 'OneUI, ColorOS, HyperOS and so on)'
  ui_print ''

  ui_print 'And it is a canary build'
  ui_print 'Please install it very carefully'
  ui_print 'This module will make your device to a bricked device probably!'
  ui_print ''

  ui_print 'So do u still want to install it?'
  ui_print 'Volume Up: Continue'
  ui_print 'Volume Down: Abort'
  if [[ "$(getevent -l | grep -m 1 KEY_VOLUME)" == *"VOLUMEDOWN"* ]]; then
    abort 'Installation cancelled'
  fi;
}

assertAospRom
testKernelSU

prepareSQL

ui_print ''
ui_print ''

grantRootApps

if [ $(ls $LSPDDBPATH) = $LSPDDBPATH ]; then
	ui_print ''
	ui_print ''

	activateModuleLSPD
	migratePrefs

	ui_print ''
	ui_print ''
	ui_print 'Installation Complete!'
	ui_print 'Please Reboot your device to activate'
else
	ui_print 'Lsposed not found!!'
	ui_print 'This module will not work without Lsposed'
	ui_print 'Please:'
	ui_print '- Install Lsposed'
	ui_print '- Reboot'
#	ui_print '- Manually enable AospXpert in Lsposed'
#	ui_print '- Reboot'
fi

	ui_print ''
	ui_print '  **********************'
	ui_print '  * Brought to you by: *'
	ui_print '  *                    *'
	ui_print '  * AOSPXpert team    *'
	ui_print '  **********************'
	ui_print ''
