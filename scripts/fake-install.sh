#!/bin/bash
# A stand-in for /usr/sbin/amadeos-install to try the wizard without root and without a disk:
#
#   AMADEOS_INSTALL_COMMAND="$PWD/scripts/fake-install.sh" ./gradlew run
#
# It prints the progress lines the real script prints, slowly, and writes nothing. With
# FAKE_INSTALL_FAIL=<step id> it fails at that step, e.g. FAKE_INSTALL_FAIL=bootloader.
set -u
echo "fake-install: $*"
prev=""
for arg in "$@"; do
    case "$prev" in --config) echo "--- config ---"; sed 's/^PASSWORD=.*/PASSWORD=***/' "$arg"; echo "--------------" ;; esac
    prev="$arg"
done
steps=(partition format mount copy live configure user initramfs bootloader finish)
n=0
for id in "${steps[@]}"; do
    n=$((n + 1))
    printf '@@step %d/%d %s\n' "$n" "${#steps[@]}" "$id"
    echo "== $id"
    if [ "$id" = copy ]; then
        for p in 5 20 40 60 80 100; do printf '@@percent %d\n' "$p"; sleep 0.3; done
    else
        sleep 0.5
    fi
    if [ "${FAKE_INSTALL_FAIL:-}" = "$id" ]; then
        echo "something went wrong in $id"
        printf '@@error The installation failed (step %s); see /var/log/amadeos-install.log\n' "$id"
        exit 2
    fi
done
echo "amadeOS is installed on /dev/vda3 (/dev/vda). Remove the live medium and restart."
printf '@@done\n'
