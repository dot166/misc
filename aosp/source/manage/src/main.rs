use lib_aosp::build::{get_build_type, get_device, BuildType};
use lib_aosp::scripts;
use std::process::Command;
use std::{env, fs, path::Path};
use anyhow::{Context, Result, ensure};
use quick_xml::de::from_str;
use serde::{Deserialize, Serialize};
use serde_yaml;
use std::collections::{HashMap, HashSet};
use tempfile::TempDir;

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
    additional_non_manifest_repos: HashMap<String, Vec<String>>,
    forked_repos: HashMap<String, HashMap<String, Vec<String>>>,
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
    fn run(config: ManifestConfig, mut graphene_revision: String, branch :String) -> Result<()> {
        let out_file = Path::new("platform_manifest/default.xml");
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
            .status()?;
        ensure!(status.success(), "git clone failed");
        graphene_revision.push_str("~1"); // this is needed as GrapheneOS release tags have a slightly different manifest, so we point to the commit before to get the working manifest at build time
        let status = Command::new("git")
            .args(&[
                "-C",
                tmp_path.join("platform_manifest").to_str().unwrap(),
                "checkout",
                &graphene_revision.as_str(),
                "--quiet",
            ])
            .status()?;
        ensure!(status.success(), "git checkout failed");

        let manifest_path = tmp_path.join("platform_manifest/default.xml");
        let manifest_str = fs::read_to_string(&manifest_path)
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
            "<!-- This file was generated by manage. To update it, edit config.yml in misc"
                .to_string(),
            "and run 'manage manifest_only' from the root of OS checkout. -->".to_string(),
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
        fs::write(out_file, final_xml)?;
        env::set_current_dir(out_file.parent().unwrap())?;
        let status = Command::new("git").arg("diff").arg("--quiet").status();

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
                .status();

            if status.is_err() {
                panic!(
                    "Error committing default.xml: {}",
                    status.unwrap_err()
                );
            }

            let status = Command::new("git").arg("push").status();

            if status.is_err() {
                panic!(
                    "Error pushing new manifest: {}",
                    status.unwrap_err()
                );
            }
        }
        Ok(())
    }
}

fn parse_config(path: &Path) -> Result<ManifestConfig> {
    let data = fs::read_to_string(path)?;
    let config: ManifestConfig = serde_yaml::from_str(&data)?;
    Ok(config)
}

