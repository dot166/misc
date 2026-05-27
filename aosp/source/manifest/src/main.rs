use anyhow::{Context, Result, ensure};
use clap::Parser;
use lib_aosp::scripts;
use quick_xml::de::from_str;
use serde::{Deserialize, Serialize};
use serde_yaml;
use std::collections::{HashMap, HashSet};
use std::env;
use std::path::{Path, PathBuf};
use tempfile::TempDir;
use tokio;
use tokio::process::Command;

fn get_os_work_dir() -> PathBuf {
    let script_dir = "/misc/aosp";
    let exe_path = env::current_exe().expect("Failed to get executable path");
    let exe_dir = exe_path.parent().expect("Failed to get parent directory");
    let exe_str = exe_dir.to_str().expect("Invalid UTF-8 path");
    if !exe_str.ends_with(script_dir) {
        panic!(
            "Executable path ({}) does not end with {}",
            exe_str, script_dir
        );
    }
    PathBuf::from(&exe_str[..exe_str.len() - script_dir.len()])
}

lazy_static::lazy_static! {
    static ref OS_WORK_DIR: PathBuf = get_os_work_dir();
}

#[derive(Parser, Debug)]
#[command(author, version, about = "Generate Android Manifest", long_about = None)]
struct Cli {
    #[arg(long)]
    config: Option<PathBuf>,

    #[arg(long)]
    out: Option<PathBuf>,
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let cli = Cli::parse();

    let config_path = cli
        .config
        .unwrap_or_else(|| OS_WORK_DIR.join("platform_manifest/config.yml"));

    let out_path = cli
        .out
        .unwrap_or_else(|| OS_WORK_DIR.join("platform_manifest/default.xml"));

    GenerateManifest::run(config_path.as_path(), out_path.as_path()).await?;

