#!/usr/bin/env node
// Hook: copies ZSDK_ANDROID_API.jar to platforms/android/app/libs
// and ensures build.gradle includes it when Android platform is added.
// Avoids ENOTDIR errors from Cordova treating JAR as a Gradle module.

const fs = require('fs');
const path = require('path');

module.exports = function (context) {
  const rootDir = context.opts && context.opts.projectRoot ? context.opts.projectRoot : process.cwd();
  const pluginDir = path.join(__dirname, '..');
  const srcJar = path.join(pluginDir, 'src', 'android', 'libs', 'ZSDK_ANDROID_API.jar');
  const platformsDir = path.join(rootDir, 'platforms', 'android');
  
  // Don't run if platforms/android doesn't exist yet (we'll run on before_prepare instead)
  if (!fs.existsSync(platformsDir)) {
    console.log('[zebra-plugin] platforms/android does not exist yet, skipping jar copy');
    return;
  }

  const appLibs = path.join(platformsDir, 'app', 'libs');
  const destJar = path.join(appLibs, 'ZSDK_ANDROID_API.jar');
  const buildGradle = path.join(platformsDir, 'app', 'build.gradle');

  try {
    if (!fs.existsSync(srcJar)) {
      console.log('[zebra-plugin] Source JAR not found:', srcJar);
      return;
    }

    // Ensure libs directory exists
    if (!fs.existsSync(appLibs)) {
      fs.mkdirSync(appLibs, { recursive: true });
      console.log('[zebra-plugin] Created:', appLibs);
    }

    // Copy JAR
    fs.copyFileSync(srcJar, destJar);
    console.log('[zebra-plugin] Copied JAR to:', destJar);

    // Update build.gradle
    if (fs.existsSync(buildGradle)) {
      let gradle = fs.readFileSync(buildGradle, 'utf8');
      const needle = "implementation files('libs/ZSDK_ANDROID_API.jar')";
      
      if (!gradle.includes(needle)) {
        // Insert into dependencies block
        const depRegex = /dependencies\s*\{/;
        if (depRegex.test(gradle)) {
          gradle = gradle.replace(
            depRegex,
            "dependencies {\n    " + needle
          );
          fs.writeFileSync(buildGradle, gradle, 'utf8');
          console.log('[zebra-plugin] Added JAR to build.gradle dependencies');
        } else {
          console.log('[zebra-plugin] Warning: dependencies block not found in build.gradle');
        }
      } else {
        console.log('[zebra-plugin] JAR already in build.gradle');
      }
    } else {
      console.log('[zebra-plugin] build.gradle not found yet:', buildGradle);
    }
  } catch (e) {
    console.error('[zebra-plugin] Error:', e.message);
    // Don't throw - allow build to continue
  }
};
