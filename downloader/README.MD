# Downloader

A simple (ish) rust program that downloads vocaloid songs

## IMPORTANT

This uses the [VocaDB](https://vocadb.net) API. Metadata is provided by VocaDB and is licensed under Creative Commons Attribution; if you redistribute metadata from this tool, please include a link back to VocaDB.

Please be moderate with your usage of this in case your device gets blocked by the API. For more information, see the [VocaDB API Usage Terms](https://wiki.vocadb.net/docs/public-api#api-usage-rules).

This tool downloads media; you are responsible for complying with the source site’s terms and copyright law.

## How to use

I do not ship precompiled binaries of this, so you will need to compile it yourself or run it via cargo

Compile:
```
$ cargo build
```

Run via Cargo (intended):
```
$ cargo run {path of file}
```

Run via binary:
```
$ ./downloader {path of file}
```

You will also need a file that you put the IDs in, you can use [test-songs.txt](test-songs.txt) as an example of how it is formatted

## License

see [LICENSE](../LICENSE)

## Third-Party Data

This project retrieves metadata from [VocaDB](https://vocadb.net). VocaDB database contents are licensed under Creative Commons Attribution (CC BY). See https://wiki.vocadb.net/docs/public-api for details.
