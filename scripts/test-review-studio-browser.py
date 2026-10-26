#!/usr/bin/env python3
"""Run an actual Chromium review of the static Glimpse owner studio.

This tests UI interaction and browser persistence, not Android native accessibility
or whether human reviewers approve any design.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import json
import os
import subprocess
import time
import urllib.request

from playwright.sync_api import sync_playwright

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"review/browser-evidence"
OUT.mkdir(parents=True,exist_ok=True)
server=subprocess.Popen(["python3","-m","http.server","8765","--bind","127.0.0.1"],
    cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
try:
    for _ in range(30):
        try:
            urllib.request.urlopen("http://127.0.0.1:8765/review/",timeout=1)
            break
        except Exception:
            time.sleep(.3)
    else:
        raise AssertionError("Local review site did not start")
    with sync_playwright() as pw:
        browser=pw.chromium.launch(headless=True, channel=os.getenv('GLIMPSE_CHROME_CHANNEL') or None)
        desktop=browser.new_context(viewport={"width":1440,"height":900},accept_downloads=True)
        page=desktop.new_page()
        errors=[]
        page.on("pageerror",lambda err:errors.append(str(err)))
        page.goto("http://127.0.0.1:8765/review/",wait_until="networkidle")
        assert page.locator("#screenTitle").inner_text()=="Home"
        assert page.locator("#options button").count()>=3
        assert page.locator(".frame").count()==1
        first=page.locator("#sourceHash").inner_text()
        page.locator("#options button").nth(1).click()
        assert page.locator("#sourceHash").inner_text()!=first
        page.keyboard.press("ArrowLeft")
        assert page.locator("#sourceHash").inner_text()==first
        page.locator("#compare").click()
        assert page.locator(".frame").count()==2
        page.locator("#compare").click()
        page.locator('[data-status="accepted"]').click()
        assert '"status":"accepted"' in page.evaluate("localStorage.getItem('glimpse.review.v1.gamesbyian.Glimpse')")
        page.locator("#startPin").click()
        frame=page.locator(".frame").first
        frame.click(position={"x":180,"y":225})
        assert page.locator("#pinComposer").is_visible()
        page.locator("#pinText").fill("Reduce subtitle density here.")
        page.locator("#savePin").click()
        assert page.locator("#commentCount").inner_text()=="1"
        page.screenshot(path=str(OUT/"review-desktop.png"),full_page=True)
        page.locator('[data-section="question"]').click()
        assert page.locator("#screenTitle").inner_text().startswith("D01")
        page.locator('[data-choice]').first.click()
        page.locator("#openDashboard").click()
        assert page.locator("#overviewDialog").is_visible()
        assert "1/8" in page.locator("#overviewBody").inner_text()
        page.locator("#closeOverview").click()
        with page.expect_download() as download:
            page.locator("#exportButton").click()
        with TemporaryDirectory() as tmp:
            target=Path(tmp)/"review.json"
            download.value.save_as(target)
            packet=json.loads(target.read_text())
            assert packet["format"]=="glimpse-design-review-v1"
            assert len(packet["selectedPrimary"])==24 and len(packet["selectedSupport"])==34
            assert packet["approval"]["repositoryOwnerApproved"] is False
            assert packet["approval"]["wholeSetVisualAcceptanceRequested"] is False
            assert packet["decisions"]["D01"]["choice"]
        assert not errors,errors
        desktop.close()

        mobile=browser.new_context(viewport={"width":393,"height":852},
            device_scale_factor=1,has_touch=True,is_mobile=True)
        p=mobile.new_page()
        mobile_errors=[]
        p.on("pageerror",lambda err:mobile_errors.append(str(err)))
        p.goto("http://127.0.0.1:8765/review/",wait_until="networkidle")
        assert p.locator("#screenTitle").inner_text()=="Home"
        frame=p.locator(".frame").first.bounding_box()
        assert frame is not None and frame["height"]>=310,frame
        assert frame["x"]>=-1 and frame["x"]+frame["width"]<=394,frame
        p.locator("#openBrowse").click()
        assert p.locator("#browse").is_visible()
        p.locator('[data-section="support"]').click()
        assert p.locator("#screenTitle").inner_text().startswith("About")
        p.locator("#openInspector").click()
        assert p.locator("#inspector").is_visible()
        p.screenshot(path=str(OUT/"review-mobile.png"),full_page=True)
        assert not mobile_errors,mobile_errors
        browser.close()
    print("PASS: Chromium desktop A/B, pin, local choice, eight decisions, JSON export; mobile browse, inspection, viewport, zero JS errors")
finally:
    server.terminate()
    try:server.wait(timeout=4)
    except subprocess.TimeoutExpired:server.kill()
