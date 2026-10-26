#!/usr/bin/env python3
"""Check resolved Maven coordinates against public OSV advisories.

Only public package names and versions leave the machine. No source, environment
variables, credentials, cookies or application data are submitted.
"""
import json
import pathlib
import subprocess
import sys
import urllib.request

root = pathlib.Path(__file__).resolve().parents[1]
launcher = "mvnw.cmd" if sys.platform == "win32" else "./mvnw"
result = subprocess.run(
    [launcher, "-B", "-ntp", "dependency:list", "-DoutputFile=target/dependencies.txt"],
    cwd=root, check=True, capture_output=True, text=True,
)
queries = []
for line in (root / "target/dependencies.txt").read_text().splitlines():
    parts = line.strip().split()
    if not parts:
        continue
    coordinate = parts[0].split(":")
    if len(coordinate) != 5 or coordinate[2] != "jar":
        continue
    group, artifact, _, version, scope = coordinate
    if scope in {"compile", "runtime", "test"}:
        queries.append({"package": {"ecosystem": "Maven", "name": group + ":" + artifact}, "version": version})
if not queries:
    raise SystemExit("No dependency coordinates were resolved; audit did not run.")
request = urllib.request.Request(
    "https://api.osv.dev/v1/querybatch", data=json.dumps({"queries": queries}).encode(),
    headers={"Content-Type": "application/json"}, method="POST",
)
with urllib.request.urlopen(request, timeout=45) as response:
    results = json.load(response)["results"]
if len(results) != len(queries):
    raise SystemExit("Incomplete advisory response; audit did not complete.")
findings = []
for query, result in zip(queries, results):
    for vulnerability in result.get("vulns", []):
        findings.append({"package": query["package"]["name"], "version": query["version"], "advisory": vulnerability["id"]})
(root / "target/dependency-audit.json").write_text(json.dumps({"checked": len(queries), "findings": findings}, indent=2))
print("Checked", len(queries), "Maven dependencies.")
for finding in findings:
    print(finding["package"], finding["version"], finding["advisory"])
sys.exit(1 if findings else 0)
