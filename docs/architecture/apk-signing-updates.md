> **Latest observed mismatch (2026-10-31/2026-11-01 UTC):** Three
> successful distinct `main` Android CI builds still use *different* public
> signer certificates: [run 41249622784](https://github.com/gamesbyian/Glimpse/actions/runs/41249622784)
> `6cadb7f2db006fc9ac33b0ca53f3547ec6880b91ba15f65a15585bf9126c84f9`;
> [run 41250242171](https://github.com/gamesbyian/Glimpse/actions/runs/41250242171)
> `4b5ac005254a8d45b4e40207c038db0bcc8ac3683501df0561bedcdd7ca0a832`;
> [run 41265420324](https://github.com/gamesbyian/Glimpse/actions/runs/41265420324)
> `1cc8d03391d1c7de8c329610207641949550eb582999bb2080cf5759070fc93e`.
> The `Configure optional stable CI debug signing` script executes successfully,
> but logs explicitly warn that persistent signing is unavailable. The green
> step is **not** evidence of a configured owner-controlled key. Issue #72
> was reopened; exact installed-update continuity remains unproven. These
> values are public `apksigner` fingerprints, not secret key material.

> **P0 acceptance reaffirmed (2026-10-29, GL-AUD-01):** Signed debug artifacts from different runners were empirically incompatible. A configured optional key is **not** proof of upgrade safety. Release remains blocked until two consecutive protected main builds have the identical public signing fingerprint and `adb install -r` preserves populated Room and immutable originals without uninstall. Owner must separately provision and escrow secrets; neither agent nor CI should disclose private signing bytes. See [audit](../quality/ARCHITECTURE-AUDIT-2026-10-29.md) and [#72](https://github.com/gamesbyian/Glimpse/issues/72).

# Glimpse Android signing and update continuity

**Observed 27 October 2026:** Two successful debug APK artifacts from separate
GitHub Actions runs were inspected through the APK Signing Block. Their signer
certificate SHA-256 fingerprints differed. The GHA-default debug keystore is
ephemeral; an apparently newer build may fail Android's signature-matching
update check. No artifact is a signed release.

The native CI workflow now accepts optional, encrypted repository secrets
for a persistent PKCS#12 debug signing key. The private key is decoded only
on main-branch push or manual dispatch, never from PR code, into an ephemeral
runner; it is not uploaded. Missing *all* four secrets
leaves explicit per-run debug signing. Missing *some* aborts rather than
mislabeling the output. Each APK artifact includes a public signing
fingerprint for independent comparison.

**Human-controlled trust boundary:** No stable signing key has been provided
to or configured by this development agent. A persistent certificate must be
generated, escrowed, rotated and installed by the authorized repository owner
through GitHub Actions Secrets, or an approved future distribution channel.
Never put it into a public branch or upload it through a bug report. Treat a
changed fingerprint as an incompatible install, **never** advise uninstalling
without a independently verified complete backup/restore. Current ZIP export
does not include app settings or wallpaper/shuffle state.

Acceptance: two successive main CI APKs with configured secrets must have
identical certificate fingerprints; an Android emulator must install the
second *over* the first without deleting Room collections and original bytes.
Only then mark upgrade continuity accepted. PR debug artifacts lacking secrets
are not acceptance candidates.
