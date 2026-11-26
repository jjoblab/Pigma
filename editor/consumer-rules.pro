# Règles consommateur de Pigma : un hôte qui minifie sa release
# (R8/ProGuard) garde le client LSP vivant — lsp4j et Gson chargent
# leurs protocoles par RÉFLEXION (les classes de messages et les
# paramètres y vivent par nom), la coupure des noms casse la
# sérialisation JSON-RPC à l'exécution seulement.
-keep class org.eclipse.lsp4j.** { *; }
-keep class org.eclipse.lsp4j.jsonrpc.** { *; }
-keep class com.google.gson.** { *; }
# les entrées du protocole que la façade de Pigma sérialise
-keep class jo.pigma.lsp.** { *; }
-keepclassmembers class jo.pigma.lsp.** { *; }
# Gson instancie par réflexion les records du protocole
-keep class jo.pigma.editor.lsp.** { *; }
-dontwarn org.eclipse.lsp4j.**
-dontwarn com.google.gson.**
