#!/usr/bin/env python3
"""Evidence gate for the *unselected* Grid provenance proposal.

This guards exact CC0 JPEG bytes, four visibly blocked photo IDs and unchanged
pre-existing p12 research selection. Pass != cleared p10-p15 rights or native proof.
"""
import base64
import hashlib
import html
import json
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[2]
FILE=ROOT/'design/proposals/whole-product-candidates/grid-photography-provenance-p12.svg'
NS='{http://www.w3.org/2000/svg}'
VERIFIED={'p06':('38','216',58415,'c5902098629b2ce85686a94204ea011a88711ac6d65308aa48323d9052a8dc1a'),
          'p07':('126','216',77791,'a64e072f62f72b5ebd167bea0ad44f64e6d51a7d07a7dc78d01c0c0fc0160e59')}
BLOCKED={'p08':('214','216'),'p09':('302','216'),'p16':('214','434'),'p17':('302','434')}
P12_FIXTURE_SHA='6ab0cfafca534be77192041b78c763dc315fedf97cbd21cdcf9f69138129c905'

def main():
    data=FILE.read_bytes(); root=ET.fromstring(data)
    m=root.find(NS+'metadata')
    assert m is not None and m.text
    state=json.loads(html.unescape(m.text))
    assert state['selectedAssetId']=='p12' and state['wallpaperMutation'] is False
    assert state['ownerApproved'] is False and state['androidVerified'] is False
    assert state['photoFixtureSHA256']['p12']==P12_FIXTURE_SHA
    assert set(state['photoBlockedIDs']) == set(BLOCKED)
    assert 'UNVERIFIED' in state['gridPhotoFixturePolicy']
    assert 'sourceSHA256' not in state, 'Legacy hash must be labeled sourceBaselineSHA256'
    images=list(root.iter(NS+'image'))
    found={}
    for im in images:
        key=(im.get('x'),im.get('y'))
        link=im.get('href') or im.get('{http://www.w3.org/1999/xlink}href')
        if link and link.startswith('data:image/jpeg;base64,'):found[key]=base64.b64decode(link.split(',',1)[1],validate=True)
    for name,(x,y,n,sha) in VERIFIED.items():
        raw=found[(x,y)]
        assert len(raw)==n, (name,len(raw),n)
        assert hashlib.sha256(raw).hexdigest()==sha, name
        assert state[name+'CC0PhotoSHA256']==sha, name
    originals=found[('0','0')]
    assert hashlib.sha256(originals).hexdigest()==P12_FIXTURE_SHA, 'p12 selected fixture changed'
    strings=data.decode('utf-8')
    for name in BLOCKED:
        assert strings.count(name+' · NO PHOTO')==1, name
        assert f'id="wash-{name}"' in strings
        assert name in state['photoBlockedIDs']
    assert len([t for t in images if t.get('x') in ('38','126') and t.get('y')=='216'])==2
    assert 'p10-p15 rights unverified' in strings
    print(json.dumps({'candidate':str(FILE.relative_to(ROOT)),
      'sourceSHA256':hashlib.sha256(data).hexdigest(),
      'confirmedCC0Bytes':sorted(VERIFIED),'rightsBlockedFallbackIDs':sorted(BLOCKED),
      'p12FixtureSHA256':P12_FIXTURE_SHA,'legacyResearchRights':'unverified',
      'AndroidVerified':False,'ownerApproved':False},indent=2))

if __name__=='__main__':main()
