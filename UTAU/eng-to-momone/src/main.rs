use std::io;
use libmomone::convert;
use libmomone::get_note_length;

fn main() {
    let mut input = String::new();
    io::stdin().read_line(&mut input).unwrap();
    let output = convert(input);
    println!("UTAU Lyrics: {}", output);
    let length = get_note_length(output);
    println!("Note Lengths: {}", length)
}
