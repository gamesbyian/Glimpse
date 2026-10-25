#!/usr/bin/env python3
"""Independently enforce that native Android instrumentation actually ran.

A green Gradle task isn't sufficient if API tests were silently skipped.
Counts are expected from the current C-native suite, not old screenshots.
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RESULTS=ROOT/"app/build/outputs/androidTest-results/connected"
EXPECTED={
    "io.github.gamesbyian.glimpse.data.PhotoImporterDeviceTest": {
        "verifiedOfflineCopySurvivesSourceDeletion",
        "duplicateBytesDeduplicateButMembershipIsCollectionSpecific",
        "invalidInputDoesNotDeleteSourceOrPersistPhotos",
        "sameUriWithMutatedPixelsCreatesDifferentImmutableIdentity",
        "corruptedPrivateCopyIsAtomicallyRestoredBeforeReusingHashId",
        "controlledContentProviderGrantCanExpireAfterOfflineImport",
        "realAndroidMediaStoreImageRemainsOfflineAfterPublicDeletion",
    },
    "io.github.gamesbyian.glimpse.domain.WallpaperGatewayDeviceTest": {
        "explicitHomeCommandAgreesWithImmediateOsWallpaperIdReadback",
        "pendingUncertainOperationBlocksApplyWithoutCallingOs",
        "decodableButChangedPixelsNeverReachAndroidWallpaperManager",
        "exceptionAtOsBoundaryPreservesPendingAndNeverRetries",
        "cancellationAtOsBoundaryPersistsPauseAndPropagatesCancellation",
    },
}
def main():
    files=list(RESULTS.rglob("TEST-*.xml"))
    if not files:
        raise ValueError(f"No native Android JUnit XML under {RESULTS}")
    observed=set()
    for source in files:
        root=ET.parse(source).getroot()
        for case in root.iter("testcase"):
            classname=case.get("classname","")
            name=case.get("name","")
            if classname not in EXPECTED:
                continue
            if name not in EXPECTED[classname]:
                continue
            if any(case.find(x) is not None for x in ("skipped","failure","error")):
                raise ValueError(f"Native test failed or skipped: {classname}.{name}")
            observed.add((classname,name))
    needed={(klass,name) for klass,names in EXPECTED.items() for name in names}
    missing=needed-observed
    if missing:
        raise ValueError(f"Required native cases missing: {sorted(missing)}; JUnit files: {files}")
    print(f"PASS: {len(observed)} independent Android instrumentation "
          "tests actually executed without failures or skips, including "
          "content:// URI revocation, offline SHA repair, and Home OS readback")
    print("Emulator evidence only. Not physical Pixel, Photo Picker, "
          "TalkBack, Doze, OS future-state or human design approval.")
    return 0

if __name__=="__main__":
    try:
        sys.exit(main())
    except (ValueError, OSError, ET.ParseError) as exc:
        print(f"FAIL: native Android evidence gate: {exc}",file=sys.stderr)
        sys.exit(1)
