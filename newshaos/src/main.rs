use axum::{
    Json, Router,
    response::{IntoResponse, Response},
    routing::get,
};
use chrono::Local;
use serde::{Deserialize, Serialize};
use std::cmp::Reverse;
use string_builder::Builder;

#[derive(Debug, Deserialize)]
struct ArticlesResponse {
    data: Vec<Article>,
}

#[derive(Debug, Deserialize)]
struct Article {
    headline_no: String,
    updated_at: String,
    title: String,
    breaking: bool,
}

#[derive(Serialize)]
struct NewsResponse {
    headlines: String,
}

#[tokio::main]
async fn main() {
    let app = Router::new().route("/get-news", get(get_news));

    let listener = tokio::net::TcpListener::bind("0.0.0.0:8088")
        .await
        .expect("failed to bind to 0.0.0.0:8088");

    axum::serve(listener, app)
        .await
        .expect("server error");
}

async fn get_news() -> Response {
    let time = Local::now();
    let resp = reqwest::get("https://api.nhkworld.jp/nwapi/news/v1/en/articles?type=news&tag=&offset=0&limit=22").await.unwrap();
    let articles_response: ArticlesResponse =
        serde_json::from_str(&resp.text().await.unwrap())
            .map_err(|e| format!("JSON parse error: {}", e)).unwrap();
    let mut usable_data = articles_response.data;
    usable_data.sort_by_key(|article| Reverse(article.updated_at.parse::<i128>().unwrap()));
    usable_data.sort_by_key(|article| article.headline_no.clone());
    usable_data.sort_by_key(|article| Reverse(article.breaking));
    let mut builder = Builder::default();
    if !usable_data.is_empty() {
        builder.append(format!("Here is the headlines from NHK World at {}: ", time.format("%Y-%m-%d %H:%M:%S")));
        builder.append(".\n\n");
        for article in usable_data {
            if article.breaking {
                builder.append("BREAKING: ");
            }
            builder.append(article.title);
            builder.append(".\n\n");
        }
    }
    Json(NewsResponse {
        headlines: builder.string().unwrap().strip_suffix(".\n\n").unwrap().to_string()
    }).into_response()
}
