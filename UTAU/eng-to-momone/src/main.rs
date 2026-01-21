use std::io;
use libmomone::convert;

fn main() {
    let mut input = String::new();
    io::stdin().read_line(&mut input).unwrap();
    let output = convert(input);
    println!("{}", output);
}
