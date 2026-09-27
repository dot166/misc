#!/bin/bash
set -euo pipefail
pacstrap -K /mnt base linux linux-firmware $1-ucode sof-firmware networkmanager vim grub efibootmgr rustup

grub-install --target=x86_64-efi --efi-directory=/boot --bootloader-id=GRUB
