use chrono::{Datelike, Utc, Timelike};
use chrono_tz::{Asia::Tokyo, Europe::London};
use std::env;
use std::{fs, path::PathBuf};

fn read_tz() -> String {
    let path = PathBuf::from(
        std::env::var("HOME").unwrap() +
        "/.config/lockdate.conf"
    );

    let content = fs::read_to_string(path).unwrap_or_else(|_| "tz=jp".to_string());

    for line in content.lines() {
        if let Some(v) = line.strip_prefix("tz=") {
            return v.trim().to_string();
        }
    }

    "jp".to_string()
}

fn set_tz(value: &str) {
    let path = PathBuf::from(
        std::env::var("HOME").unwrap() +
        "/.config/lockdate.conf"
    );

    fs::write(path, format!("tz={}\n", value))
        .expect("failed to write config");
}

fn japanese_era(year: i32) -> (&'static str, i32) {
    if year >= 2019 {
        ("令和", year - 2018)
    } else if year >= 1989 {
        ("平成", year - 1988)
    } else if year >= 1926 {
        ("昭和", year - 1925)
    } else {
        ("", year)
    }
}

fn english_weekday(day: chrono::Weekday) -> &'static str {
    match day {
        chrono::Weekday::Mon => "Monday",
        chrono::Weekday::Tue => "Tuesday",
        chrono::Weekday::Wed => "Wednesday",
        chrono::Weekday::Thu => "Thursday",
        chrono::Weekday::Fri => "Friday",
        chrono::Weekday::Sat => "Saturday",
        chrono::Weekday::Sun => "Sunday",
    }
}

fn main() {
    let args: Vec<String> = env::args().collect();

    if args.len() > 1 {
        match args[1].as_str() {
            "--set-tz" => {
                if args.len() > 2 {
                    set_tz(&args[2]);
                    return;
                }
            }
            _ => {}
        }
    }

    let mut show_date = true;
    let mut show_time = true;

    let mode_jp = read_tz() == "jp";

    for arg in &args {
        match arg.as_str() {
            "--time" => show_date = false,
            "--date" => show_time = false,
            _ => {}
        }
    }

    let now = if mode_jp {
        Utc::now().with_timezone(&Tokyo)
    } else {
        Utc::now().with_timezone(&London)
    };

    if mode_jp {
        let (era, era_year) = japanese_era(now.year());

        let weekday = match now.weekday() {
            chrono::Weekday::Mon => "月曜日",
            chrono::Weekday::Tue => "火曜日",
            chrono::Weekday::Wed => "水曜日",
            chrono::Weekday::Thu => "木曜日",
            chrono::Weekday::Fri => "金曜日",
            chrono::Weekday::Sat => "土曜日",
            chrono::Weekday::Sun => "日曜日",
        };

        if show_date && show_time {
            println!(
                "{}{}年{}月{}日{} | {:02}:{:02}:{:02}",
                era,
                era_year,
                now.month(),
                now.day(),
                weekday,
                now.hour(),
                now.minute(),
                now.second()
            );
        } else if show_date {
            println!(
                "{}{}年{}月{}日{}",
                era,
                era_year,
                now.month(),
                now.day(),
                weekday
            );
        } else if show_time {
            println!(
                "{:02}:{:02}:{:02}",
                now.hour(),
                now.minute(),
                now.second()
            );
        }

    } else {
        let weekday = english_weekday(now.weekday());

        if show_date && show_time {
            println!(
                "{}, {:02}/{:02}/{:04} | {:02}:{:02}:{:02}",
                weekday,
                now.day(),
                now.month(),
                now.year(),
                now.hour(),
                now.minute(),
                now.second()
            );
        } else if show_date {
            println!(
                "{}, {:02}/{:02}/{:04}",
                weekday,
                now.day(),
                now.month(),
                now.year()
            );
        } else if show_time {
            println!(
                "{:02}:{:02}:{:02}",
                now.hour(),
                now.minute(),
                now.second()
            );
        }
    }
}
