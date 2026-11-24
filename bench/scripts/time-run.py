#!/usr/bin/env python3
"""Lance une commande et mesure son temps elapsed et sa memoire max.

Le bac a sable n'a pas /usr/bin/time : ce lanceur ecrit la mesure
(temps elapsed depuis lancement, ru_maxrss des enfants) sur la sortie
d'erreur au format « MESUREGEN wall X s maxrss Y Mo » ; la sortie et
l'erreur de la commande traversent telles quelles.
"""

import resource
import subprocess
import sys
import time


def main():
    if len(sys.argv) < 2:
        print("usage : time-run.py commande [args...]", file=sys.stderr)
        return 2
    start = time.monotonic()
    proc = subprocess.run(sys.argv[1:], check=False)
    elapsed = time.monotonic() - start
    peak = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss / 1024.0
    print(f"MESUREGEN wall {elapsed:.2f} s maxrss {peak:.0f} Mo code {proc.returncode}",
          file=sys.stderr)
    return proc.returncode


if __name__ == "__main__":
    sys.exit(main())
