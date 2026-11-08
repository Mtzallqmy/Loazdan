"""Publish the already signed APKs using the exact bytes recorded for Atlas."""
import hashlib
import json
import os
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
TAG = "v2.0.0"
INPUTS = {
    "com.lughzdan.app": ("atlas-original/app-release-unsigned.apk", "atlas-signature.patch"),
    "com.lughzdan.app.preview.diwan": ("atlas-diwan/Lughzdan-2.0.0-Preview-ARM64.apk", "diwan-signature.patch"),
}


def output(*args):
    return subprocess.check_output(args, text=True).strip()


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def prepare_files(entries, work, build_tools):
    assert {entry["package"] for entry in entries} == set(INPUTS) and len(entries) == 2
    dist = work / "atlas-release"
    dist.mkdir(exist_ok=True)
    files = []
    for entry in entries:
        archive, patch = INPUTS[entry["package"]]
        original = work / archive
        with tempfile.TemporaryDirectory(dir=work) as directory:
            signed = Path(directory) / "target.apk"
            shutil.copyfile(original, signed)
            subprocess.run(["git", "apply", "--no-index", "--binary", str(HERE / patch)], cwd=directory, check=True)
            assert signed.stat().st_size == entry["bytes"] and sha256(signed) == entry["sha256"]
            with zipfile.ZipFile(original) as before, zipfile.ZipFile(signed) as after:
                assert before.namelist() == after.namelist()
                assert all(before.read(name) == after.read(name) for name in before.namelist())
            certificate = output(build_tools / "apksigner", "verify", "--verbose", "--print-certs", signed)
            assert "Verified using v2 scheme (APK Signature Scheme v2): true" in certificate
            assert "Verified using v3 scheme (APK Signature Scheme v3): true" in certificate
            assert "Signer #1 certificate SHA-256 digest: " + entry["certificateSha256"] in certificate
            badging = output(build_tools / "aapt", "dump", "badging", signed)
            package = f"package: name='{entry['package']}' versionCode='{entry['versionCode']}' versionName='{entry['versionName']}'"
            assert package in badging and f"sdkVersion:'{entry['minSdk']}'" in badging and "application-debuggable" not in badging
            subprocess.run(["python3", str(ROOT / "scripts/verify_release_apk.py"), signed], check=True)
            destination = dist / entry["filename"]
            shutil.copyfile(signed, destination)
            files.append(destination)
    sums = dist / "SHA256SUMS"
    sums.write_text("".join(f"{sha256(file)}  {file.name}\n" for file in sorted(files)), encoding="ascii")
    return [*files, sums]


def publish(files, repo, source):
    api = f"repos/{repo}"
    refs = json.loads(output("gh", "api", f"{api}/git/matching-refs/tags/{TAG}"))
    if any(ref["ref"] == f"refs/tags/{TAG}" for ref in refs):
        assert output("gh", "api", f"{api}/commits/{TAG}", "--jq", ".sha") == source

    view = ["gh", "release", "view", TAG, "--json", "databaseId,isDraft,targetCommitish,url"]
    existing = subprocess.run(view, capture_output=True, text=True)
    if existing.returncode:
        subprocess.run(["gh", "release", "create", TAG, "--draft", "--target", source, "--title",
                        "لُغزدان 2.0.0 — أطلس المعرفة العربية", "--notes-file", str(HERE / "NOTES.md")], check=True)
    release = json.loads(output(*view))
    if release["isDraft"]:
        assert release["targetCommitish"] == source
    assets_url = f"{api}/releases/{release['databaseId']}/assets"

    def remote_digests():
        return {asset["name"]: asset["digest"] for asset in json.loads(output("gh", "api", assets_url))}

    remote = remote_digests()
    for file in files:
        if file.name in remote:
            assert remote[file.name] == "sha256:" + sha256(file), file.name
        else:
            subprocess.run(["gh", "release", "upload", TAG, str(file)], check=True)
    final = remote_digests()
    assert all(final.get(file.name) == "sha256:" + sha256(file) for file in files)
    if release["isDraft"]:
        subprocess.run(["gh", "release", "edit", TAG, "--draft=false", "--latest"], check=True)
    assert output("gh", "api", f"{api}/commits/{TAG}", "--jq", ".sha") == source
    print(release["url"])


if __name__ == "__main__":
    release_record = json.loads((ROOT / "docs/atlas/RELEASE.json").read_text())
    packages = prepare_files(release_record["artifacts"], Path(os.environ["RUNNER_TEMP"]),
                             Path(os.environ["ANDROID_HOME"]) / "build-tools/36.0.0")
    publish(packages, os.environ["GH_REPO"], os.environ["SOURCE_COMMIT"])
