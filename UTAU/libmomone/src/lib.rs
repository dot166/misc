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
}
