# :ai — la couche d'intégration de l'IA (pure JVM)

**Pigma ne connaît aucune IA.** Ce module ne contient que des types
de données et des interfaces : ni code réseau, ni fournisseur, ni
modèle, ni clé, ni écran de réglage. Il dit ce que L'HÔTE
implémente (`AiBackend`) et ce qui voyage (`AiRequest`, `AiResult`) ;
l'hôte choisit où partent les données et qui les paie.

- `AiBackend` — ce que l'hôte implémente : ses capacités (les tâches
  acceptées, le flux) et le démarrage d'une demande (poignée
  annulable, rappels de n'importe quel fil).
- `AiListener` / `AiCall` / `AiError` — les rappels (delta, issue
  unique : complète OU erreur), l'annulation, les familles de panne
  (indisponible, annulé, délai, refusé, interne).
- `AiTask` — expliquer, expliquer l'erreur, corriger, modifier la
  sélection, compléter en ligne, documenter, écrire le test.
- `AiRequest` — le contexte préparé : préfixe/suffixe bornés au
  BUDGET EN CARACTÈRES (`maxContextChars` — Pigma ignore les jetons
  d'un modèle), sélection, instruction, diagnostics, bloc englobant,
  extraits d'autres documents (fournis par un callback de l'hôte).
- `AiResult` — scellée à trois formes : le texte qui se lit (du
  Markdown, rien ne se pose), le remplacement d'une plage (un
  aperçu, une unité d'annulation), l'insertion à une position.

La pureté y vit d'un test ArchUnit (aucune dépendance à Android),
le contrat d'un test JVM (immuable, nuls vidés, bornes vérifiées).

La FAÇADE côté vue vit au module `:editor`
(`jo.pigma.editor.ai.AiAssistant`) : elle branche le moteur de
l'hôte à l'éditeur — les entrées à l'ampoule et à l'étage des
correctifs, le vol annulable, les réponses vérifiées. Ce module
demeure ce qu'il est : des types et des interfaces, rien d'Android.
