use std::fs;
use std::path::Path;
use std::process::Command;
use quick_xml::Reader;
use quick_xml::events::Event;
use reqwest::blocking::get;

fn fetch_common_sh_at_tag(tag: &str) -> String {
    let url = format!(
        "https://raw.githubusercontent.com/GrapheneOS/script/{}/common.sh",
        tag
    );

    get(url)
        .expect("Failed to fetch file")
        .text()
        .expect("Failed to read body")
}

pub fn read_common_sh() -> (String, String, String) {
    let contents = fetch_common_sh_at_tag(&latest_graphene_tag());

    let mut aosp_tag = String::new();
    let mut aosp_tag_old = String::new();
    let mut branch = String::new();

    for line in contents.lines() {
        if line.starts_with("readonly aosp_tag=") {
            aosp_tag = line
                .trim_start_matches("readonly aosp_tag=")
                .trim_matches('"')
                .to_string();
        } else if line.starts_with("readonly aosp_tag_old=") {
            aosp_tag_old = line
                .trim_start_matches("readonly aosp_tag_old=")
                .trim_matches('"')
                .to_string();
        } else if line.starts_with("readonly branch=") {
            branch = line
                .trim_start_matches("readonly branch=")
                .trim_matches('"')
                .to_string();
        }
    }

    (aosp_tag, aosp_tag_old, branch)
}

pub fn read_config_file() -> (String, String, String) {
    let graphene_tag = latest_graphene_tag();
    let graphene_tag_old = graphene_tag_from_manifest("../../platform_manifest/default.xml");
    let lineage_latest_branch = latest_lineage_branch();

    (graphene_tag, graphene_tag_old, lineage_latest_branch)
}

fn latest_graphene_tag() -> String {
    const URL: &str = "https://releases.grapheneos.org/felix-stable";

    let response = get(URL)
        .expect("Failed to fetch GrapheneOS release metadata");

    let body = response
        .text()
        .expect("Failed to read release metadata");

    let line = body
        .lines()
        .next()
        .expect("Release file was empty");

    let tag = line
        .split_whitespace()
        .next()
        .expect("Failed to parse release tag");

    tag.to_string()
}

fn graphene_tag_from_manifest(path: &str) -> String {
    let xml;
    if Path::new(path).exists() {
        xml = fs::read_to_string(path)
            .expect("Failed to read local manifest")
    } else {
        const URL: &str = "http://dot166.github.io/jOS-Updates/felix-stable";

        let response = get(URL)
            .expect("Failed to fetch release metadata");

        let body = response
            .text()
            .expect("Failed to read release metadata");

        let line = body
            .lines()
            .next()
            .expect("Release metadata was empty");

        let tag = line
            .split_whitespace()
            .next()
            .expect("Failed to parse manifest tag");

        let url = format!(
            "https://raw.githubusercontent.com/dot166/platform_manifest/{}/default.xml",
            tag.to_string()
        );

        xml = get(url)
            .expect("Failed to fetch manifest fallback")
            .text()
            .expect("Failed to read fallback manifest")
    }

    let mut reader = Reader::from_str(&xml);
    reader.config_mut().trim_text(true);

    let mut buf = Vec::new();

    loop {
        match reader.read_event_into(&mut buf) {
            Ok(Event::Start(e)) | Ok(Event::Empty(e)) => {
                if e.name().as_ref() == b"remote" {
                    let mut is_graphene = false;
                    let mut revision = None;

                    for attr in e.attributes().flatten() {
                        match attr.key.as_ref() {
                            b"name" if attr.value.as_ref() == b"grapheneos" => {
                                is_graphene = true;
                            }
                            b"revision" => {
                                revision = Some(
                                    String::from_utf8(attr.value.into_owned())
                                        .expect("Invalid UTF-8 in revision attribute")
                                );
                            }
                            _ => {}
                        }
                    }

                    if is_graphene {
                        let rev = revision
                            .expect("grapheneos remote missing revision attribute");

                        return rev
                            .trim_start_matches("refs/tags/")
                            .to_string();
                    }
                }
            }
            Ok(Event::Eof) => break,
            Err(e) => panic!("XML parsing error: {:?}", e),
            _ => {}
        }
        buf.clear();
    }

    panic!("grapheneos remote not found in manifest");
}

fn latest_lineage_branch() -> String {
    let output = Command::new("git")
        .args([
            "ls-remote",
            "--heads",
            "https://github.com/LineageOS/android"
        ])
        .output()
        .expect("Failed to query LineageOS branches");

    let stdout = String::from_utf8(output.stdout)
        .expect("Invalid UTF-8 from git");

    let mut branches: Vec<String> = stdout
        .lines()
        .filter_map(|line| {
            line.split("refs/heads/")
                .nth(1)
                .map(|s| s.to_string())
        })
        .filter(|b| b.starts_with("lineage-"))
        .collect();

    branches.sort();
    branches.last()
        .cloned()
        .expect("No LineageOS branches found")
}
