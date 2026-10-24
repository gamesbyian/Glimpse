#!/usr/bin/env python3
"""Production A source/401x890 raster checks. Does NOT demonstrate Android, TalkBack or owner acceptance."""
from __future__ import annotations
import argparse
import hashlib
import io
import json
from pathlib import Path
import xml.etree.ElementTree as ET

SCREENS = ('quick-import','settings','quickstart','discover','destinations','automation','activity',
           'image-editor','automation-editor','source-detail','schedule-simulator',
           'diagnostics','widgets','quick-settings-and-shortcuts','backup-and-restore',
           'onboarding','error-empty-permission','accessibility-preferences')
REQUIRED = {
'quick-import':('SAMPLE PREVIEW','Save to collection','Saving does not change your wallpaper.',
                'Just save a copy','Show next','Apply…'),
'settings':('Network and privacy','Backup and restore'),
'quickstart':('Start rotation','Requested: every 5 minutes','Home screen','Lock screen'),
'discover':('Wikimedia Commons','Review each original','Search sources'),
'destinations':('Home screen','Lock screen','Android chooser'),
'automation':('Foreground rotation enabled','Change Home now','Pause rotation','exact timing unknown'),
'activity':('Recorded Glimpse actions','Home acknowledged; device may differ','Review p04','Review last Home choice','no new apply logged'),
'image-editor':('Edit a copy','Save new copy','Saving does not apply'),
'automation-editor':('Save draft','Conflict review required'),
'source-detail':('Provider record','No hidden provider permissions'),
'schedule-simulator':('Hypothetical sequence only','Try another simulation'),
'diagnostics':('Current OS wallpaper: unknown','Review before retrying'),
'widgets':('Your launcher places the widget',),
'quick-settings-and-shortcuts':('Android owns tile placement',),
'backup-and-restore':('Include photograph files','Preview before any restore'),
'onboarding':('Your library stays yours',),
'error-empty-permission':('No photos yet','Choose photos'),
'accessibility-preferences':('200% design target','Grid'),
}
NS='{http://www.w3.org/2000/svg}'
ROOT=Path(__file__).resolve().parents[2]

def verify(file:Path, output:Path|None):
    data=file.read_bytes(); raw=data.decode('utf-8'); root=ET.fromstring(data)
    assert root.tag==NS+'svg' and root.get('viewBox')=='0 0 1160 920', file
    assert '#F6F0E6' in raw and '#153F46' in raw, file
    assert 'PROPOSAL' in raw and 'NOT APPROVED' in raw, file
    assert 'ownerApproved&quot;:true' not in raw and 'ownerApproved":true' not in raw, file
    assert not any(n.tag in (NS+'script',NS+'foreignObject') for n in root.iter()),file
    assert any(n.get('x')=='15' and n.get('y')=='14' and n.get('width')=='401'
               and n.get('height')=='890' for n in root.iter(NS+'rect')),file
    phone=raw.split('<rect x="439" y="14"')[0]
    assert '<rect x="34" y="68" width="40" height="40"' not in phone, file
    assert '<rect x="358" y="73" width="40" height="40"' not in phone, file
    for phrase in REQUIRED.get(file.stem,()):
        assert phrase in phone,(file,'missing phone-side phrase',phrase)
    if file.stem == 'quick-import':
        for yy in ('451',):
            assert all(('<rect x="'+xx+'" y="'+yy+'" width="'+w+'" height="50"') in phone
                       for xx,w in [('44','100'),('158','92'),('262','121')]), "Edit targets must be >=48px"
        assert '<rect x="41" y="685" width="342" height="52"' in phone
        assert '<rect x="41" y="746" width="165" height="50"' in phone
        assert '<rect x="217" y="746" width="166" height="50"' in phone
    row={'source':str(file),'sourceSHA256':hashlib.sha256(data).hexdigest(),
         'phoneCrop':[15,14,401,890],'sourceValidated':True,
         'rasterValidated':False,'independentlyOpticallyInspected':False,
         'AndroidNativeVerified':False,'humanApproved':False}
    if output:
        import cairosvg
        from PIL import Image,ImageStat
        png=cairosvg.svg2png(bytestring=data,output_width=1160,output_height=920)
        im=Image.open(io.BytesIO(png)).convert('RGB').crop((15,14,416,904))
        assert im.size==(401,890) and sum(ImageStat.Stat(im).stddev)>40,file
        output.mkdir(parents=True,exist_ok=True)
        dest=output/(file.stem+'.phone.png'); im.save(dest)
        row.update({'cropPath':str(dest),'cropSHA256':hashlib.sha256(dest.read_bytes()).hexdigest(),
                    'rasterValidated':True})
    return row

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--paths',nargs='*',help='Explicit SVG paths; default: all 17 A-owned primary plates')
    p.add_argument('--render-output',type=Path)
    args=p.parse_args()
    files=[Path(a) for a in args.paths] if args.paths else [ROOT/'design/proposals/plates'/(a+'.svg') for a in SCREENS]
    rows=[verify(f,args.render_output) for f in files]
    if args.render_output:(args.render_output/'source-raster-ledger.json').write_text(json.dumps(rows,indent=2)+'\n')
    print(json.dumps({'checked':len(rows),'sourcePASS':len(rows),'rendered':bool(args.render_output),
          'AndroidNativeVerified':False,'humanApproved':False},indent=2))

if __name__=='__main__':main()
