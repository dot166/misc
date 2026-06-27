use std::env;
use lib_aosp::build;

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        println!("Usage: build-android [device]\n this tool can support building multiple devices, just chain [device] as follows\n [device1] [device2] etc");
        panic!("Expected one or more command-line arguments");
    }
    for i in 1..args.len() {
        let mut build = args[i].as_str();
        if build == "emulator" {
            build = "sdk_phone64_x86_64";
        }
        let device = build::get_device(build.parse().unwrap());
        build::build_aosp(device);
    }
}