#!/usr/bin/env python3
"""Guard the four selected Glimpse task screens against regression to empty cards.

These are strict SOURCE semantic/fixture checks, not optical, native Android,
gesture, accessibility, delivery or owner approval tests. A phone text assertion
reads only x <= 415, never right-side designer notes.
"""
import base64,hashlib,json,runpy
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
NS="{http://www.w3.org/2000/svg}"
X="{http://www.w3.org/1999/xlink}href"
P={
 'home':"design/proposals/audit-revisions/r03-home-primary-actions-p04.svg",
 'reel':"design/proposals/audit-revisions/r01-photo-primary-wallpaper-action.svg",
 'grid':"design/proposals/audit-revisions/r02-grid-primary-actions-p12.svg",
 'photo-detail':"design/proposals/audit-revisions/r02-photo-detail-actions-p12.svg"
}
V={
 'home':["Photo 04 · Home","p12 selected, not applied","Device may have changed since",
         "Change Home now","Pause shuffle","Open Rainbows","Share saved p04","Automation"],
 'reel':["Photo 12 of 22 · Preview only","SELECTED p12",
         "View all","Set wallpaper…","Previous photo","Next photo","Collections"],
 'grid':["Selected p12 of 22 · same as Reel","Search photos / jump to number",
         "Back to reel","View p12","Share p12","Select multiple"],
 'photo-detail':["Details for p12","Local photo p12","Rainbows · original preserved",
         "Grid","Set wallpaper…","Share photo","Edit copy"]
}

def load(p):
    tree=ET.parse(ROOT/p)
    svg=tree.getroot()
    assert svg.tag==NS+"svg" and svg.get("viewBox")=="0 0 1160 920",p
    meta=svg.find(NS+"metadata")
    assert meta is not None,p
    info=json.loads(meta.text)
    phone=set()
    for node in svg.iter(NS+"text"):
        try:x=float(node.get("x","9999"))
        except ValueError:continue
        if x<=415:
            value="".join(node.itertext()).strip()
            if value:phone.add(value)
    assert not info.get("ownerApproved"),p
    return svg,info,phone

def jpeg_sha(svg,ident=None):
    for node in svg.iter(NS+"image"):
        if ident and node.get("id")!=ident:continue
        uri=node.get("href",node.get(X,""))
        if uri.startswith("data:image/jpeg;base64,"):
            return hashlib.sha256(base64.b64decode(uri.split(",",1)[1])).hexdigest()
    raise AssertionError(f"Missing JPEG {ident or ''}")

def main():
    review=json.loads((ROOT/"design/proposals/whole-product-review-selection.v1.json").read_text(encoding="utf-8"))
    assert review["approval"]["ownerApproved"] is False
    choices={x["surfaceId"]:x["reviewCandidatePath"] for x in review["primarySurfaces"]}
    migration=json.loads((ROOT/"design/proposals/whole-product-candidates/cc0-consistent-20261024/migration-manifest.v1.json").read_text(encoding="utf-8"))
    assert all(choices[role]==path for role,path in migration["sourcePaths"].items()),"Six-screen CC0 draft preference must remain atomic"
    assert runpy.run_path(str(ROOT/"scripts/validate-agent-c-review-selection.py"))["validate"]()=="ATOMIC_CC0_SIX_UNAPPROVED"
    # Preserve the old four task-action proposals as independently checked alternatives.
    observed={}
    for family,path in P.items():
        svg,info,phone=load(path)
        missing=set(V[family])-phone
        assert not missing,f"{family} missing PHONE-side labels: {sorted(missing)}"
        observed[family]=(svg,info)
    home=observed["home"][1]
    reel=observed["reel"][1]
    grid=observed["grid"][1]
    detail=observed["photo-detail"][1]
    assert home["lastConfirmedAppliedAssetId"]=="p04"
    assert home["selectedAssetId"]=="p12"
    assert home["presentOSWallpaperStatus"]=="not-read-back"
    assert home["wallpaperMutation"] is False
    assert "illustrative vector" in home["p04HeroArtwork"]
    assert reel["selectedAssetId"]=="p12" and reel["photoTotal"]==22
    assert reel["wallpaperTarget"] is None and reel["requestedAssetId"] is None
    assert reel["appliedStatus"]=="unchanged"
    assert "chooser" in reel["primaryActionSemantic"].lower()
    assert grid["selectedAssetId"]=="p12" and grid["photoTotal"]==22
    assert grid["wallpaperMutation"] is False
    assert grid["selectedAssetIDs"]==["p12"]
    assert not grid["multiSelectActive"]
    assert detail["selectedAssetId"]=="p12" and detail["appliedStatus"]=="unchanged"
    assert "chooser" in detail["setWallpaperSemantic"].lower()
    p12_source_sha=reel["photoFixtureSHA256"]["p12"]
    assert jpeg_sha(observed["reel"][0],"fixture1")==p12_source_sha
    assert jpeg_sha(observed["grid"][0],"fixture1")==p12_source_sha
    assert jpeg_sha(observed["photo-detail"][0],"fixture1")==p12_source_sha
    assert jpeg_sha(observed["home"][0])==p12_source_sha
    for pid in ("p10","p11","p12","p13","p14"):
        assert grid["photoFixtureSHA256"][pid]==reel["photoFixtureSHA256"][pid],pid
        assert grid["fixtureArtworkByPhotoId"][pid]==reel["fixtureArtworkByPhotoId"][pid],pid
    assert grid["fixtureArtworkByPhotoId"]["p15"]=="art6"
    assert len(set(grid["photoFixtureSHA256"].values()))==6
    assert len(review["primarySurfaces"])==24
    print("PASS: coordinated six-screen draft preference passes atomic source guard; four historical task-action alternatives preserve controls; "
          "p12 byte-identical across Home/Reel/Grid/Detail; six Grid fixtures distinct; "
          "OS current state unknown and all owner approvals absent")
    print("NOTE: 200% font, native touch, current selected visual optical review "
          "and source-verified p04 still require separate evidence")

if __name__=="__main__":main()
