[//]: # (### For Pixel Stock Android 12 and 13 &#40;Up to Nov 2022 - AOSP 13R8&#41;:  )

[//]: # ([![Latest Release for A12 & A13 up to Nov 2022]&#40;https://img.shields.io/badge/Download-v2.4.1-blue&#41;]&#40;https://github.com/siavash79/PixelXpert/releases/tag/v2.4.1&#41;  )
[//]: # ()
### For Pixel Stock Android 13 and 14 (starting with Dec 2022 security patch):  

[//]: # ([![Latest Release]&#40;https://img.shields.io/github/v/release/siavash79/PixelXpert?color=green&include_prereleases&label=Download%20Latest%20Stable&#41;]&#40;https://github.com/siavash79/PixelXpert/releases/latest&#41;)
[![Latest Canary Release](https://img.shields.io/badge/Download%20Latest-Canary-blue)](https://github.com/dttzyjw0012/AospXpert/releases/tag/canary_425)

[//]: # (![Downloads - Stable channel]&#40;https://img.shields.io/github/downloads/siavash79/PixelXpert/total?color=red&label=Downloads%20-%20Stable%20Channel&#41;)

[//]: # (### **PixelXpert Support Channels:**)

[//]: # ()
[//]: # ([![XDA URL]&#40;https://img.shields.io/twitter/url?label=XDA%20Developers&logo=XDA-Developers&style=social&url=http://XDA.PixelXpert.siava.sh&#41;]&#40;http://XDA.PixelXpert.siava.sh&#41;)

[//]: # ([![Telegram URL]&#40;https://img.shields.io/badge/Telegram-Join-2CA5E?style=social&logo=telegram&#41;]&#40;https://t.me/PixelXpert_Discussion&#41;)

![Header Image](https://github.com/siavash79/PixelXpert/blob/canary/.github/PixelXpert_Banner_1280.png?raw=true)

This is a mixed Xposed+Magisk module, which is made to allow customizations that are not originally designed in AOSP (Android Open Source Project). Please read thorough below before reaching to download links
<hr>

### **Features:**
Currently, AOSPXpert offers customizations on different aspects of system framework and SystemUI, including:
- Status bar
- Quick Settings panel
- Lock screen
- Notifications
- Gesture Navigations
- Phone & Dialer
- Hotspot
- Package Manager
- Screen properties
<hr>

### **Compatibility:**
AOSPXpert is a fork of the PixelXpert with a lot of different things from the original module. It is designed for be compatible with pure AOSP roms(include which port from the AOSP source code or just is an AOSP gsi). Some custom ROM (including YAAP and the Pixel Project and etc) which similar to the pure AOSP maybe can work normally with AOSPXpert, so they will get some limited support by me. Other custom roms(including PE, PE plus, Pixel Plus UI, Lineage OS, CLO based roms just like AOSPA and etc) or stock ROMs (e.g. OneUI on Samsung, MIUI on Xiaomi and etc, even the stock pixel firmware on Google pixel devices, Sony UI and Hello UI) are not supported and may not be fully (or even at all) compatible.

Here is the compatibility chart according to different android versions and QPRs:

[//]: # (- Android 12/12.1: [final version: v2.4.1]&#40;https://github.com/siavash79/PixelXpert/releases/tag/v2.4.1&#41;.)

[//]: # (- Android 13 stable QPR1 &#40;up until November 2022 firmware&#41;: [final version: v.2.4.1]&#40;https://github.com/siavash79/PixelXpert/releases/tag/v2.4.1&#41;.)
- Android 13 stable QPR3 (starting from December 2022 firmware till QPR3): [starting with v.2.5](https://github.com/siavash79/PixelXpert/releases/tag/v2.5.0) up until the latest stable/canary versions.
- Android 14: [starting with v.2.9](https://github.com/siavash79/PixelXpert/releases/tag/v2.9.0) up until the latest stable/canary versions.
<hr>
- Android 15 beta builds: only latest canary version (anytime)
<hr>

### **Prerequisites:**
- Compatible ROM (see Compatibility text above)
- Device Rooted with Magisk 24.2+ or KSU
- LSPosed (Zygisk Version preferred) (For Android 14+ use [LSPosed_mod](https://github.com/mywalkb/LSPosed_mod/releases))
<hr>

### **How to install:**
- Download the stable magisk module according to your firmware as mentioned above 
- Install in magisk/KSU
- Reboot (no bootloops are expected)
- Open PixelXpert app and apply changes

P.S. For KSU, there is an extra step of granting root access to PixelXpert as it doesn't request automatically as in Magisk
<hr>

### **Release Variants:**  
The module is also released in 2 flavors with different manual download and update procedures. But both can utilize automated updates through magisk manager, or through in-app updater (for canary, updates will not count against the module's download count).

<ins>Stable release:</ins> 
- Manual Install/Update: through repository's Github release page (link below) AND through in-app updater

<ins>Canary release:</ins>
- Manual Install/Update: through repository's Actions page and [telegram channel]() (latest version is available from [here](https://github.com/siavash79/PixelXpert/releases/) also)

*No matter which flavor you're on, you can always switch to the other one with in-app updater
<hr>

### **Translations:**  
[![Crowdin](https://badges.crowdin.net/aospmods/localized.svg)](https://crowdin.com/project/aospmods)  
Want to help translate PixelXpert to your language? Visit [Crowdin](https://crowdin.com/project/aospmods)
<hr>

### **Donations:**
This project is open source and free for usage, build or copy. However, if you really feel like it, you can donate to your favorite charity on our behalf, or help funding education for children in need, at [Child Foundation](https://mycf.childfoundation.org/s/donate)
<hr>

### **Credits / Thanks:**
- Android Team
- @topjohnwu for Magisk
- @rovo89 for Xposed
- Team LSPosed
- @siavash79 & @ElTifo for PixelXpert
- @C3C0 for GravityBox


**UI design:**  
- @Mahmud0808  

**Graphic design:**  
- JstormZx@Telegram (Icon and Banner) 
- RKBDI@Telegram  (Icon)

**Brought to you by:**
- nameless(@dttzyjw0012)
<hr>
