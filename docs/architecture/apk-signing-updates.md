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
