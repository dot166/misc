use libmomone::generate_utau_projects;
use std::fs;
use std::env;

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        println!("No file provided");
        return;
    }

    let contents = fs::read_to_string(&args[1]).unwrap();

    let sentences: Vec<&str> = contents
        .lines()
        .map(str::trim)
        .filter(|l| !l.is_empty() && !l.starts_with('#'))
        .collect();
    generate_utau_projects(sentences);
}
