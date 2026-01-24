use std::io;
use libmomone::get_note_length;

fn main() {
    let mut input = String::new();
    io::stdin().read_line(&mut input).unwrap();
    let output = get_note_length(input);
    println!("Note Lengths: {}", output)
}
