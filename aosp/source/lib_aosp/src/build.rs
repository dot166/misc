use crate::utils::*;
use std::env;
use std::fmt::{Display, Formatter};
use std::io::Error;
use std::process::Command;

pub enum Device {
    // list of devices natively supported by GrapheneOS (and forks of it)
    Rango, //rango (Pixel 10 Pro Fold)
    Mustang, //mustang (Pixel 10 Pro XL)
    Blazer, //blazer (Pixel 10 Pro)
    Frankel, //frankel (Pixel 10)
    Tegu, //tegu (Pixel 9a)
    Comet, //comet (Pixel 9 Pro Fold)
    Komodo, //komodo (Pixel 9 Pro XL)
    Caiman, //caiman (Pixel 9 Pro)
    Tokay, //tokay (Pixel 9)
    Akita, //akita (Pixel 8a)
    Husky, //husky (Pixel 8 Pro)
    Shiba, //shiba (Pixel 8)
    Felix, //felix (Pixel Fold)
    Tangorpro, //tangorpro (Pixel Tablet)
    Lynx, //lynx (Pixel 7a)
    Cheetah, //cheetah (Pixel 7 Pro)
    Panther, //panther (Pixel 7)
    Bluejay, //bluejay (Pixel 6a)
    Raven, //raven (Pixel 6 Pro)
    Oriole, //oriole (Pixel 6)
    SdkPhone64X8664, //sdk_phone64_x86_64 (Emulator for x86_64/amd64 pc)
}

impl Display for Device {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Rango => f.write_str("rango"),
            Self::Mustang => f.write_str("mustang"),
            Self::Blazer => f.write_str("blazer"),
            Self::Frankel => f.write_str("frankel"),
            Self::Tegu => f.write_str("tegu"),
            Self::Comet => f.write_str("comet"),
            Self::Komodo => f.write_str("komodo"),
            Self::Caiman => f.write_str("caiman"),
            Self::Tokay => f.write_str("tokay"),
            Self::Akita => f.write_str("akita"),
            Self::Husky => f.write_str("husky"),
            Self::Shiba => f.write_str("shiba"),
            Self::Felix => f.write_str("felix"),
            Self::Tangorpro => f.write_str("tangorpro"),
            Self::Lynx => f.write_str("lynx"),
            Self::Cheetah => f.write_str("cheetah"),
            Self::Panther => f.write_str("panther"),
            Self::Bluejay => f.write_str("bluejay"),
            Self::Raven => f.write_str("raven"),
            Self::Oriole => f.write_str("oriole"),
            Self::SdkPhone64X8664 => f.write_str("sdk_phone64_x86_64"),
        }
    }
}

pub fn get_device(device: String) -> Device {
    match device.as_str() {
        "rango" => Device::Rango,
        "mustang" => Device::Mustang,
        "blazer" => Device::Blazer,
        "frankel" => Device::Frankel,
        "tegu" => Device::Tegu,
        "comet" => Device::Comet,
        "komodo" => Device::Komodo,
        "caiman" => Device::Caiman,
        "tokay" => Device::Tokay,
        "akita" => Device::Akita,
        "husky" => Device::Husky,
        "shiba" => Device::Shiba,
        "felix" => Device::Felix,
        "tangorpro" => Device::Tangorpro,
        "lynx" => Device::Lynx,
        "cheetah" => Device::Cheetah,
        "panther" => Device::Panther,
        "bluejay" => Device::Bluejay,
        "raven" => Device::Raven,
        "oriole" => Device::Oriole,
        "sdk_phone64_x86_64" => Device::SdkPhone64X8664,
        _=>panic!("Device is currently unsupported by this script, please check https://grapheneos.org/build for manual build instructions"),
    }
}

pub enum BuildType {
    ENG, // eng
    UserDebug, // userdebug
    USER, // user
}

impl Display for BuildType {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::ENG => f.write_str("eng"),
            Self::UserDebug => f.write_str("userdebug"),
            Self::USER => f.write_str("user"),
        }
    }
}

pub fn get_build_type(build_type: String) -> BuildType {
    match build_type.as_str() {
        "eng" => BuildType::ENG,
        "userdebug" => BuildType::UserDebug,
        "user" => BuildType::USER,
        _=>default_to_user(),
    }
}

fn default_to_user() -> BuildType {
    println!("the selected build variant is currently unsupported by this script.");
    println!("using default (user) instead");
    BuildType::USER
}

pub fn build_aosp(device: Device, build_type: BuildType) {
    require_top();
    env::set_current_dir(get_top().unwrap()).unwrap();
    let build_args;
    match device {
        // build args are sourced from https://grapheneos.org/build
        Device::Rango=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Mustang=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Blazer=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Frankel=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Tegu=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Comet=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Komodo=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Caiman=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Tokay=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Akita=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Husky=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Shiba=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Felix=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Tangorpro=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Lynx=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Cheetah=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Panther=>build_args = "vendorbootimage vendorkernelbootimage target-files-package otatools-package",
        Device::Bluejay=>build_args = "vendorbootimage target-files-package otatools-package",
        Device::Raven=>build_args = "vendorbootimage target-files-package otatools-package",
        Device::Oriole=>build_args = "vendorbootimage target-files-package otatools-package",
        Device::SdkPhone64X8664=>build_args = "",
    }
    let pixel_args = format!("script/finalize.sh && script/generate-release.sh {} $BUILD_NUMBER", device);
    let extra_args;
    if build_args.is_empty() { // only on emulator
        extra_args = "emulator";
    } else {
        println!("Building drivers for {}", device);
        let output = Command::new("bash")
            .arg("-c")
            .arg("yarn install --cwd vendor/adevtool/")
            .spawn()
            .unwrap()
            .wait_with_output()
            .unwrap();

        if !output.status.success() {
            panic!("adevtool build failed with status: {}", output.status);
        }

        let output = Command::new("bash")
            .arg("-c")
            .arg(format!("vendor/adevtool/bin/run generate-all -d {}", device))
            .spawn()
            .unwrap()
            .wait_with_output()
            .unwrap();

        if !output.status.success() {
            panic!("driver build failed with status: {}", output.status);
        }
        println!("Finished building drivers for {}", device);
        extra_args = pixel_args.as_str();
    }
    let output = Command::new("bash")
        .arg("-c")
        .arg(format!("source build/envsetup.sh && lunch {}-cur-{} && sudo rm -rf out && m {} && {}", device, build_type, build_args, extra_args))
        .spawn()
        .unwrap()
        .wait_with_output()
        .unwrap();

    if !output.status.success() {
        panic!("Build failed with status: {}", output.status);
    }
}

pub fn repo_sync() -> Result<(), Error> {
    require_top();
    env::set_current_dir(get_top().unwrap()).expect("cannot set current dir");
    let output = Command::new("repo")
        .arg("sync")
        .arg("-j")
        .arg("32")
        .arg("--force-sync")
        .spawn()?
        .wait_with_output()?;
    let status = output.status;

    if !status.success() {
        return Err(Error::new(
            std::io::ErrorKind::Other,
            format!("repo sync failed with exit code {}", status.code().unwrap_or(-1)),
        ));
    }

    Ok(())
}
