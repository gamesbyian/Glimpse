#!/usr/bin/env python3
"""Check unselected three-column Grid evidence without implying Android parity."""
from __future__ import annotations
import base64,hashlib,html,json
from pathlib import Path
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
PATH=ROOT/'design/proposals/whole-product-candidates/grid-3column-native-aligned-p12.svg'
SVG='{http://www.w3.org/2000/svg}'
HASH={
'p06':'c5902098629b2ce85686a94204ea011a88711ac6d65308aa48323d9052a8dc1a',
'p07':'a64e072f62f72b5ebd167bea0ad44f64e6d51a7d07a7dc78d01c0c0fc0160e59',
'p10':'26320b6f0324734ef00c1d359ab2bbeb01063048a3b4760a6d72c3caaae916ab',
'p11':'2b2aefbdee4a4934e6ce8476d6019cfda422a45f85ac417e29f629f863119891',
'p12':'6ab0cfafca534be77192041b78c763dc315fedf97cbd21cdcf9f69138129c905',
'p13':'9bd8d1c812c278572df94547c6e22e9e3b68c9e2ea56b9d2090d8109802ae2d4',
'p14':'4f728e108d60f3ac6940e26043179841f6433fdd93afddb0a929aea05c80ada2',
'p15':'cc6bf8f1766e8f4c3a09d3780c753b76c1ee448a847a9582cb07df4f40010346'}
BLOCKED=['p08','p09','p16','p17']
ids=[f'p{x:02d}' for x in range(6,18)]
def main():
    data=PATH.read_bytes();root=ET.fromstring(data)
    meta=root.find(SVG+'metadata'); assert meta is not None and meta.text
    s=json.loads(html.unescape(meta.text))
    assert s['columns']==3 and s['collectionSize']==22 and s['selectedAssetId']=='p12'
    assert s['displayedAssetIds']==ids and s['status']=='UNSELECTED_DRAFT'
    assert set(s['rightsUnverifiedResearchIDs'])==set(HASH)-{'p06','p07'}
    assert set(s['photoBlockedIDs'])==set(BLOCKED)
    assert s['wallpaperMutation'] is False and s['androidVerified'] is False
    assert s['humanApproved'] is False
    images=[i for i in root.iter(SVG+'image') if (i.get('href') or '').startswith('data:image/jpeg;base64,')]
    assert len(images)==len(HASH)
    for id,sha in HASH.items():
        index=ids.index(id); x=39+(index%3)*118; y=202+(index//3)*112
        selected=[i for i in images if i.get('x')==str(x) and i.get('y')==str(y)]
        assert len(selected)==1, (id,'missing, swapped or duplicate jpeg')
        dat=base64.b64decode(selected[0].get('href').split(',',1)[1],validate=True)
        assert hashlib.sha256(dat).hexdigest()==sha, (id,'image bytes changed')
    text=data.decode('utf-8')
    assert text.count('NO PHOTO</text>')==len(BLOCKED)
    assert text.count('RIGHTS?</text>')==6
    assert text.count('Back to reel</text>')==1
    assert text.count('View p12</text>')==1
    print(json.dumps({'sourceSHA256':hashlib.sha256(data).hexdigest(),
        'visible':12,'columns':3,'p06p07_CC0_exact':True,
        'p10p15ResearchBytesExactRightsUnknown':True,'blockedFallbacks':BLOCKED,
        'AndroidNativeVerified':False,'ownerApproved':False},indent=2))
if __name__=='__main__':main()
