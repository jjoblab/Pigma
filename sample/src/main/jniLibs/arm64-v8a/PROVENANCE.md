# Provenance du serveur natif Lua

`libemmylua_ls.so` (arm64-v8a) : l'exécutable du serveur de langage
EmmyLua, extraite de sa distribution officielle. Le binaire x86_64
n'est plus embarqué (il ne servait qu'à l'émulateur — l'APK filtre
sur arm64-v8a, le dépôt s'allège de 7,9 Mo) : l'émulateur qui veut
le serveur Lua y copie le sien dans
`sample/src/main/jniLibs/x86_64/`.

L'APK le pose dans `nativeLibraryDir` (seul répertoire exécutable
d'une application, `useLegacyPackaging` l'y extrait) — le bouton
« LSP Lua » de l'écran de démonstration y lance le processus.
