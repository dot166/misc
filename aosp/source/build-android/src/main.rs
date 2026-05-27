use std::env;
use lib_aosp::build;

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        println!("Usage: build-android [device-build type]\n this tool can support building multiple devices, just chain [device-build type] as follows\n [device1-build type1] [device2-build type2] etc");
        panic!("Expected one or more command-line arguments");
    }
    for i in 1..args.len() {
        let mut build = args[i].split("-").collect::<Vec<&str>>();
        let build_type = build::get_build_type((&build[1]).parse().unwrap());
        if build[0] == "emulator" {
            build[0] = "sdk_phone64_x86_64";
        }
        let device = build::get_device((&build[1]).parse().unwrap());

        build::build_aosp(device, build_type);
    }
}