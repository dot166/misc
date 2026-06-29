use rand::{distr::Alphanumeric, RngExt};
use std::{fs, path::PathBuf};

fn generate_random_string(len: usize) -> String {
    rand::rng()
        .sample_iter(&Alphanumeric)
        .take(len)
        .map(char::from)
        .collect()
}

pub fn generate_utau_projects(replacement_strings: Vec<&str>) {
    let file_template = fs::read_to_string("templates/template.txt").unwrap();
    let speech_template = fs::read_to_string("templates/word.txt").unwrap();

    let mut updated_content = file_template;
    let mut total_dur = snap_to_grid(300, 60); // add padding of 10 grid space

    let mut output_path = PathBuf::from("output");
    fs::create_dir_all(&output_path).unwrap();
    output_path.push(format!("{}.ustx", generate_random_string(8)));

    for replacement_string in replacement_strings {
        let words: Vec<String> = replacement_string
            .split_whitespace()
            .map(|w| w.to_string())
            .collect();

        for word in words {
            let mut segment = speech_template.clone();

            segment = segment.replace("POS__", &total_dur.to_string());
            segment = segment.replace("WORD__", &word);
            let duration: i32 = get_note_length(word).parse().unwrap();
            segment = segment.replace("DUR__", &duration.to_string());

            total_dur += duration;

            let tone: u8 = rand::rng().random_range(64..=67);
            segment = segment.replace("TONE__", &tone.to_string());

            updated_content.push('\n');
            updated_content.push_str(&segment);
        }

        total_dur += 120; // gap between sentences
    }
    updated_content = updated_content.replace("TOTAL__DUR__", &total_dur.to_string());

    updated_content.push_str("\n  curves: []");
    updated_content.push_str("\nwave_parts: []");
    fs::write(output_path, updated_content).unwrap();
}

fn snap_to_grid(value: i32, grid: i32) -> i32 {
    ((value + grid / 2) / grid) * grid
}

fn count_vowels(s: &str) -> usize {
    s.chars()
        .filter(|c| matches!(c, 'a' | 'e' | 'i' | 'o' | 'u'))
        .count()
}

pub fn get_note_length(input: String) -> String {
    let binding = input
        .to_lowercase();
    let notes = binding
        .split_whitespace()
        .collect::<Vec<_>>();

    let mut note_lengths: Vec<String> = Vec::new();

    let base: f32 = 300.0;
    let scale: f32 = 220.0;

    for note in notes {
        let vowels = count_vowels(&note).max(1) as f32;

        let mut note_length = (base + vowels.sqrt() * scale) as i32;

        // Snap to UTAU's default 60-tick grid
        note_length = snap_to_grid(note_length, 60);

        note_lengths.push(note_length.to_string());
    }

    note_lengths.join(" ")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn note_length_correct() {
        let result = get_note_length("test".to_string());
        assert_eq!(result, "540");
    }
}
