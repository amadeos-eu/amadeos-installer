#!/bin/bash
# Makes a disk image that looks like a laptop with Windows and an older Linux, to try the installer's
# "existing partition" mode in QEMU without touching a real machine:
#
#   GPT: EFI System Partition (300 MB, FAT32, with the files os-prober looks for: EFI/Microsoft/Boot/BCD
#        and bootmgfw.efi, and Windows' fallback loader EFI/BOOT/BOOTX64.EFI; all of them dummies),
#        "Basic data partition" (4 GB, NTFS, label Windows), "old linux" (the rest, ext4).
#
# No root needed (mtools, ntfs-3g, e2fsprogs, util-linux, qemu-utils). Then, in the amadeos repository:
#
#   ./run-qemu.sh --install-disk /path/to/dualboot.qcow2 --uefi
#   ./run-qemu.sh --boot-disk /path/to/dualboot.qcow2 --uefi      (after the installation)
#
# In the wizard choose "Install into an existing partition" and the ext4 partition. The GRUB menu of
# the installed system should then offer "Windows Boot Manager" as well (its entry only points at dummy
# files here, so choosing it fails, which is expected).
#
# Usage: make-dualboot-test-disk.sh OUTPUT.qcow2 [SIZE_GB (default 24)]
set -euo pipefail

OUT="${1:?usage: make-dualboot-test-disk.sh OUTPUT.qcow2 [SIZE_GB]}"
SIZE_GB="${2:-24}"
for tool in sfdisk mformat mmd mcopy mkfs.ntfs mkfs.ext4 qemu-img python3; do
    command -v "$tool" >/dev/null 2>&1 || { echo "$tool is missing" >&2; exit 1; }
done

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
RAW="$WORK/disk.raw"
truncate -s "${SIZE_GB}G" "$RAW"

sfdisk --quiet "$RAW" <<SF
label: gpt
size=300M, type=C12A7328-F81F-11D2-BA4B-00A0C93EC93B, name="EFI system partition"
size=4G, type=EBD0A0A2-B9E5-4433-87C0-68B6B72699C7, name="Basic data partition"
type=0FC63DAF-8483-4772-8E79-3D69D8477DE4, name="old linux"
SF

# Start and size of each partition in 512-byte sectors.
read -r ESP_START ESP_SECTORS WIN_START WIN_SECTORS LIN_START LIN_SECTORS < <(sfdisk --json "$RAW" | python3 -c '
import json, sys
parts = json.load(sys.stdin)["partitiontable"]["partitions"]
print(" ".join(str(p["start"]) + " " + str(p["size"]) for p in parts))')

# EFI System Partition: FAT32 with the Microsoft files os-prober checks for, and the fallback loader.
mformat -i "$RAW@@$((ESP_START * 512))" -T "$ESP_SECTORS" -F -v EFI ::
mmd -i "$RAW@@$((ESP_START * 512))" ::/EFI ::/EFI/Microsoft ::/EFI/Microsoft/Boot ::/EFI/BOOT
printf 'dummy Windows boot manager (test disk)\n' > "$WORK/bootmgfw.efi"
printf 'dummy BCD store (test disk)\n' > "$WORK/BCD"
mcopy -i "$RAW@@$((ESP_START * 512))" "$WORK/bootmgfw.efi" ::/EFI/Microsoft/Boot/bootmgfw.efi
mcopy -i "$RAW@@$((ESP_START * 512))" "$WORK/BCD" ::/EFI/Microsoft/Boot/BCD
mcopy -i "$RAW@@$((ESP_START * 512))" "$WORK/bootmgfw.efi" ::/EFI/BOOT/BOOTX64.EFI

# Windows partition: NTFS made in a file of its own and written in place (mkfs.ntfs has no offset option).
truncate -s "$((WIN_SECTORS * 512))" "$WORK/windows.part"
mkfs.ntfs --quick --force --label Windows "$WORK/windows.part" >/dev/null 2>&1
dd if="$WORK/windows.part" of="$RAW" bs=1M seek="$((WIN_START * 512 / 1048576))" conv=notrunc,sparse status=none
rm -f "$WORK/windows.part"

# The older Linux partition amadeOS will be installed into.
mkfs.ext4 -q -F -L oldlinux -E "offset=$((LIN_START * 512))" "$RAW" "${LIN_SECTORS}s"

qemu-img convert -f raw -O qcow2 "$RAW" "$OUT"
echo "Created $OUT:"
sfdisk --list "$RAW" | sed -n '/^Device/,$p'
