#!/usr/bin/env node
// Copies ZSDK_ANDROID_API.jar into platforms/android/app/libs and ensures build.gradle references it.
// Idempotent and safe to run multiple times.

const fs = require('fs');
const path = require('path');

module.exports = function (context) {
  const rootDir = context.opts && context.opts.projectRoot ? context.opts.projectRoot : process.cwd();
  const pluginDir = path.join(__dirname, '..');
  const srcJar = path.join(pluginDir, 'src', 'android', 'libs', 'ZSDK_ANDROID_API.jar');
  const appLibs = path.join(rootDir, 'platforms', 'android', 'app', 'libs');
  const destJar = path.join(appLibs, 'ZSDK_ANDROID_API.jar');
  const buildGradle = path.join(rootDir, 'platforms', 'android', 'app', 'build.gradle');

  try {
    if (!fs.existsSync(srcJar)) {
      console.log('[zebra-plugin] Source JAR not found, skipping copy:', srcJar);
      return;
    }

    if (!fs.existsSync(appLibs)) {
      fs.mkdirSync(appLibs, { recursive: true });
      console.log('[zebra-plugin] Created app libs directory:', appLibs);
    }

    // Copy JAR
    fs.copyFileSync(srcJar, destJar);
    console.log('[zebra-plugin] Copied JAR to', destJar);

    // Ensure build.gradle contains implementation entry
    if (fs.existsSync(buildGradle)) {
      let gradle = fs.readFileSync(buildGradle, 'utf8');
      const needle = "implementation files('libs/ZSDK_ANDROID_API.jar')";
      if (!gradle.includes(needle)) {
        // Try to insert into dependencies { ... }
        const depRegex = /dependencies\s*\{([\s\S]*?)\}/m;
        const match = gradle.match(depRegex);
        if (match) {
          const before = gradle.slice(0, match.index);
          const deps = match[0];
          const after = gradle.slice(match.index + match[0].length);
          const newDeps = deps.replace(/\}\s*$/m, `    ${needle}\n}`);
          gradle = before + newDeps + after;
          fs.writeFileSync(buildGradle, gradle, 'utf8');
          console.log('[zebra-plugin] Added JAR implementation to build.gradle');
        } else {
          console.log('[zebra-plugin] Could not find dependencies block in build.gradle to insert implementation entry.');
        }
      } else {
        console.log('[zebra-plugin] build.gradle already contains JAR implementation entry');
      }
    } else {
      console.log('[zebra-plugin] build.gradle not found, skipping modification:', buildGradle);
    }
  } catch (e) {
    console.error('[zebra-plugin] Error copying JAR or updating build.gradle:', e);
    throw e;
  }
};
