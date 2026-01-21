use std::collections::HashMap;

fn default_map() -> HashMap<String, String> {
    let map = HashMap::new();

    // Empty for now
    //map.insert("test".into(), "te su to".into()); // experimental, broke it, this is only here to provide an example for other people on how to add to libmomone

    map
}

pub fn convert(input: String) -> String {
    let map = default_map();

    input
        .to_lowercase()
        .split_whitespace()
        .map(|word| {
            map.get(word)
                .cloned()
                .unwrap_or_else(|| word.to_string())
        })
        .collect::<Vec<_>>()
        .join(" ")
}

pub fn get_note_length(input: String) -> String {
    let binding = input
        .to_lowercase();
    let notes = binding
        .split_whitespace()
        .collect::<Vec<_>>();

    let mut note_lengths: Vec<String> = Vec::new();

    for note in notes {
        // remove punctuation from note before calculating the length because the punctuation would break the calculation and make the note too long
        note_lengths.push((note.replace(".","").replace(",","").replace("?","").replace("!","").replace("'","").len()*180).to_string());
    }

    note_lengths.join(" ")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_swaps_known_words() {
        // empty, so no tests (·•᷄∩•᷅ )
        //let result = convert("test".to_string());
        //assert_eq!(result, "te su to");
    }

    #[test]
    fn it_leaves_unknown_words() {
        let result = convert("hello world".to_string());
        assert_eq!(result, "hello world");
    }

    #[test]
    fn note_length_correct() {
        let result = get_note_length("test".to_string());
        assert_eq!(result, "720");
    }
}