fn make_aosp_fork_map(config: &ManifestConfig) -> HashMap<String, String> {
    let mut res = HashMap::new();
    for (remote, repos_and_upstreams) in &config.forked_repos {
        for (_, repos) in repos_and_upstreams {
            for repo in repos.iter() {
                res.insert(repo.clone(), remote.clone());
            }
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

fn init(action: String, repo: String, branch: String) {
    if action == "init" {
        if !Path::new(repo.as_str()).exists() {
            let status = Command::new("git")
                .arg("clone")
                .arg(format!("https://github.com/dot166/{}", repo))
                .status();

            if status.is_err() {
                panic!("Error cloning {}: {}", repo, status.unwrap_err());
            }
        }
    }

    let status = env::set_current_dir(&repo);
    if status.is_err() {
        panic!(
            "Failed to change directory to {}: {}",
            repo,
            status.unwrap_err()
        );
    }

    if action != "bupdate" {
        let status = Command::new("git").arg("checkout").arg(&branch).status();

        if status.is_err() {
            panic!(
                "Error checking out branch {}: {}",
                &branch,
                status.unwrap_err()
            );
        }
    } else {
        let status = Command::new("git").arg("checkout").arg("origin").status();

        if status.is_err() {
            panic!(
                "Error checking out origin for {}: {}",
                repo,
                status.unwrap_err()
            );
        }

        let status = Command::new("git").arg("pull").status();

        if status.is_err() {
            panic!(
                "Error pulling changes for {}: {}",
                repo,
                status.unwrap_err()
            );
        }

        let status = Command::new("git")
            .arg("switch")
            .arg("-c")
            .arg(&branch)
            .status();

        if status.is_err() {
            panic!(
                "Error switching to branch {}: {}",
                &branch,
                status.unwrap_err()
            );
        }

        let status = Command::new("git")
            .arg("push")
            .arg("--set-upstream")
            .arg("origin")
            .arg(&branch)
            .status();

        if status.is_err() {
            panic!(
                "Error pushing {} to upstream: {}",
                &branch,
                status.unwrap_err()
            );
        }
    }

    let status = Command::new("git").arg("pull").status();

    if status.is_err() {
        panic!(
            "Error pulling changes for {}: {}",
            repo,
            status.unwrap_err()
        );
    }
}

fn init_disable_bupdate(action: String, repo: String) {
    if action == "bupdate" {
        // bupdate is not supported, treat as update during init
        init("update".to_string(), repo.clone(), "main".to_string());
    } else {
        init(action.clone(), repo.clone(), "main".to_string());
    }
}

fn default(repo: String, branch: String) {
    let status = Command::new("gh")
        .arg("repo")
        .arg("edit")
        .arg(format!("dot166/{}", repo))
        .arg("--default-branch")
        .arg(&branch)
        .status();

    if status.is_err() {
        panic!(
            "Error editing default branch for {}: {}",
            repo,
            status.unwrap_err()
        );
    }
}

fn init2_graphene(action: String, repo: String) {
    if action == "init" {
        let remote_url = &format!("https://github.com/grapheneos/{}", repo);

        let status = Command::new("git")
            .arg("remote")
            .arg("add")
            .arg("upstream")
            .arg(remote_url)
            .status();

        if status.is_err() {
            panic!(
                "Error adding upstream for {}: {}",
                repo,
                status.unwrap_err()
            );
        }

        let status = Command::new("git")
            .arg("fetch")
            .arg("upstream")
            .arg("--tags")
            .status();

        if status.is_err() {
            panic!(
                "Error fetching upstream tags for {}: {}",
                repo,
                status.unwrap_err()
            );
        }

        let status = Command::new("gh")
            .arg("repo")
            .arg("set-default")
            .arg("origin")
            .status();

        if status.is_err() {
            panic!(
                "Error setting default github repo for {}: {}",
                repo,
                status.unwrap_err()
            );
        }
    }
}

fn main() -> Result<()> {
    let config: ManifestConfig = parse_config(Path::new("misc/aosp/config.yml"))?;
    let (aosp_tag, aosp_tag_old, branch) = scripts::read_common_sh();
    let (graphene_tag, graphene_tag_old) = scripts::read_config_file();
    println!("aosp_tag: {}", aosp_tag);
    println!("aosp_tag_old: {}", aosp_tag_old);
    println!("branch: {}", branch);
    println!("graphene_tag: {}", graphene_tag);
    println!("graphene_tag_old: {}", graphene_tag_old);
    let args: Vec<String> = env::args().collect();
    if args.len() == 1 {
        panic!("expected action as argument");
    }
    let action = args[1].clone();
    let mut tag_name = "";
    let mut builds: Vec<String> = Vec::new();

    if action == "update"
        || action == "default"
        || action == "init"
        || action == "bupdate"
        || action == "manifest_only"
    {
        if env::var("IS_CI").unwrap_or("false".parse()?) == "true" && action != "init" {
            panic!(
                "cannot use {} in ci, this is done to prevent the ci from destroying the source tree",
                action
            );
        }
        if args.len() != 2 {
            panic!("expected no arguments for {}", action);
        }
    } else if action == "delete" {
        if env::var("IS_CI").unwrap_or("false".parse()?) == "true" {
            panic!(
                "cannot use {} in ci, this is done to prevent the ci from destroying the source tree",
                action
            );
        }
        if args.len() != 3 {
            panic!("expected tag name as argument for {}", action);
        }
        tag_name = &args[2];
    } else if action == "release" {
        if env::var("IS_CI").unwrap_or("false".parse()?) == "true" {
            panic!(
                "cannot use {} in ci, this is done to prevent the ci from destroying the source tree",
                action
            );
        }
        if args.len() < 4 {
            panic!("expected at least 2 arguments for {}", action);
        }
        tag_name = &args[2];
        for i in 3..args.len() {
            let device = args[i].split("-").collect::<Vec<&str>>()[0];
            if device == "emulator" || device == "sdk_phone64_x86_64" {
                panic!("releasing emulator is not yet supported");
            }
            builds.push(args[i].clone());
        }
    } else {
        panic!("unrecognized action");
    }
    if config.forked_repos.get("jos") == None {
        panic!("Config error")
    }
    let additional_config = config.additional_projects.clone();
    let additional_non_manifest_config = config.additional_non_manifest_repos.clone();
    let forked_config = config.forked_repos.get("jos").unwrap();
    let mut aosp_forks: Vec<String> = Vec::new();
    aosp_forks.append(forked_config.clone().get_mut("aosp").unwrap().iter().map(|s| make_fork_name(s)).collect::<Vec<String>>().as_mut());
    let mut grapheneos_forks: Vec<String> = Vec::new();
    grapheneos_forks.append(forked_config.clone().get_mut("graphene").unwrap());
    let mut grapheneos_app_forks: Vec<String> = Vec::new();
    grapheneos_app_forks.append(additional_non_manifest_config.clone().get_mut("forked_graphene_apps").unwrap());
    let mut independent: Vec<String> = Vec::new();
    for repo in additional_config {
        independent.push(repo.name);
    }
    independent.append(additional_non_manifest_config.clone().get_mut("independent").unwrap());

    println!("aosp_forks: {}", aosp_forks.join(", "));
    println!("grapheneos_forks: {}", grapheneos_forks.join(", "));
    println!("grapheneos_app_forks: {}", grapheneos_app_forks.join(", "));
    println!("independent: {}", independent.join(", "));

    for repo in aosp_forks {
        println!(">>> Handling {}", repo);
        init(action.clone(), repo.clone(), branch.clone());
        match action.as_str() {
            "delete" => {
                let _ = Command::new("git")
                    .arg("tag")
                    .arg("-d")
                    .arg(tag_name)
                    .status();

                let _ = Command::new("git")
                    .arg("push")
                    .arg("origin")
                    .arg("--delete")
                    .arg(tag_name)
                    .status();
            }
            "release" => {
                let status = Command::new("git")
                    .arg("tag")
                    .arg("-s")
                    .arg(tag_name)
                    .arg("-m")
                    .arg(tag_name)
                    .status();

                if status.is_err() {
                    panic!(
                        "Error creating release tag {}: {}",
                        tag_name,
                        status.unwrap_err()
                    );
                }

                let status = Command::new("git")
                    .arg("push")
                    .arg("origin")
                    .arg(tag_name)
                    .status();

                if status.is_err() {
                    panic!(
                        "Error pushing release tag {}: {}",
                        tag_name,
                        status.unwrap_err()
                    );
                }
            }
            "update" => {
                let status = Command::new("git")
                    .arg("fetch")
                    .arg("upstream")
                    .arg("--tags")
                    .arg("--force")
                    .status();

                if status.is_err() {
                    panic!("Error fetching upstream tags: {}", status.unwrap_err());
                }

                let status = Command::new("git")
                    .arg("rebase")
                    .arg("--onto")
                    .arg(&aosp_tag)
                    .arg(&aosp_tag_old)
                    .status();

                if status.is_err() {
                    panic!("Error rebasing: {}", status.unwrap_err());
                }

                let status = Command::new("git").arg("push").arg("-f").status();

                if status.is_err() {
                    panic!("Error pushing changes: {}", status.unwrap_err());
                }
            }
            "default" => {
                default(repo.clone(), branch.clone());
            }
            _ => {}
        }

        if action == "init" {
            let status = Command::new("git")
                .arg("remote")
                .arg("add")
                .arg("upstream")
                .arg(format!(
                    "https://android.googlesource.com/{}",
                    repo.replace('_', "/")
                ))
                .status();

            if status.is_err() {
                panic!(
                    "Error adding upstream for {}: {}",
                    repo,
                    status.unwrap_err()
                );
            }

            let status = Command::new("git")
                .arg("fetch")
                .arg("upstream")
                .arg("--tags")
                .status();

            if status.is_err() {
                panic!(
                    "Error fetching upstream tags for {}: {}",
                    repo,
                    status.unwrap_err()
                );
            }

            let status = Command::new("gh")
                .arg("repo")
                .arg("set-default")
                .arg("origin")
                .status();

            if status.is_err() {
                panic!(
                    "Error setting default github repo for {}: {}",
                    repo,
                    status.unwrap_err()
                );
            }
        }

        let status = env::set_current_dir("..");
        if status.is_err() {
            panic!(
                "Failed to change back to parent directory: {}",
                status.unwrap_err()
            );
        }
    }

    for repo in grapheneos_forks {
        println!(">>> Handling {}", repo);
        init(action.clone(), repo.clone(), branch.clone());
        match action.as_str() {
            "delete" => {
                let _ = Command::new("git")
                    .arg("tag")
                    .arg("-d")
                    .arg(tag_name)
                    .status();

                let _ = Command::new("git")
                    .arg("push")
                    .arg("origin")
                    .arg("--delete")
                    .arg(tag_name)
                    .status();
            }
            "release" => {
                let status = Command::new("git")
                    .arg("tag")
                    .arg("-s")
                    .arg(tag_name)
                    .arg("-m")
                    .arg(tag_name)
                    .status();

                if status.is_err() {
                    panic!(
                        "Error creating release tag {}: {}",
                        tag_name,
                        status.unwrap_err()
                    );
                }

                let status = Command::new("git")
                    .arg("push")
                    .arg("origin")
                    .arg(tag_name)
                    .status();

                if status.is_err() {
                    panic!(
                        "Error pushing release tag {}: {}",
                        tag_name,
                        status.unwrap_err()
                    );
                }

                if repo == "platform_frameworks_base" {
                    // trigger SettingsLib release script
                    let status = Command::new("gh")
                    .arg("workflow")
                    .arg("run")
                    .arg("build.yml")
                    .status();

                    if status.is_err() {
                        panic!("Error releasing SettingsLib: {}", status.unwrap_err());
                    }
                }
            }
            "update" => {
                let status = Command::new("git")
                    .arg("fetch")
                    .arg("upstream")
                    .arg("--tags")
                    .arg("--force")
                    .status();

                if status.is_err() {
                    panic!("Error fetching upstream tags: {}", status.unwrap_err());
                }

                let status = Command::new("git")
                    .arg("rebase")
                    .arg("--onto")
                    .arg(&graphene_tag)
                    .arg(&graphene_tag_old)
                    .status();

                if status.is_err() {
                    panic!("Error rebasing {}: {}", repo, status.unwrap_err());
                }

                if repo == "platform_packages_inputmethods_LatinIME" {
                    let status = Command::new("git")
                        .arg("diff")
                        .arg("--quiet")
                        .arg("HEAD")
                        .arg("origin")
                        .status();

                    let update_libmozc = if let Ok(status) = status {
                        if status.success() { 1 } else { 0 }
                    } else {
                        0 // assume rebase if diff died
                    };

                    println!("MANUALLY_UPDATE_LIBMOZC={}", update_libmozc);

                    // trigger libmozc.so update script if there is no changes in rebase, this means that libmozc.so and its data files will always be up to date
                    if update_libmozc == 1 {
                        let status = Command::new("gh")
                            .arg("workflow")
                            .arg("run")
                            .arg("build.yml")
                            .status();

                        if status.is_err() {
                            panic!("Error updating libmozc: {}", status.unwrap_err());
                        }
                    }
                }

                let status = Command::new("git").arg("push").arg("-f").status();

                if status.is_err() {
                    panic!(
                        "Error pushing changes for {}: {}",
                        repo,
                        status.unwrap_err()
                    );
                }
            }
            "default" => {
                default(repo.clone(), branch.clone());
            }
            _ => {}
        }
        init2_graphene(action.clone(), repo.clone());
        let status = env::set_current_dir("..");
        if status.is_err() {
            panic!(
                "Failed to change back to parent directory: {}",
                status.unwrap_err()
            );
        }
    }

    for repo in grapheneos_app_forks {
        println!(">>> Handling {}", repo);
        // bupdate is not supported for app_forks, treat as update during init
        init_disable_bupdate(action.clone(), repo.clone());
        match action.as_str() {
            "update" => {
                let status = Command::new("git")
                    .arg("fetch")
                    .arg("upstream")
                    .arg("--tags")
                    .arg("--force")
                    .status();

                if status.is_err() {
                    panic!("Error fetching upstream tags: {}", status.unwrap_err());
                }

                let status = Command::new("git")
                    .arg("rebase")
                    .arg(scripts::get_latest_tag())
                    .status();

                if status.is_err() {
                    panic!("Error rebasing {}: {}", repo, status.unwrap_err());
                }

                let status = Command::new("git").arg("push").arg("-f").status();

                if status.is_err() {
                    panic!(
                        "Error pushing changes for {}: {}",
                        repo,
                        status.unwrap_err()
                    );
                }
            }
            _ => {}
        }
        init2_graphene(action.clone(), repo.clone());
        let status = env::set_current_dir("..");
        if status.is_err() {
            panic!(
                "Failed to change back to parent directory: {}",
                status.unwrap_err()
            );
        }
    }

    for repo in independent {
        if repo == "misc" || repo == "jOS_j-lib" {
            println!(">>> Skipping {}", repo);
            continue
        }
        println!(">>> Handling {}", repo);
        if repo != "jOS-Updates" {
            init(action.clone(), repo.clone(), branch.clone());
        } else {
            // bupdate is not supported for release server, treat as update during init
            init_disable_bupdate(action.clone(), repo.clone());
        }
        match action.as_str() {
            "delete" => {
                let _ = Command::new("git")
                    .arg("tag")
                    .arg("-d")
                    .arg(tag_name)
                    .status();

                let _ = Command::new("git")
                    .arg("push")
                    .arg("origin")
                    .arg("--delete")
                    .arg(tag_name)
                    .status();
            }
            "release" => {
                if repo == "jOS-Updates" {
                    for build in &builds {
                        let split: Vec<&str> = build.split('-').collect();
                        let device = get_device(split[0].to_string());
                        let build_type = get_build_type(split[1].to_string());
                        let channel: String;
                        if build_type == BuildType::USER {
                            channel = "stable".to_string();
                            // copy beta anyway, just in case
                            let status = Command::new("cp")
                                .arg("-T")
                                .arg(format!("{}{}-beta", format!(
                                    "../../grapheneos/releases/{}/release-{}-{}/",
                                    tag_name, device, tag_name
                                ), device))
                                .arg(format!("{}-beta", device))
                                .status();

                            if status.is_err() {
                                panic!(
                                    "Error copying beta release for {}: {}",
                                    device,
                                    status.unwrap_err()
                                );
                            }
                        } else if build_type == BuildType::UserDebug {
                            channel = "beta".to_string();
                        } else {
                            // no idea how tf this happened, fuck it, set to beta
                            channel = "beta".to_string();
                        }
                        let status = Command::new("cp")
                            .arg("-T")
                            .arg(format!("{}{}-{}", format!(
                                "../../grapheneos/releases/{}/release-{}-{}/",
                                tag_name, device, tag_name
                            ), device, channel))
                            .arg(format!("{}-{}", device, channel))
                            .status();

                        if status.is_err() {
                            panic!(
                                "Error copying {} release for {}: {}",
                                channel,
                                device,
                                status.unwrap_err()
                            );
                        }
                    }

                    let status = Command::new("git").arg("add").arg(".").status();

                    if status.is_err() {
                        panic!("Error adding files: {}", status.unwrap_err());
                    }

                    let status = Command::new("git")
                        .arg("commit")
                        .arg("-m")
                        .arg("add new version information")
                        .status();

                    if status.is_err() {
                        panic!(
                            "Error committing files: {}",
                            status.unwrap_err()
                        );
                    }

                    let status = Command::new("git").arg("push").status();

                    if status.is_err() {
                        panic!(
                            "Error pushing changes: {}",
                            status.unwrap_err()
                        );
                    }

                    let mut release = Command::new("gh");
                    release.arg("release")
                        .arg("create")
                        .arg(tag_name)
                        .arg("--latest=true")
                        .arg("--notes-file")
                        .arg("./release-notes.txt");

                    for build in &builds {
                        let split: Vec<&str> = build.split('-').collect();
                        let device = get_device(split[0].to_string());
                        release.arg(format!("{}{}-ota_update-{}.zip", format!(
                            "../../grapheneos/releases/{}/release-{}-{}/",
                            tag_name, device, tag_name
                        ), device, tag_name))
                            .arg(format!("{}{}-install-{}.zip", format!(
                                "../../grapheneos/releases/{}/release-{}-{}/",
                                tag_name, device, tag_name
                            ), device, tag_name));
                    }

                    let status = release.status();

                    if status.is_err() {
                        panic!(
                            "Error creating release: {}",
                            status.unwrap_err()
                        );
                    }
                } else {
                    if repo == "platform_manifest" {
                        let status = Command::new("git")
                            .arg("checkout")
                            .arg("-B")
                            .arg("tmp")
                            .status();

                        if status.is_err() {
                            panic!(
                                "Error checking out tmp branch for {}: {}",
                                repo,
                                status.unwrap_err()
                            );
                        }

                        let data =
                            fs::read_to_string(env::current_dir()?.join("default.xml"))?;
                        let mut from = "heads/".to_string();
                        let mut to = "tags/".to_string();
                        from.push_str(&branch);
                        to.push_str(tag_name);
                        let new = data.replace(&from, &to);
                        let status =
                            fs::write(env::current_dir()?.join("default.xml"), &new);

                        if status.is_err() {
                            panic!(
                                "Error updating default.xml for {}: {}",
                                repo,
                                status.unwrap_err()
                            );
                        }

                        let status = Command::new("git")
                            .arg("commit")
                            .arg("default.xml")
                            .arg("-m")
                            .arg(tag_name)
                            .status();

                        if status.is_err() {
                            panic!(
                                "Error committing default.xml for {}: {}",
                                repo,
                                status.unwrap_err()
                            );
                        }

                        let status = Command::new("git")
                            .arg("push")
                            .arg("-fu")
                            .arg("origin")
                            .arg("tmp")
                            .status();

                        if status.is_err() {
                            panic!(
                                "Error pushing tmp branch for {}: {}",
                                repo,
                                status.unwrap_err()
                            );
                        }
                    }
                    let status = Command::new("git")
                        .arg("tag")
                        .arg("-s")
                        .arg(tag_name)
                        .arg("-m")
                        .arg(tag_name)
                        .status();

                    if status.is_err() {
                        panic!("Error tagging {}: {}", repo, status.unwrap_err());
                    }

                    let status = Command::new("git")
                        .arg("push")
                        .arg("origin")
                        .arg(tag_name)
                        .status();

                    if status.is_err() {
                        panic!("Error pushing tag {}: {}", repo, status.unwrap_err());
                    }
                }
            }
            "default" => {
                if repo != "jOS-Updates" {
                    default(repo.clone(), branch.clone());
                }
            }
            _ => {}
        }

        let status = env::set_current_dir("..");
        if status.is_err() {
            panic!(
                "Failed to change back to parent directory: {}",
                status.unwrap_err()
            );
        }
    }

    if action == "bupdate" {
        let status = Command::new("misc/aosp/manage").arg("update").status();

        if status.is_err() {
            panic!("Error running update: {}", status.unwrap_err());
        }

        let status = Command::new("misc/aosp/manage").arg("default").status();

        if status.is_err() {
            panic!("Error running default: {}", status.unwrap_err());
        }
    } else if action == "update" || action == "manifest_only" {
        println!(">>> Generating Manifest");
        GenerateManifest::run(config, graphene_tag, branch)?;
    }
    Ok(())
}
