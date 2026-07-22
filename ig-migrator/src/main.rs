use std::fs::File;
use std::{env, fs, process::Command};
use std::process::Stdio;
use std::path::Path;
use std::path::PathBuf;
use serde::Deserialize;
use image::{Rgb, RgbImage};
use imageproc::drawing::draw_text_mut;
use ab_glyph::{FontArc, PxScale};
use indicatif::{ProgressBar, ProgressStyle};
use futures::stream::{self, StreamExt};
use indicatif::MultiProgress;
use uuid::Uuid;
use std::sync::{
    Arc,
    atomic::{AtomicUsize, Ordering},
};
use std::time::{Duration, SystemTime};

#[derive(Debug, Deserialize)]
struct IGResponse {
    relationships_following: Vec<IGFollowing>,
}

#[derive(Debug, Deserialize)]
struct IGFollowing {
    title: String,
}

#[derive(Debug, Deserialize)]
struct OutFile {
    #[serde(rename = "hiddenFromAll")]
    hidden_from_all: bool,
    #[serde(rename = "isAll")]
    is_all: bool,
    url: String,
}

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() != 3 {
        println!("Usage: cargo run <input_json_file> <output_json_file>");
        return;
    }
    let in_path = Path::new(&args[1]);
    let out_path = Path::new(&args[2]);
    let in_json_str = fs::read_to_string(&in_path)
        .map_err(|e| format!("fs read: {}", e)).unwrap();
    let in_json: IGResponse =
        serde_json::from_str(&in_json_str)
            .map_err(|e| format!("JSON parse error: {}", e)).unwrap();
    println!("{:?}", in_json);
    let out_json_str = fs::read_to_string(&out_path)
        .map_err(|e| format!("fs read: {}", e)).unwrap();
    let mut out_json: Vec<OutFile> =
        serde_json::from_str(&out_json_str)
            .map_err(|e| format!("JSON parse error: {}", e)).unwrap();
    println!("{:?}", out_json);
    for following in in_json.relationships_following {
        println!("{}", following.title);
        let json = OutFile {
            hidden_from_all: false,
            is_all: false,
            url: format!("https://rsshub-balancer.virworks.moe/picnob.info/user/{}", following.title)
        };
        if json_not_contains(&json, &out_json) {
            out_json.push(json)
        }
    }
}

fn json_not_contains(json: &OutFile, out_json: &Vec<OutFile>) -> bool {
    for out in out_json {
        if out.url == json.url {
            return false;
        }
    }
    true
}
