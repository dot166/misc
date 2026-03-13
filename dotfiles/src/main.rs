use anyhow::{bail, Context, Result};
use clap::Parser;
use os_release::OsRelease;
use std::{
    fs,
    path::{Path, PathBuf},
    process::{Command, Stdio},
};
use tempfile::TempDir;

#[derive(Parser, Debug)]
#[command(author, version, about = "Arch Linux bootstrap tool")]
struct Args {
    #[arg(long)]
    desktop: bool,

    #[arg(long)]
    laptop: bool,

    #[arg(long)]
    server: bool,
}

fn main() -> Result<()> {
    let args = Args::parse();

    ensure_arch()?;
    println!("[*] Running on supported Arch-based distro");

    println!("[*] Updating system...");
    Pkg::system_update()?;

    ensure_yay_installed()?;

    install_base_packages()?;
    println!("[*] Configuring rustup (cargo and rustc)");
    run("rustup", &["default", "stable"])?;
    run("rustup", &["target", "add", "x86_64-pc-windows-gnu"])?;

    if !args.server {
        install_gui()?;
    }

    install_oh_my_zsh()?;
    install_zsh_plugins()?;
    setup_flatpak()?;

    if args.desktop {
        install_dev_stack()?;
        install_steam()?;
    }

    if args.laptop {
        install_libreoffice()?;
        Pkg::install(&["displaylink", "evdi-dkms", "kid3", "openutau-bin"])?;
    }

    if args.server {
        install_server_components()?;
    }

    println!("[*] Setting Zsh as default shell...");
    run("chsh", &["-s", "/usr/bin/zsh"])?;

    println!("[@] Setup complete! Reboot recommended.");
    Ok(())
}

fn ensure_arch() -> Result<()> {
    let info = OsRelease::new().context("Failed to parse /etc/os-release")?;

    match info.id.as_str() {
        "arch" => Ok(()),
        other => bail!("Unsupported distribution: {:?}", other),
    }
}

struct Pkg;

impl Pkg {
    fn system_update() -> Result<()> {
        run("sudo", &["pacman", "-Syu", "--noconfirm"])
    }

    fn install(pkgs: &[&str]) -> Result<()> {
        let missing: Vec<&str> = pkgs
            .iter()
            .copied()
            .filter(|pkg| !Self::is_installed(pkg))
            .collect();

        if missing.is_empty() {
            println!("[*] All packages already installed.");
            return Ok(());
        }

        println!("[*] Installing missing packages: {:?}", missing);

        let mut args = vec!["-S"];
        args.extend(&missing);
        args.push("--noconfirm");
        run("yay", &args)
    }

    fn is_installed(pkg: &str) -> bool {
        Command::new("pacman")
            .args(["-Q", pkg])
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .status()
            .map(|s| s.success())
            .unwrap_or(false)
    }
}

fn ensure_yay_installed() -> Result<()> {
    if Pkg::is_installed("yay") {
        println!("yay already installed.");
        return Ok(());
    }

    println!("Installing yay from AUR...");

    run(
        "sudo",
        &["pacman", "-S", "--noconfirm", "--needed", "base-devel", "git"],
    )?;

    let tmp = TempDir::new()?;
    let yay_path = tmp.path().join("yay");

    run(
        "git",
        &[
            "clone",
            "https://aur.archlinux.org/yay-bin.git",
            yay_path.to_str().unwrap(),
        ],
    )?;

    run_in_dir(&yay_path, "makepkg", &["-si", "--noconfirm"])?;

    println!("yay installed.");
    Ok(())
}

fn install_base_packages() -> Result<()> {
    println!("[*] Installing base packages...");
    Pkg::install(&[
        "zsh",
        "vim",
        "git",
        "curl",
        "figlet",
        "lolcat",
        "flatpak",
        "fastfetch",
        "hyfetch",
        "github-cli",
        "wget",
        "ex-vi-compat",
        "rustup",
        "jdk17-openjdk",
        "mikusays",
        "mingw-w64-gcc",
        "dosfstools",
        "thefuck"
    ])?;
    Ok(())
}