    Ok(())
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct AddRemote {
    name: String,
    fetch: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct Remote {
    #[serde(rename = "@name")]
    name: String,
    #[serde(rename = "@fetch")]
    fetch: String,
    #[serde(rename = "@review", skip_serializing_if = "Option::is_none")]
    review: Option<String>,
    #[serde(rename = "@revision", skip_serializing_if = "Option::is_none")]
    revision: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct AddProject {
    path: String,
    name: String,
    groups: Option<String>,
    clone_depth: Option<String>,
    revision: Option<String>,
    remote: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct Project {
    #[serde(rename = "@path")]
    path: String,
    #[serde(rename = "@name")]
    name: String,
    #[serde(rename = "@groups", skip_serializing_if = "Option::is_none")]
    groups: Option<String>,
    #[serde(rename = "@clone-depth", skip_serializing_if = "Option::is_none")]
    clone_depth: Option<String>,
    #[serde(rename = "@revision", skip_serializing_if = "Option::is_none")]
    revision: Option<String>,
    #[serde(rename = "@remote", skip_serializing_if = "Option::is_none")]
    remote: Option<String>,
    #[serde(rename = "@aosp-name", skip_serializing_if = "Option::is_none")]
    aosp_name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    linkfile: Option<Vec<LinkFile>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    copyfile: Option<Vec<LinkFile>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct LinkFile {
    #[serde(rename = "@src")]
    src: String,
    #[serde(rename = "@dest")]
    dest: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct Default {
    #[serde(rename = "@revision")]
    revision: String,
    #[serde(rename = "@remote")]
    remote: String,
    #[serde(rename = "@sync-j")]
    syncj: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct SuperProject {
    #[serde(rename = "@name")]
    name: String,
    #[serde(rename = "@remote")]
    remote: String,
    #[serde(rename = "@revision")]
    revision: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct ContactInfo {
    #[serde(rename = "@bugurl")]
    bugurl: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct ManifestConfig {
    additional_remotes: Vec<AddRemote>,
    additional_projects: Vec<AddProject>,
    additional_non_manifest_repos: Vec<String>,
    forked_repos: HashMap<String, Vec<String>>,
    removed_repos: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct Manifest {
    remote: Vec<Remote>,
    default: Default,
    superproject: SuperProject,
    contactinfo: ContactInfo,
    project: Vec<Project>,
}

struct GenerateManifest;

impl GenerateManifest {
    async fn run(config_file: &Path, out_file: &Path) -> Result<()> {
        let config: ManifestConfig = parse_config(config_file).await?;

        let tmp_dir = TempDir::new().context("Failed to create temp dir")?;
        let tmp_path = tmp_dir.path();

        let status = Command::new("git")
            .args(&[
                "-C",
                tmp_path.to_str().unwrap(),
                "clone",
                "--quiet",
                "https://github.com/grapheneos/platform_manifest",
            ])
            .status()
            .await?;
        ensure!(status.success(), "git clone failed");
        let mut graphene_revision = scripts::latest_graphene_tag_async().await;
        graphene_revision.push_str("~1"); // this is needed as GrapheneOS release tags have a slightly different manifest, so we point to the commit before to get the working manifest at build time
        let status = Command::new("git")
            .args(&[
                "-C",
                tmp_path.join("platform_manifest").to_str().unwrap(),
                "checkout",
                &graphene_revision.as_str(),
                "--quiet",
            ])
            .status()
            .await?;
        ensure!(status.success(), "git checkout failed");

        let manifest_path = tmp_path.join("platform_manifest/default.xml");
        let manifest_str = tokio::fs::read_to_string(&manifest_path)
            .await
            .context("Failed to read default.xml")?;

        let mut manifest: Manifest = from_str(&manifest_str)?;

        {
            graphene_revision = graphene_revision.strip_suffix("~1").unwrap().to_string();
            let remotes = &mut manifest.remote;
            ensure!(remotes.len() == 3, "expected three remotes");
            let (graphene_remotes, other_remotes) = remotes.split_at_mut(1);
            let graphene_remote = &mut graphene_remotes[0];
            let (graphene_gitlab_remote, aosp_remote) = other_remotes.split_at_mut(1);
            let graphene_gitlab_remote = &mut graphene_gitlab_remote[0];
            let aosp_remote = &mut aosp_remote[0];
            ensure!(
                graphene_remote.name == "grapheneos".to_string(),
                "grapheneos remote not found"
            );
            let mut graphene_remote_revision = "refs/tags/".to_string();
            graphene_remote_revision.push_str(graphene_revision.as_str());
            graphene_remote.revision = Some(graphene_remote_revision.clone());
            ensure!(
                graphene_gitlab_remote.name == "grapheneos-gitlab".to_string(),
                "grapheneos-gitlab remote not found"
            );
            graphene_gitlab_remote.revision = Some(graphene_remote_revision.clone());
            ensure!(
                aosp_remote.name == "aosp".to_string(),
                "aosp remote not found"
            );

            let (_, _, branch) = scripts::read_common_sh_async().await;
            let mut revision = "refs/heads/".to_string();
            revision.push_str(branch.as_str());
            let mut additional_remotes: Vec<Remote> = config
                .additional_remotes
                .iter()
                .map(|r| Remote {
                    name: r.name.clone(),
                    fetch: r.fetch.clone(),
                    review: None,
                    revision: Some(revision.clone()),
                })
                .collect();

            additional_remotes.push(graphene_remote.clone());
            additional_remotes.push(graphene_gitlab_remote.clone());
            additional_remotes.push(aosp_remote.clone());
            manifest.remote = additional_remotes;
        }

        {
            let removed: HashSet<String> = config.removed_repos.iter().cloned().collect();
            manifest.project.retain(|p| !removed.contains(&p.name));
        }

        for proj in &config.additional_projects {
            let groups = if let Some(groups) = &proj.groups {
                Some(groups.clone())
            } else {
                None
            };
            let clone_depth = if let Some(depth) = &proj.clone_depth {
                Some(depth.clone())
            } else {
                None
            };
            let revision = if let Some(revision) = &proj.revision {
                Some(revision.clone())
            } else {
                None
            };
            manifest.project.push(Project {
                path: proj.path.clone(),
                name: proj.name.clone(),
                groups,
                clone_depth,
                remote: Some(
                    proj.remote
                        .clone()
                        .unwrap_or_else(|| config.additional_remotes.first().unwrap().name.clone()),
                ),
                revision,
                aosp_name: None,
                linkfile: None, // not needed by me yet, so don't support it
                copyfile: None, // not needed by me yet, so don't support it
            });
        }

        manifest
            .project
            .sort_by(|a, b| a.path.to_lowercase().cmp(&b.path.to_lowercase()));

        let fork_map = make_aosp_fork_map(&config);
        let mut forks: Vec<String> = Vec::new();
        for proj in &mut manifest.project {
            let name = proj.name.clone();
            if let Some(remote_name) = fork_map.get(&name) {
                let fork_name = make_fork_name(&name);
                proj.name = fork_name.clone();
                forks.push(fork_name.clone());
                proj.remote = Some(remote_name.clone());
            }
        }

        let mut lines = vec![
            r#"<?xml version="1.0" encoding="UTF-8"?>"#.to_string(),
            "<!-- This file was generated by manifest. To update it, edit config.yml and run"
                .to_string(),
            "'manifest' from the root of OS checkout. -->".to_string(),
            "<manifest>".to_string(),
        ];
        for r in &manifest.remote {
            lines.push(format!("  {}", remote_to_element_string(r)));
        }
        lines.push(format!(
            r#"  <default revision="{}" remote="{}" sync-j="{}"/>"#,
            manifest.default.revision, manifest.default.remote, manifest.default.syncj
        ));
        lines.push(format!(
            r#"  <superproject name="{}" remote="{}" revision="{}"/>"#,
            manifest.superproject.name,
            manifest.superproject.remote,
            manifest.superproject.revision
        ));
        lines.push(format!(
            r#"  <contactinfo bugurl="{}"/>"#,
            manifest.contactinfo.bugurl
        ));
        for p in &manifest.project {
            lines.push(format!("  {}", project_to_element_string(p)));
        }
        lines.push("</manifest>".to_string());
        lines.push("".to_string());
        let final_xml = lines.join("\n");
        tokio::fs::write(out_file, final_xml).await?;
        env::set_current_dir(out_file.parent().unwrap()).unwrap();
        let status = Command::new("git").arg("diff").arg("--quiet").status().await;

        let changes = if let Ok(status) = status {
            if status.success() { 0 } else { 1 }
        } else {
            1
        };

        println!("CHANGES={}", changes);

        if changes == 1 {
            let status = Command::new("git")
                .arg("commit")
                .arg("default.xml")
                .arg("-m")
                .arg(format!("Update Manifest {}", chrono::offset::Utc::now().date_naive().format("%Y%m%d")))
                .status()
                .await;

            if status.is_err() {
                panic!(
                    "Error committing default.xml: {}",
                    status.unwrap_err()
                );
            }
        }
        Ok(())
    }
}

async fn parse_config(path: &Path) -> Result<ManifestConfig> {
    let data = tokio::fs::read_to_string(path).await?;
    let config: ManifestConfig = serde_yaml::from_str(&data)?;
    Ok(config)
}

fn make_aosp_fork_map(config: &ManifestConfig) -> HashMap<String, String> {
    let mut res = HashMap::new();
    for (remote, repos) in &config.forked_repos {
        for repo in repos {
            res.insert(repo.clone(), remote.clone());
        }
    }
    res
}

fn make_fork_name(name: &str) -> String {
    name.replace('/', "_")
}

fn remote_to_element_string(remote: &Remote) -> String {
    let mut s = format!(r#"<remote name="{}" fetch="{}""#, remote.name, remote.fetch);

    if let Some(review) = &remote.review {
        if !review.is_empty() {
            s += &format!(r#" review="{}""#, review);
        }
    }
    if let Some(revision) = &remote.revision {
        if !revision.is_empty() {
            s += &format!(r#" revision="{}""#, revision);
        }
    }

    s += "/>";
    s
}

fn project_to_element_string(proj: &Project) -> String {
    let mut s = format!(r#"<project path="{}" name="{}""#, proj.path, proj.name);

    if let Some(groups) = &proj.groups {
        if !groups.is_empty() {
            s += &format!(r#" groups="{}""#, groups);
        }
    }
    if let Some(revision) = &proj.revision {
        if !revision.is_empty() {
            s += &format!(r#" revision="{}""#, revision);
        }
    }
    if let Some(remote) = &proj.remote {
        if !remote.is_empty() {
            s += &format!(r#" remote="{}""#, remote);
        }
    }
    if let Some(clone_depth) = &proj.clone_depth {
        if !clone_depth.is_empty() {
            s += &format!(r#" clone-depth="{}""#, clone_depth);
        }
    }
    if let Some(aosp_name) = &proj.aosp_name {
        if !aosp_name.is_empty() {
            s += &format!(r#" aosp-name="{}""#, aosp_name);
        }
    }
    if let Some(linkfile) = &proj.linkfile {
        if !linkfile.is_empty() {
            s += ">\n";
            for file in linkfile {
                s += &format!(r#"    <linkfile src="{}" dest="{}"/>"#, file.src, file.dest);
                s += "\n";
            }
            s += "  </project>"
        }
    } else if let Some(copyfile) = &proj.copyfile {
        if !copyfile.is_empty() {
            s += ">\n";
            for file in copyfile {
                s += &format!(r#"    <copyfile src="{}" dest="{}"/>"#, file.src, file.dest);
                s += "\n";
            }
            s += "  </project>"
        }
    } else {
        s += "/>";
    }
    s
}
