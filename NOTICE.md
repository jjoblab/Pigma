# Notices de tiers

Le dépôt Pigma ne dépend que de sa propre base (le SDK Android pour
l'application de démonstration) — les tiers y sont nommés ci-dessous.

## org.eclipse.lsp4j 1.0.0

- Artéfacts : `org.eclipse.lsp4j:org.eclipse.lsp4j` et
  `org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc`
- Origine : Eclipse LSP4J (https://projects.eclipse.org/projects/technology.lsp4j)
- Licence : Eclipse Public License 2.0 (avec licence secondaire GPL-2.0
  avec exception classpath) — le texte fait foi dans le dépôt du
  projet, `org.eclipse.lsp4j` embarque sa copie
- Rôle : le protocole LSP et le JSON-RPC du module `:lsp` (le client
  ne réécrit plus le protocole, chantier 26)

## com.google.code.gson (résolu en 2.15.0)

- Artéfact : `com.google.code.gson:gson` — dépendance de transitivité
  de lsp4j (sa version y est un intervalle, Gradle le résout)
- Origine : Gson (https://github.com/google/gson)
- Licence : Apache License 2.0
- Rôle : la sérialisation JSON sous lsp4j

## EmmyLua Language Server (binaire embarqué, 0.25.1)

- Artéfacts : `sample/src/main/jniLibs/arm64-v8a/libemmylua_ls.so`
  et `sample/src/main/jniLibs/x86_64/libemmylua_ls.so` — l'exécutable
  du serveur de langage Lua embarqué dans l'application de
  démonstration (le bouton « LSP Lua » le lance hors processus), sa
  bibliothèque standard Lua s'extrait du dépôt au premier lancement
- Origine : EmmyLua Analyzer — le binaire distribué correspond à la
  suite Rust du projet (https://github.com/EmmyLuaLs/emmylua-analyzer-rust,
  l'analyseur actif ; l'ancien dépôt Java/Kotlin
  https://github.com/EmmyLua/EmmyLua-LanguageServer y expose son
  successeur)
- Licence : MIT — vérifiée au dépôt de l'analyseur le 25 novembre
  2026 (le fichier LICENSE y est MIT) ; la redistribution est
  permise, la notice de copyright doit accompagner le binaire
- Empreintes (SHA-256 du binaire embarqué) :
  - arm64-v8a :
    `85970478feb0867f6070d5ab9ff3f4c8686a3389ce6681a640b9d7a6cd110b8b`
    (6 544 168 octets)
  - x86_64 :
    `bb7b2cd7074d7519f0282c2f4e0d1e135996fa5905215989a86cfdd0ae4a02de`
    (7 878 088 octets)
- Rôle : le serveur de langage Lua réel de la démonstration (chantier
  des serveurs embarqués) — la licence MIT du binaire s'acquitte par
  la présente notice
