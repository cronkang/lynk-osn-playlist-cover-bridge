#!/usr/bin/env python3
"""Build the preserved 0.3.2 sources using only Python stdlib, JDK and Android SDK."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk', default=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT'))
    parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'))
    parser.add_argument('--build-tools', help='Explicit build-tools directory (default: SDK/build-tools/35.0.0)')
    parser.add_argument('--android-jar', help='Explicit android.jar (default: SDK/platforms/android-35/android.jar)')
    args = parser.parse_args()
    if not args.java_home:
        parser.error('Set JAVA_HOME to JDK 17 or supply --java-home.')
    if not args.sdk and not (args.build_tools and args.android_jar):
        parser.error('Set ANDROID_HOME or supply --sdk (or both --build-tools and --android-jar).')
    sdk = Path(args.sdk).resolve() if args.sdk else ROOT
    java = Path(args.java_home).resolve() / 'bin'
    bt = Path(args.build_tools).resolve() if args.build_tools else sdk / 'build-tools/35.0.0'
    android = Path(args.android_jar).resolve() if args.android_jar else sdk / 'platforms/android-35/android.jar'
    suffix = '.exe' if os.name == 'nt' else ''
    exe = lambda name: java / (name + suffix)
    for path in [exe('java'), exe('javac'), exe('jar'), exe('keytool'), bt / ('aapt2' + suffix),
                 bt / ('zipalign' + suffix), bt / 'lib/d8.jar', bt / 'lib/apksigner.jar', android]:
        if not path.is_file():
            parser.error('Required tool not found: ' + str(path))

    env = os.environ.copy()
    env['JAVA_HOME'] = str(java.parent)
    env['JAVA_TOOL_OPTIONS'] = '-Dfile.encoding=UTF-8 -Duser.language=en'

    def run(command):
        subprocess.run([str(x) for x in command], env=env, check=True, cwd=ROOT)

    # Check the public protocol snapshot without requiring any proprietary APK.
    protocol = json.loads((ROOT / 'protocol/contracts.json').read_text(encoding='utf8'))
    contract_java = (ROOT / 'src/dev/lynk/usbprobe/Contracts.java').read_text(encoding='utf8')
    lines = ['package dev.lynk.usbprobe;', 'final class Contracts {', 'static int code(String group,String name){']
    for group, entries in protocol['contracts'].items():
        lines.append('if(group.equals("' + group + '")){')
        for name, code in entries.items():
            lines.append('if(name.equals("' + name + '")) return ' + str(code) + ';')
        lines.append('}')
    lines.extend(['throw new IllegalArgumentException(group+"/"+name);', '}', 'static String name(String group,int code){'])
    for group, entries in protocol['contracts'].items():
        lines.append('if(group.equals("' + group + '")){switch(code){')
        for name, code in entries.items():
            lines.append('case ' + str(code) + ': return "' + name + '";')
        lines.append('}}')
    lines.extend(['return null;', '}', '}'])
    if contract_java.strip() != '\n'.join(lines).strip():
        raise RuntimeError('Contracts.java does not match protocol/contracts.json')
    assets = json.loads((ROOT / 'assets/SOURCES.json').read_text(encoding='utf8'))
    for asset in assets:
        data = (ROOT / 'assets' / asset['file']).read_bytes()
        if hashlib.sha256(data).hexdigest() != asset['sha256']:
            raise RuntimeError('Asset checksum mismatch: ' + asset['file'])

    build = ROOT / 'build'
    build.mkdir(exist_ok=True)
    for name in ['classes', 'dex']:
        directory = build / name
        # Resolve and constrain recursive deletion to this project's build tree.
        if directory.is_symlink() or directory.resolve().parent != build.resolve():
            raise RuntimeError('Unexpected build output path: ' + str(directory))
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir()
    run([exe('javac'), '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-classpath', android,
         '-d', build / 'classes', *sorted((ROOT / 'src').rglob('*.java'))])
    run([exe('jar'), 'cf', build / 'app.jar', '-C', build / 'classes', '.'])
    run([exe('java'), '-cp', bt / 'lib/d8.jar', 'com.android.tools.r8.D8', '--lib', android,
         '--min-api', '28', '--output', build / 'dex', build / 'app.jar'])
    run([bt / ('aapt2' + suffix), 'link', '-I', android, '--manifest', ROOT / 'AndroidManifest.xml',
         '--min-sdk-version', '28', '--target-sdk-version', '28', '-o', build / 'resources.apk'])
    shutil.copyfile(build / 'resources.apk', build / 'unsigned.apk')
    with zipfile.ZipFile(build / 'unsigned.apk', 'a', zipfile.ZIP_DEFLATED) as apk:
        apk.write(build / 'dex/classes.dex', 'classes.dex')
        for asset in sorted((ROOT / 'assets').iterdir()):
            if asset.is_file():
                apk.write(asset, 'assets/' + asset.name)
    run([bt / ('zipalign' + suffix), '-f', '-p', '4', build / 'unsigned.apk', build / 'aligned.apk'])
    # This is a disposable local debug key, never the original release signing key.
    key = build / 'local-debug.keystore'
    if not key.exists():
        run([exe('keytool'), '-genkeypair', '-keystore', key, '-storepass', 'android', '-keypass', 'android',
             '-alias', 'androiddebugkey', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '3650',
             '-dname', 'CN=USB Probe Local Debug'])
    dist = ROOT / 'dist'
    dist.mkdir(exist_ok=True)
    output = dist / 'USB-Instrument-Probe-0.3.2-local.apk'
    run([exe('java'), '-jar', bt / 'lib/apksigner.jar', 'sign', '--ks', key, '--ks-key-alias',
         'androiddebugkey', '--ks-pass', 'pass:android', '--out', output, build / 'aligned.apk'])
    run([exe('java'), '-jar', bt / 'lib/apksigner.jar', 'verify', '--verbose', '--min-sdk-version',
         '28', '--max-sdk-version', '28', output])
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    (dist / 'SHA256SUMS.txt').write_text(digest + '  ' + output.name + '\n', encoding='ascii')
    print('Built:', output)
    print('SHA-256:', digest)
    print('Local signing key differs from the original release; cannot overwrite the original install.')


if __name__ == '__main__':
    try:
        main()
    except (OSError, RuntimeError, subprocess.CalledProcessError) as exc:
        print('Build failed:', exc, file=sys.stderr)
        sys.exit(1)