fn install_gui() -> Result<()> {
    println!("[*] Installing GUI...");
    Pkg::install(&[
        "plasma-login-manager",
        "wacomtablet",
        "firefox",
        "vlc-plugins-all",
        "joplin-bin",
        "signal-desktop",
        "plasma-desktop",
        "plasma-pa",
        "plasma-nm",
        "dolphin",
        "konsole",
        "audiocd-kio",
        "kio-admin",
        "dolphin-plugins",
        "krita",
        "ffmpegthumbs",
        "icoutils",
        "kde-thumbnailer-apk",
        "kdegraphics-thumbnailers",
        "kdesdk-thumbnailers",
        "kimageformats",
        "libheif",
        "libappimage",
        "qt6-imageformats",
        "raw-thumbnailer",
        "resvg",
        "taglib",
        "kdenetwork-filesharing",
        "flatpak-kcm",
        "filelight",
        "oxygen",
        "oxygen5",
        "oxygen-icons",
        "oxygen-icons-svg",
        "oxygen-sounds",
        "vlc",
        "linux-headers",
        "dkms",
        "powerdevil",
        "ark",
        "proton-pass-bin",
        "proton-vpn-gtk-app",
        "partitionmanager",
        "plasma-systemmonitor",
        "yt-dlp",
        "qemu-full",
        "virt-manager",
        "dnsmasq",
        "kde-gtk-config",
        "fcitx5-im",
        "fcitx5-mozc-ut",
        "mozc-ut",
        "android-studio",
        "visual-studio-code-bin",
        "github-desktop-bin",
        "kscreen",
        "spectacle",
        "sof-firmware",
        "android-udev"
    ])?;

    run("sudo", &["systemctl", "enable", "plasmalogin"])?;
    Ok(())
}

fn install_oh_my_zsh() -> Result<()> {
    let home = std::env::var("HOME")?;
    let path = Path::new(&home).join(".oh-my-zsh");

    if !path.exists() {
        println!("[*] Installing Oh My Zsh...");
        run(
            "sh",
            &[
                "-c",
                "curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh | sh -s -- --unattended",
            ],
        )?;
    }

    Ok(())
}

fn install_zsh_plugins() -> Result<()> {
    println!("[*] Installing ZSH plugins...");

    let home = std::env::var("HOME")?;
    let custom_dir = PathBuf::from(format!("{}/.oh-my-zsh/custom/plugins", home));
    fs::create_dir_all(&custom_dir)?;

    let autosuggestions = custom_dir.join("zsh-autosuggestions");
    let syntax = custom_dir.join("zsh-syntax-highlighting");

    if !autosuggestions.exists() {
        run(
            "git",
            &[
                "clone",
                "https://github.com/zsh-users/zsh-autosuggestions",
                autosuggestions.to_str().unwrap(),
            ],
        )?;
    }

    if !syntax.exists() {
        run(
            "git",
            &[
                "clone",
                "https://github.com/zsh-users/zsh-syntax-highlighting.git",
                syntax.to_str().unwrap(),
            ],
        )?;
    }

    Ok(())
}

fn setup_flatpak() -> Result<()> {
    println!("[*] Setting up Flatpak and Flathub...");
    run(
        "flatpak",
        &[
            "remote-add",
            "--if-not-exists",
            "flathub",
            "https://flathub.org/repo/flathub.flatpakrepo",
        ],
    )
}

fn install_dev_stack() -> Result<()> {
    println!("[*] Installing GrapheneOS development tools...");
    Pkg::install(&["grapheneos-devel"])
}

fn install_steam() -> Result<()> {
    println!("[*] Installing gaming tools...");
    Pkg::install(&["steam", "obs-studio", "v4l2loopback-dkms"])?;
    run("flatpak", &["install", "-y", "flathub", "com.usebottles.bottles"])
}

fn install_libreoffice() -> Result<()> {
    println!("[*] Installing LibreOffice...");
    Pkg::install(&["libreoffice", "hunspell-en_gb", "hunspell", "hunspell-ja-git"])
}

fn install_server_components() -> Result<()> {
    println!("[*] Installing server components...");
    Pkg::install(&["webmin"])
}

fn run(cmd: &str, args: &[&str]) -> Result<()> {
    let status = Command::new(cmd).args(args).status()?;

    if !status.success() {
        bail!("Command failed: {} {:?}", cmd, args);
    }

    Ok(())
}

fn run_in_dir(dir: &Path, cmd: &str, args: &[&str]) -> Result<()> {
    let status = Command::new(cmd)
        .current_dir(dir)
        .args(args)
        .status()?;

    if !status.success() {
        bail!("Command failed in {:?}: {} {:?}", dir, cmd, args);
    }

    Ok(())
}
