# Pages deployment trust boundary (2026-10-31)

Status: candidate security correction for issue #160. Do not claim fully verified until exact-head contract CI and a trusted-main live deployment/probe are green.

The old workflow executed Python and Node loaded from an unmerged same-repository draft inside the job with Pages write and OIDC grants. Existing repository/branch checks already excluded forks; no exploit has been demonstrated. The change removes unnecessary privileged candidate-code execution.

The new read-only stage job still checks out the exact successful upstream draft SHA, validates the existing source/review files and uploads a GitHub Pages artifact. It refuses symlinked design/review sources. A separate publish job, depending on stage success, receives the protected github-pages environment and Pages/OIDC grants; it does not check out or run code from the draft. GitHub deploy-pages consumes the immutable artifact from the same workflow run, not a URL or artifact supplied by an unrelated run. The existing live HTTP probe compares the staged SHA with the served build.json.

The workflow_run trigger now requires a successful approved upstream workflow name, same repository name and numeric ID, and the exact fixed draft branch; push/manual entry is main only. The existing singleton cancellation retains most recent publication intent. The branch is still an unapproved design proposal.

Pages actions are pinned to official immutable commit SHAs. The broader Actions tag-to-SHA pinning and update procedure remain separate unfinished work under issue #160; do not imply other Android workflows are pinned. The new offline CI test checks privileged isolation and exact source identity. GitHub environment protection and actual deploy success cannot be proven by a YAML test; verify them independently before closing the issue.
