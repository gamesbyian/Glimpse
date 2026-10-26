#!/usr/bin/env python3
"""Fail-closed verification of six *still-unapproved* cross-surface CC0 candidates.

Checks actual embedded JPEG SHA256, immutable ID semantics, metadata and visible
single-photo selection. Not a license grant, Android behavior, owner approval or
permission to change the integration-owned selected manifest.
"""
from __future__ import annotations
import base64
import hashlib
import html
import json
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
DIR=ROOT/'design/proposals/whole-product-candidates/cc0-consistent-20261024'
SVG='{http://www.w3.org/2000/svg}'
EXPECTED={
'p04':'20d6a8dab7e5eed0fbbbb0d98de799487512264aa7d0cf7ced701944683e2bd0',
'p06':'c5902098629b2ce85686a94204ea011a88711ac6d65308aa48323d9052a8dc1a',
'p07':'a64e072f62f72b5ebd167bea0ad44f64e6d51a7d07a7dc78d01c0c0fc0160e59',
'p08':'68a2e5c556be1db5a3ac6abdf60a8e2c2eaa8773aae84f48a3bf600a9c73e656',
'p09':'7a01f8b4f1ab6a6a28cf85b4b4387f0df9a8734186eb322568e273a5174cc00d',
'p10':'8baff97c5dc73daa533bde70ac7bb3bd08e767a93d316894c9b9f1161d9331b9',
'p11':'658b7e6b88eb1d28b922042427e7eceac7f7fbef88454e0919666980096d645a',
'p12':'e781fb03f2fd56a97e409118009524fe79be4dd68f1b871108fe7073e36d15be',
'p13':'c62038af15c3cc41b0ca607cf144fdb4069d1bbc8dc349014f87b6a63c14badf',
'p14':'3fe5b70cfc9d7994a7ac0732413e4a19b9167d7f6ba5b47cda386208e0c6350e',
'p15':'5b0f2e0e7afcfcae31f0641409819087fe3d69611912d1430ebdd804783ded74',
'p16':'92d30c89130514f15bcb82539db6b31c9ae5f6175a73c13d02aa6c04161c4d48',
'p17':'ab0010efc6ebed8ada9cefc3ea0f246ef0a4de4437ea9efc8a6ebf83b0524d8e'
}
REEL={'fixture1':'p12','fixture2':'p10','fixture3':'p11','fixture4':'p13','fixture5':'p14','fixture6':'p15'}
GRID_ORDER=[f'p{i:02}' for i in range(6,18)]

def jpeg(img: ET.Element) -> bytes:
    href=img.get('href') or img.get('{http://www.w3.org/1999/xlink}href')
    assert href and href.startswith('data:image/jpeg;base64,'), 'Referenced visual is not an embedded JPEG'
    return base64.b64decode(href.split(',',1)[1],validate=True)

def assert_jpeg(name:str,img:ET.Element) -> None:
    digest=hashlib.sha256(jpeg(img)).hexdigest()
    assert digest==EXPECTED[name],f'Photo bytes or ID mismatch: {name}, got {digest}'

def read(name:str):
    path=DIR/(name+'.svg')
    raw=path.read_bytes()
    root=ET.fromstring(raw)
    assert root.get('viewBox')=='0 0 1160 920'
    metadata=root.find(SVG+'metadata')
    assert metadata is not None and metadata.text
    state=json.loads(html.unescape(metadata.text))
    assert state.get('photoSourceMode')=='rights-verified-replacements',name
    assert (state.get('selectedAssetId')=='p12' if name in ('home','reel','grid-3column','detail')
            else state.get('previewAssetId')=='p12' if name=='collections'
            else state.get('sourceAssetId')=='p12' and state.get('selectedAssetId')=='p12c'),name
    assert (state.get('ownerApproved') is False or state.get('humanApproved') is False
            or state.get('visualApproval')=='not-owner-approved')
    assert state.get('wallpaperMutation',False) is False
    assert state.get('androidVerified',False) is False
    # WCAG 2.2 normal-size static text: previous #667975 on paper was only 4.07:1.
    def luminance(code:str)->float:
        v=[int(code[x:x+2],16)/255 for x in (1,3,5)]
        linear=[c/12.92 if c<=0.04045 else ((c+0.055)/1.055)**2.4 for c in v]
        return sum(w*c for w,c in zip((0.2126,0.7152,0.0722),linear))
    def ratio(a:str,b:str)->float:
        lo,hi=sorted((luminance(a),luminance(b)))
        return (hi+0.05)/(lo+0.05)
    assert ratio('#566861','#F6F0E6')>=4.5
    assert '#667975' not in raw.decode('utf-8'), 'Inaccessible legacy secondary text token in CC0 state'
    return path,root,state

