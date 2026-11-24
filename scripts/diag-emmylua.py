#!/usr/bin/env python3
"""Diagnostic : parler LSP à emmylua_ls à la main, sans lsp4j.

Vérifie ce que le serveur répond à didChange (delta puis texte entier)
et au survol qui suit — pour savoir si notre delta est en cause.
"""
import json
import subprocess
import sys
import threading
import time
import queue

SERVER = "/home/z/my-project/tools/vendor/emmylua/emmylua_ls"

DEMO = (
    "-- démo du harnais : le serveur réel analyse ce texte.\n"
    "local caisse = {}\n"
    "\n"
    "function caisse.total(graine, taille)\n"
    "  local calcule = graine * taille\n"
    "  return calcule\n"
    "end\n"
    "\n"
    "local x = caisse.total(12, 24)\n"
    "local somme = ca\n"
)

proc = subprocess.Popen([SERVER, "--resources-path", "/tmp/emmy-diag-res"],
                        stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                        stderr=open("/tmp/emmy-diag-stderr.log", "wb"))
q = queue.Queue()

def reader():
    length = None
    buffer = b""
    while True:
        chunk = proc.stdout.read(1)
        if not chunk:
            q.put(("eof", None))
            return
        buffer += chunk
        if buffer.endswith(b"\r\n\r\n") and length is None:
            header = buffer.decode()
            length = int([l for l in header.split("\r\n")
                          if l.lower().startswith("content-length")][0].split(":")[1])
            buffer = b""
            continue
        if length is not None and len(buffer) == length:
            q.put(("message", json.loads(buffer.decode())))
            length = None
            buffer = b""

threading.Thread(target=reader, daemon=True).start()

def send(method, params, ident=None):
    message = {"jsonrpc": "2.0", "method": method, "params": params}
    if ident is not None:
        message["id"] = ident
    raw = json.dumps(message).encode()
    proc.stdin.write(f"Content-Length: {len(raw)}\r\n\r\n".encode() + raw)
    proc.stdin.flush()

def recv_until(predicate, timeout=20.0):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            kind, message = q.get(timeout=deadline - time.time())
        except queue.Empty:
            return None
        if kind == "eof":
            print("EOF du serveur", file=sys.stderr)
            return None
        if predicate(message):
            return message
    return None

URI = "file:///tmp/emmy-diag.lua"
open("/tmp/emmy-diag.lua", "w").write(DEMO)

send("initialize", {"clientInfo": {"name": "diag"}, "capabilities": {}}, ident=0)
init = recv_until(lambda m: m.get("id") == 0)
print("initialize ->", json.dumps(init.get("result", {}).get("capabilities", {}))[:120])
send("initialized", {})
send("textDocument/didOpen", {"textDocument": {"uri": URI, "languageId": "lua",
    "version": 1, "text": DEMO}})
time.sleep(1.0)
send("textDocument/hover", {"textDocument": {"uri": URI}, "position": {"line": 8, "character": 10}}, ident=1)
hover = recv_until(lambda m: m.get("id") == 1)
print("hover avant changement ->", json.dumps(hover.get("result"))[:150] if hover else None)

# didChange par DELTA : la plage de « {} » remplacée par « { seuil = 1 } »
# dans les coordonnées de l'ANCIEN texte : ligne 1, caractères 15-17.
CHANGED = DEMO.replace("local caisse = {}", "local caisse = { seuil = 1 }")
send("textDocument/didChange", {
    "textDocument": {"uri": URI, "version": 2},
    "contentChanges": [{
        "range": {"start": {"line": 1, "character": 15}, "end": {"line": 1, "character": 17}},
        "text": "{ seuil = 1 }"}]})
time.sleep(1.0)
send("textDocument/hover", {"textDocument": {"uri": URI}, "position": {"line": 8, "character": 10}}, ident=2)
hover2 = recv_until(lambda m: m.get("id") == 2)
print("hover apres delta ->", json.dumps(hover2.get("result"))[:150] if hover2 else None)

# le texte ENTIER pour comparer
CHANGED2 = DEMO.replace("local caisse = {}", "local caisse = { seuil = 1 }")
send("textDocument/didChange", {
    "textDocument": {"uri": URI, "version": 3},
    "contentChanges": [{"text": CHANGED2}]})
time.sleep(1.0)
send("textDocument/hover", {"textDocument": {"uri": URI}, "position": {"line": 8, "character": 10}}, ident=3)
hover3 = recv_until(lambda m: m.get("id") == 3)
print("hover apres texte entier ->", json.dumps(hover3.get("result"))[:150] if hover3 else None)

proc.terminate()
