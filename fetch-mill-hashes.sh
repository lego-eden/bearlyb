#!/usr/bin/env bash

VERSION=$1

if [ -z "$VERSION" ]; then
    echo "error: missing version" 1>&2
    exit 1
fi

system=("aarch64-darwin" "x86_64-darwin" "aarch64-linux" "x86_64-linux")
artifactsuffix=("native-mac-aarch64" "native-mac-amd64" "native-linux-aarch64" "native-linux-amd64")

echo {

for n in $(seq 0 3);
do
    SUFFIX=${artifactsuffix[$n]}
    echo "  \"${system[$n]}\": {"
    HASH=$(nix hash convert --hash-algo sha256 $(nix-prefetch-url --type sha256 https://repo1.maven.org/maven2/com/lihaoyi/mill-dist-$SUFFIX/$VERSION/mill-dist-$SUFFIX-$VERSION.exe 2> /dev/null))
    echo "    \"artifact-suffix\": \"${artifactsuffix[$n]}\","
    echo "    \"hash\": \"$HASH\""
    echo -n "  }"
    if [ $n -lt 3 ]; then
        echo ,
    else
        echo
    fi
done

echo }
