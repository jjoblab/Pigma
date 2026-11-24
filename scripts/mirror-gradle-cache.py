#!/usr/bin/env python3
"""Miroir local du cache Gradle : Maven Central limite le réseau (429
au moindre HEAD). Le cache files-2.1 a déjà tout — il se reflète en
dépôt Maven lisible par Gradle : groupe/artefact/version/fichier, le
niveau sha1 en moins. Le script d'init (tools/gradle-home/init.gradle)
met ce miroir en tête des dépôts : le réseau ne sert plus qu'aux
absents du cache."""
from pathlib import Path
import shutil

CACHE = Path("/home/z/my-project/tools/gradle-home/caches/modules-2/files-2.1")
MIRROR = Path("/home/z/my-project/tools/local-m2")

count = 0
for pom_or_jar in CACHE.glob("*/*/*/*/*"):
    if pom_or_jar.is_dir():
        continue
    # files-2.1 : groupe/artefact/version/sha1/fichier — le groupe
    # s'éclate par points (org.jetbrains.kotlin → org/jetbrains/kotlin),
    # c'est la convention d'arborescence d'un dépôt Maven
    version_dir = pom_or_jar.parent.parent
    artifact_dir = version_dir.parent
    group_dir = artifact_dir.parent
    group_path = Path(*group_dir.name.split("."))
    target = MIRROR / group_path / artifact_dir.name / version_dir.name / pom_or_jar.name
    if target.exists():
        continue
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(pom_or_jar, target)
    count += 1

print(f"miroir : {count} fichiers copiés vers {MIRROR}")
