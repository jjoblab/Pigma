# Notices de tiers

Le dépôt Pigma ne dépend que de sa propre base (le SDK Android pour
l'application de démonstration) — à une exception près, décidée par le
propriétaire au chantier 26 : le client de serveur de langage.

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
