# Third-party projects

YouTube+tr is an unofficial personal build configuration. It does not claim authorship of the upstream patching or translation implementations and is not an official Google, Yandex, ReVanced, microG, or Morphe release.

## anddea patches

The separate TV build is a modified SmartTube derivative with an Android VOT port. Its source overlay and pinned revisions are in `tv/`. It is not an official SmartTube release.

Source: https://github.com/anddea/revanced-patches/tree/d4c3ce4b67155c4ea728aa831f5485bdb3d75934

Version: 4.3.0. The original upstream source archive is included in the release as `anddea-patches-4.3.0-source.tar.gz`. Build-time options select the name, icon and included patches. Since YouTube+tr 1.0.1, one SponsorBlock layout resource is replaced with the marked derivative in `overrides/youtube/sponsorblock/shared/layout/revanced_sb_skip_sponsor_button.xml` to fix label clipping with enlarged fonts. The replacement source and its application script are included in this repository at the corresponding release tag. No upstream bytecode is changed by this local fix. Original copyright notices, upstream credits and license information remain in the source archive and application. Preserve the upstream GPLv3 additional terms in the relevant source files and the [NOTICE](NOTICE) when redistributing covered work.

The Yandex translation integration credits anddea, Jav1x and other contributors identified in the upstream files. It is based on https://github.com/ilyhalight/voice-over-translation and its credited predecessors.

The TV port specifically adapts `VotApiClient.java`, `VotProtobuf.java` and `VotAuthWebViewDialog.java` from this pinned revision. Original headers are retained, changes are marked, and the application shows credits for anddea, Jav1x, sashade8-ship-it, ilyhalight and sodapng. Full notices and license text are available from its translation credits dialog. The port changes Java language compatibility and preferences and adds an independent TV playback controller.

## SmartTube and its modules

SmartTube: https://github.com/yuliskov/SmartTube/tree/c8873b539d8463b3276d54e395f94e5cd2bd3198

MediaServiceCore: https://github.com/yuliskov/MediaServiceCore/tree/090dc9b813d4e67e192a7d17ba3dd323157b2335

SharedModules: https://github.com/yuliskov/SharedModules/tree/5b6b5d1d3ba670e3f89edef1d1394a7562251f2c

Copyright (c) 2020-present yuliskov. These repositories use the MIT license, with separate notices and licenses for included components such as ExoPlayer, AndroidX and native dependencies. Their original license files and build dependency declarations are retained in the TV source bundle. The combined YouTube+tr TV work includes GPLv3-covered VOT code and is distributed under GPLv3 while preserving the MIT notices. The native TV interface, remote navigation and YouTube playback remain the work of SmartTube and its contributors.

The TV source bundle contains the exact original trees of SmartTube, both copies of SharedModules, MediaServiceCore, the pinned anddea tree, and the YouTube-tr overlay/build scripts. The overlay marks modified files, adds visible attribution, changes branding and disables incompatible upstream update prompts. The TV signing key is local and is not the upstream SmartTube key. GmsCore is not included in the TV APK.

## ReVanced GmsCore / microG

Source: https://github.com/ReVanced/GmsCore/tree/v0.3.13.2.250932

Version: 0.3.13.2.250932, version code 250932004, included since YouTube+tr 1.0.2. The redistributed APK is unmodified and its SHA-256 matches the official release asset. The corresponding upstream source archive is included in the release as `GmsCore-0.3.13.2.250932-source.tar.gz`; it retains the upstream license texts, copyright notices, dependency references and build instructions. Earlier YouTube+tr releases included unmodified GmsCore 0.3.1.4.240913 and its matching source archive. The microG project and its components retain their respective licenses, including Apache-2.0 where indicated upstream.

## Morphe Desktop

Build tool: https://github.com/MorpheApp/morphe-desktop/releases/tag/v1.20.0

This repository downloads the tool from its official release and verifies the pinned SHA-256. The tool binary is not included in the repository or release. The original tool license and branding remain with its authors.

## YouTube

YouTube and its proprietary application components belong to Google. The GPL license for this repository's scripts and upstream patches does not relicense Google's application or grant trademark rights. This project has no affiliation with Google or Yandex.
