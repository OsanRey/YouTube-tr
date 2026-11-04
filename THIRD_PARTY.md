# Third-party projects

YouTube+tr is an unofficial personal build configuration. It does not claim authorship of the upstream patching or translation implementations and is not an official Google, Yandex, ReVanced, microG, or Morphe release.

## anddea patches

Source: https://github.com/anddea/revanced-patches/tree/d4c3ce4b67155c4ea728aa831f5485bdb3d75934

Version: 4.3.0. The original upstream source archive is included in the release as `anddea-patches-4.3.0-source.tar.gz`. Build-time options select the name, icon and included patches. Since YouTube+tr 1.0.1, one SponsorBlock layout resource is replaced with the marked derivative in `overrides/youtube/sponsorblock/shared/layout/revanced_sb_skip_sponsor_button.xml` to fix label clipping with enlarged fonts. The replacement source and its application script are included in this repository at the corresponding release tag. No upstream bytecode is changed by this local fix. Original copyright notices, upstream credits and license information remain in the source archive and application. Preserve the upstream GPLv3 additional terms in the relevant source files and the [NOTICE](NOTICE) when redistributing covered work.

The Yandex translation integration credits anddea, Jav1x and other contributors identified in the upstream files. It is based on https://github.com/ilyhalight/voice-over-translation and its credited predecessors.

## ReVanced GmsCore / microG

Source: https://github.com/ReVanced/GmsCore/tree/v0.3.13.2.250932

Version: 0.3.13.2.250932, version code 250932004, included since YouTube+tr 1.0.2. The redistributed APK is unmodified and its SHA-256 matches the official release asset. The corresponding upstream source archive is included in the release as `GmsCore-0.3.13.2.250932-source.tar.gz`; it retains the upstream license texts, copyright notices, dependency references and build instructions. Earlier YouTube+tr releases included unmodified GmsCore 0.3.1.4.240913 and its matching source archive. The microG project and its components retain their respective licenses, including Apache-2.0 where indicated upstream.

## Morphe Desktop

Build tool: https://github.com/MorpheApp/morphe-desktop/releases/tag/v1.20.0

This repository downloads the tool from its official release and verifies the pinned SHA-256. The tool binary is not included in the repository or release. The original tool license and branding remain with its authors.

## YouTube

YouTube and its proprietary application components belong to Google. The GPL license for this repository's scripts and upstream patches does not relicense Google's application or grant trademark rights. This project has no affiliation with Google or Yandex.
