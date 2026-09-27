#!/bin/bash
set -euo pipefail
sudo systemctl enable --now NetworkManager
nmtui
rustup default stable
mkdir ~/git
cd ~/git
git clone https://github.com/dot166/misc
cd ~/git/dotfiles
cargo run $1
