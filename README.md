# amadeos-installer

**amadeos-installer** installs the [amadeOS](https://github.com/datazuul/amadeos) live system onto a
hard disk: a step-by-step assistant in the CDE style, and the script `amadeos-install` that does the
work as root.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)
![Debian](https://img.shields.io/badge/Debian-package-red)

> Status: early development (0.1.0), not yet tested on a real machine. Whole-disk and single-partition
> installations, UEFI and BIOS. No encryption, no separate home partition, no dual-boot assistance yet.

---

## What it does

The live image is one squashfs file on the boot medium. Installing it means:

1. **Partition** the target disk (GPT: a 1 MB BIOS boot partition, a 512 MB EFI System Partition, an
   optional swap partition and the amadeOS root partition), or format an existing partition.
2. **Copy** the squashfs onto the root partition with `unsquashfs`.
3. **Remove the live parts**: the packages `live-boot`, `live-config` and `amadeos-installer` itself,
   `/etc/live`, the live user's password hook and the live drop-in of the display manager.
4. **Write the settings**: `/etc/fstab` by UUID, host name, `/etc/default/locale`,
   `/etc/default/keyboard`, `/etc/localtime` and `/etc/timezone`, a fresh machine id, the first user
   with the groups of the live user, a locked root account. Network connections of the live session
   (WLAN passwords included) are carried over.
5. **Build the initramfs** without live-boot and **install GRUB**: `grub-install` for UEFI (with the
   signed shim when `shim-signed` and `grub-efi-amd64-signed` are in the image, so Secure Boot works)
   or for BIOS, plus `update-grub`.

Nothing is downloaded; everything the installed system needs is in the image.

## The assistant

```
┌ Install amadeOS ──────────────────────────────────────────────────────────┐
│ ▶ Welcome        │ Welcome                                                │
│   Keyboard       │                                                        │
│   Time Zone      │ Welcome to amadeOS. This assistant installs the live   │
│   Target Disk    │ system you are running onto a disk of this computer... │
│   User           │                                                        │
│   Summary        │ Language:  [Deutsch            ▼]                      │
│   Installation   │                                                        │
│   Finished       │                                                        │
├──────────────────┴────────────────────────────────────────────────────────┤
│                                          [< Back] [Next >]       [Cancel] │
└───────────────────────────────────────────────────────────────────────────┘
```

- **Welcome**: the language of the installed system; the assistant switches to it at once (English,
  Deutsch, Français, Español, Italiano).
- **Keyboard**: the XKB layouts of `evdev.lst`, applied to the live session so that they can be tried
  in a text field; an optional variant such as `nodeadkeys`.
- **Time Zone**: the zones of `zone1970.tab` with a filter field, preselected with the zone chosen at
  login.
- **Target Disk**: erase a whole disk (with a swap partition proposed from the memory size) or install
  into an existing partition (on UEFI with the EFI System Partition to use). The medium the live system
  runs from, mounted disks, optical drives and disks under 8 GB are not offered.
- **User**: full name, login name (proposed from the full name), password (at least 8 characters),
  computer name.
- **Summary**: everything chosen, the warning which device is erased, and a checkbox that must be
  ticked before *Install* does anything.
- **Installation**: the ten steps of `amadeos-install` with a progress bar and its log.
- **Finished**: *Restart Now* or close and keep using the live system.

The assistant runs as the live user and starts `amadeos-install` through `pkexec`. The polkit rule
`50-amadeos-installer.rules` lets members of `sudo` (the live user is one) do that without a prompt;
on other systems the administrator's password is asked for.

## amadeos-install

The script can be used on its own, for an unattended installation or from a terminal:

```bash
sudo amadeos-install --disk /dev/sda --swap 4G --hostname pc --user ralf --fullname "Ralf Eichinger" \
    --password-hash "$(mkpasswd -m yescrypt)" --locale de_DE.UTF-8 --keyboard de --timezone Europe/Berlin
```

Without `--yes` it checks everything and prints the plan but writes nothing. `--root-partition` installs
into an existing partition instead of erasing a disk (`--efi-partition` names the EFI System Partition
on UEFI; the first one on the disk otherwise). `--config FILE` reads the same settings as `KEY=VALUE`
lines; that is how the assistant passes them, with the plain password, in a file only root and the
user can read. `--help` lists all options. The log is `/var/log/amadeos-install.log`, copied into the
installed system.

The checks refuse the disk the live system runs from, mounted devices, and running outside a live
system (`--force` overrides the last one). Progress goes to standard output as `@@step N/10 ID`,
`@@percent N`, `@@done` and `@@error MESSAGE` lines for the assistant, with the commands run in
between.

## Building

```bash
./gradlew test               # unit tests
./gradlew deb                # build/distributions/amadeos-installer_0.1.0_all.deb
./gradlew installToAmadeOs   # ... and drop it into ../amadeos/config/packages.chroot/
```

The package installs the assistant (`/usr/bin/amadeos-installer`, jar under `/opt/amadeos-installer`),
the script (`/usr/sbin/amadeos-install`), the polkit action and rule, a desktop entry in the Tools
drawer of the front panel (`Categories=System;Settings;`) and the icon. It depends on `squashfs-tools`,
`dosfstools`, `e2fsprogs`, `grub2-common`, `grub-pc-bin`, `grub-efi-amd64-bin`, `efibootmgr` and
`pkexec`, and recommends `shim-signed` and `grub-efi-amd64-signed` for Secure Boot.
[amade-commons](https://github.com/datazuul/amade-commons) is taken from JitPack, or built from a
checkout in `../amade-commons`.

To try the assistant on a development machine, without root and without a disk, let it run the
stand-in script instead of `amadeos-install`:

```bash
AMADEOS_INSTALL_COMMAND="$PWD/scripts/fake-install.sh" ./gradlew run
FAKE_INSTALL_FAIL=bootloader AMADEOS_INSTALL_COMMAND="$PWD/scripts/fake-install.sh" ./gradlew run
```

## Testing in QEMU

In the amadeos repository, `run-qemu.sh --install-disk` adds an empty 20 GB disk to the virtual machine
(`artifacts/install-disk.qcow2`), `--uefi` boots with OVMF. Install from the live system in the virtual
machine, then boot the disk alone with `run-qemu.sh --boot-disk [--uefi]`.

## Roadmap

- Test on real hardware: UEFI and BIOS machines, NVMe, Secure Boot with the signed shim.
- Dual boot: shrink a partition, keep the other system's boot entry (`os-prober`).
- Encryption (LUKS) of the root partition, a separate home partition.
- Move the shared language, keyboard and time zone code of the login greeter (amade-system) and this
  assistant into amade-commons.

## License

[EUPL-1.2](LICENSE).
