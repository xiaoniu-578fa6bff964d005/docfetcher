#!/bin/sh

die () {
    echo >&2 "$@"
    exit 1
}

[ "$#" -eq 1 ] || die "No SourceForge.net user name specified."

./build-website.py

rsync -avP -e ssh dist/website/ $1,DocFetcher@web.sourceforge.net:/home/project-web/docfetcher/htdocs