def check():
    manifest=json.loads((DIR/'migration-manifest.v1.json').read_text())
    assert manifest['status']=='EDITORIAL_CC0_SIX_REVIEW_CANDIDATE_NOT_OWNER_APPROVED'
    assert manifest['selectedId']=='p12'
    assert manifest['lastGlimpseAcknowledgedId']=='p04'
    assert manifest['androidWallpaperCurrentVerified'] is False
    assert manifest['nativeAcceptance'] is False and manifest['ownerApproved'] is False
    assert set(manifest['sourcePaths'])=={'home','reel','grid','photo-detail','collections','share'}
    recorded=manifest['verifiedIndependentPhotos']
    assert len(recorded)==13 and {z['id'] for z in recorded}==set(EXPECTED)
    for asset in recorded:
        assert asset['derivedSHA256']==EXPECTED[asset['id']]
        assert 'CC0' in asset['license']
        assert asset.get('sourceUrl') and asset.get('assetPath')
    companion=manifest['supplementalUnselectedSourceDetail']
    assert companion['sourcePhotoId']=='p12'
    assert companion['path']==str((DIR/'source-p12.svg').relative_to(ROOT))
    assert companion['ownerApproved'] is False and companion['nativeVerified'] is False
    assert manifest['derivedPreview']['id']=='p12c'
    assert manifest['derivedPreview']['sourceId']=='p12'
    assert manifest['derivedPreview']['sharesheetDeliveryConfirmed'] is False
    records={}
    for name in ('home','reel','grid-3column','detail','collections','share'):
        path,root,state=read(name)
        images=list(root.iter(SVG+'image'))
        if name=='home':
            assert len(images)==2
            hero=next(i for i in images if (i.get('x'),i.get('y'))==('52','159'))
            selected=next(i for i in images if (i.get('x'),i.get('y'))==('53','512'))
            assert_jpeg('p04',hero)
            assert_jpeg('p12',selected)
            assert state['lastConfirmedAppliedAssetId']=='p04'
            assert state['presentOSWallpaperStatus']=='not-read-back'
            assert state['p12FixtureSHA256']==EXPECTED['p12']
        elif name=='reel':
            rendered_markup=path.read_text()
            for xx,ww in (('40','122'),('172','219')):
                assert ('<rect x="'+xx+'" y="407" width="'+ww+'" height="49"') in rendered_markup, 'Reel action below 48px'
            assert '#667975' not in rendered_markup.split('<rect x="442" y="14"')[0], 'Low contrast Reel secondary text'
            assert '>View grid</text>' in rendered_markup and '>View all</text>' not in rendered_markup, 'Reel/Grid navigation ambiguous'
            assert len(images)==6
            by_id={i.get('id'):i for i in images}
            assert set(by_id)==set(REEL), 'Reel has missing/duplicate fixture'
            for fixture,pid in REEL.items():assert_jpeg(pid,by_id[fixture])
            for pid in REEL.values():
                assert state['photoFixtureSHA256'][pid]==EXPECTED[pid]
        elif name=='grid-3column':
            assert len(images)==12, ('Expected twelve distinct independent JPEGs',len(images))
            index={(i.get('x'),i.get('y')):i for i in images}
            assert len(index)==12
            for j,pid in enumerate(GRID_ORDER):
                key=(str(39+(j%3)*118),str(202+(j//3)*112))
                assert_jpeg(pid,index[key])
                assert state['sourcePhotoCC0'][pid]['sha256']==EXPECTED[pid]
            assert state['columns']==3 and state['photoBlockedIDs']==[]
            assert state['rightsUnverifiedResearchIDs']==[]
            assert state['p12FixtureSHA256']==EXPECTED['p12']
            visible=path.read_text()
            assert 'NO PHOTO</text>' not in visible and 'RIGHTS?</text>' not in visible
            assert '>CC0</text>' not in visible, 'Tiny visible license tags misleadingly generalize collection contents'
            assert 'Sample-photo provenance</text>' in visible
            assert 'Per-photo rights are not inferred for arbitrary imports.</text>' in visible
            # Twelve strong-keyline label plates are intentionally source geometry;
            # every sample ID remains readable without a tiny license badge.
            for j,pid in enumerate(GRID_ORDER):
                xx=43+(j%3)*118; yy=277+(j//3)*112
                plate=next((e for e in root.iter(SVG+'rect')
                    if e.get('x')==str(xx) and e.get('y')==str(yy)),None)
                assert plate is not None and plate.get('width')=='56'
                assert plate.get('height')=='23' and plate.get('stroke')=='#1D5960'
                assert float(plate.get('stroke-width','0'))>=1.25
                label=next((e for e in root.iter(SVG+'text')
                    if e.get('x')==str(xx+7) and e.get('y')==str(yy+16)),None)
                assert label is not None and label.text==pid.upper(),pid
        elif name=='detail':
            detail_markup=path.read_text()
            assert '<rect x="269" y="63" width="113" height="50"' in detail_markup, 'Grid return target below 48px'
            assert '>●  ▰</text>' not in detail_markup, 'Unsupported status glyph'
            assert '#667975' not in detail_markup.split('<rect x="442" y="14"')[0], 'Low contrast Detail secondary text'
            assert 'public research photo fixture' not in detail_markup, 'Stale visual reviewer provenance'
            assert 'All displayed p12 images reuse C-verified photo bytes' in detail_markup
            assert len(images)==1
            assert_jpeg('p12',images[0])
            assert state['p12FixtureSHA256']==EXPECTED['p12']
            assert state['photoCredit']['creator']=='Sporlo'
            assert state['photoCredit']['independentSourcePhotoId']=='p12'
            assert state['photoCredit']['sourceUrl'].startswith('https://commons.wikimedia.org/wiki/File:Chive_flower')
            assert 'CC0' in state['photoCredit']['licenseClaim']
            assert state['photoCredit']['originalPreserved'] is True
            visible=path.read_text()
            assert 'Source and rights ›</text>' in visible
            assert 'Sporlo · CC0 source listed</text>' in visible
            assert 'Local photo p12</text>' not in visible
            assert '<clipPath id="detail-mini-p12">' in visible
            thumbnails=[node for node in root.iter(SVG+'use') if node.get('href')=='#fixture1']
            assert len(thumbnails)==2, 'Hero and source card must reuse exact same p12 fixture'
        elif name=='collections':
            assert len(images)==3
            order=[('Rainbows','p12','261'),('Golden Hour','p08','411'),('Patterns','p15','561')]
            for collection,source,yy in order:
                photo=next((x for x in images if x.get('x')=='53' and x.get('y')==yy),None)
                assert photo is not None, (collection,'missing cover')
                assert_jpeg(source,photo)
                assert state['sourcePhotoMap'][collection]['id']==source
                assert state['sourcePhotoMap'][collection]['sha256']==EXPECTED[source]
            assert state['wallpaperMutation'] is False
        else:
            assert name=='share' and len(images)==3
            for image in images:assert_jpeg('p12',image)
            assert state['sourcePhotoSHA256']==EXPECTED['p12']
            assert state['selectedAssetId']=='p12c' and state['derivedAssetId']=='p12c'
            assert state['shareRecipientConfirmed'] is False
            assert state['wallpaperMutation'] is False
            assert state['closeBehavior'].startswith('Close 48px')
            assert state['sharesheetOutcome'].startswith('Open OS chooser')
            visible=path.read_text()
            assert '<rect x="31" y="68" width="48" height="48"' in visible
            assert 'Opens Android Sharesheet; delivery not confirmed.' in visible
        records[name]={'source':str(path.relative_to(ROOT)),
                       'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
                       'jpegCount':len(images),'sourceByteIntegrity':'PASS',
                       'renderIndependentlyInspected':False,
                       'AndroidVerified':False,'ownerApproved':False}
    extra=DIR/'source-p12.svg'
    source_bytes=extra.read_bytes()
    source_root=ET.fromstring(source_bytes)
    source_meta=source_root.find(SVG+'metadata')
    assert source_meta is not None and source_meta.text
    credit=json.loads(html.unescape(source_meta.text))
    assert credit['photoSourceMode']=='rights-verified-replacements'
    assert credit['sourceUrl'].startswith('https://commons.wikimedia.org/wiki/File:Chive_flower')
    assert credit['selectedAssetId']=='p12' and credit['creator']=='Sporlo'
    assert credit['photoSourceSHA256']==EXPECTED['p12']
    assert 'CC0' in credit['licenseClaim']
    assert credit['nativeUrlNavigationVerified'] is False
    assert credit['ownerApproved'] is False and credit['wallpaperMutation'] is False
    source_images=list(source_root.iter(SVG+'image'))
    assert len(source_images)==1
    assert_jpeg('p12',source_images[0])
    text=source_bytes.decode('utf-8')
    for snippet in ('Creator · Sporlo</text>','Creator and source rights for p12</text>',
                    'Rights depend on each photo</text>',
                    'Do not generalize this CC0 label.</text>',
                    'Source: Wikimedia Commons</text>', 'Back to p12</text>'):
        assert snippet in text, ('Source details missing',snippet)
    records['source-p12']={'source':str(extra.relative_to(ROOT)),
        'sha256':hashlib.sha256(source_bytes).hexdigest(),'jpegCount':1,
        'sourceByteIntegrity':'PASS','renderIndependentlyInspected':False,
        'AndroidVerified':False,'ownerApproved':False}
    assert credit['creatorRowBehavior'].startswith('Informational')
    assert credit['sourceRowBehavior'].startswith('Informational')
    assert credit['externalNavigationRequiresExplicitTap'] is True
    assert '<text x="382" y="515"' not in text
    assert '<text x="382" y="591"' not in text
    assert 'Open file page…</text>' in text
    assert len(set(EXPECTED.values()))==13
    return records

if __name__=='__main__':
    result=check()
    print(json.dumps({'photoSourceMode':'rights-verified-replacements',
        'distinctClearedIdentities':len(EXPECTED),'screens':result,
        'candidateSelection':'UNSELECTED','AndroidVerified':False,'ownerApproved':False},indent=2))
