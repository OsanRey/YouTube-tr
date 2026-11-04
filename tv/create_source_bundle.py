"""Package exact upstream trees plus committed overlay, without build/account files."""
from pathlib import Path, PurePosixPath
import json
import io
import subprocess
import tarfile

root = Path(__file__).resolve().parent.parent
pins = json.loads((root / 'tv/upstream.json').read_text(encoding='utf-8'))
staging = root / '.release-staging/tv-source-archives'
staging.mkdir(parents=True, exist_ok=True)
output = root / 'output/YouTube-tr-TV-32.69-tr1-source.tar.gz'
trees = [
    (root, 'HEAD', ''),
    (root / 'sources/SmartTube', pins['SmartTube']['commit'], 'sources/SmartTube/'),
    (root / 'sources/SmartTube/SharedModules', pins['SharedModules']['commit'], 'sources/SmartTube/SharedModules/'),
    (root / 'sources/SmartTube/MediaServiceCore', pins['MediaServiceCore']['commit'], 'sources/SmartTube/MediaServiceCore/'),
    (root / 'sources/SmartTube/MediaServiceCore/SharedModules', pins['SharedModules']['commit'], 'sources/SmartTube/MediaServiceCore/SharedModules/'),
    (root / 'sources/anddea-patches', pins['anddea']['commit'], 'sources/anddea-patches/'),
]
with tarfile.open(output, 'w:gz', compresslevel=6) as combined:
    for i, (repo, revision, prefix) in enumerate(trees):
        archive = staging / f'{i}.tar'
        subprocess.run(['git', '-C', str(repo), 'archive', '--format=tar',
                        '--prefix=YouTube-tr-TV-source/' + prefix,
                        '--output=' + str(archive), revision], check=True)
        with tarfile.open(archive) as source:
            for member in source:
                if '..' in PurePosixPath(member.name).parts or member.name.startswith('/'):
                    raise ValueError('Unsafe archive path')
                combined.addfile(member, source.extractfile(member) if member.isfile() else None)
        if prefix:
            content = (revision + '\n').encode('ascii')
            marker = tarfile.TarInfo('YouTube-tr-TV-source/' + prefix + '.youtube-tr-upstream')
            marker.size = len(content)
            marker.mode = 0o644
            combined.addfile(marker, io.BytesIO(content))
print(output)
