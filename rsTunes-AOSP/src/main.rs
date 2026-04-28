use std::io;

use crossterm::event::{self, Event, KeyCode, KeyEvent, KeyEventKind};
use ratatui::{
    style::{Stylize, Color, Modifier},
    symbols::border,
    text::Line,
    widgets::{Block, List, ListState, ListItem},
    DefaultTerminal, Frame,
};
use tokio::runtime::Runtime;
use mtp_rs::mtp::Storage;
use mtp_rs::StorageId;
use mtp_rs::NewObjectInfo;
use mtp_rs::ObjectHandle;
use bytes::Bytes;
use futures::StreamExt;

fn main() -> io::Result<()> {
    let rt = Runtime::new().unwrap();
    let _guard = rt.enter();
    ratatui::run(|terminal| App::default().run(terminal))
}

#[derive(Default)]
pub struct App {
    exit: bool,
    list_state: ListState,
    devices: Vec<mtp_rs::mtp::MtpDeviceInfo>,
    device: Option<mtp_rs::mtp::MtpDevice>,
    state: AppState,
    log: Vec<String>,
    rx: Option<tokio::sync::mpsc::Receiver<SyncEvent>>,
}

enum SyncEvent {
    Message(String),
    Complete,
}

#[derive(Default, PartialEq)]
enum AppState {
    #[default] SelectingDevice,
    Syncing,
}

impl App {
    pub fn run(&mut self, terminal: &mut DefaultTerminal) -> io::Result<()> {
        self.state = AppState::SelectingDevice;
        let mut last_tick = std::time::Instant::now();
        let tick_rate = std::time::Duration::from_millis(100);
        let usb_poll_rate = std::time::Duration::from_secs(2);
        let mut last_usb_poll = std::time::Instant::now();
        if let Ok(new_devices) = mtp_rs::mtp::MtpDevice::list_devices() {
            self.devices = new_devices;
        }
        self.list_state = ListState::default().with_selected(Some(0));
        while !self.exit {
            terminal.draw(|frame| self.draw(frame))?;
            if let Some(ref mut rx) = self.rx {
                while let Ok(event) = rx.try_recv() {
                    match event {
                        SyncEvent::Message(msg) => {
                            self.log.push(msg);
                        }
                        SyncEvent::Complete => {
                            std::thread::sleep(std::time::Duration::from_secs(1));
                            // Future Me's issue solved: 
                            // Wait a second so the user can see "Finished!" then exit.
                            self.exit = true; 
                        }
                    }
                }
            }
            if self.state == AppState::SelectingDevice {
                let timeout = tick_rate
                    .checked_sub(last_tick.elapsed())
                    .unwrap_or_default();

                if event::poll(timeout)? {
                    self.handle_events()?;
                }

                if last_tick.elapsed() >= tick_rate {
                    last_tick = std::time::Instant::now();
                }

                if last_usb_poll.elapsed() >= usb_poll_rate {
                    if let Ok(new_devices) = mtp_rs::mtp::MtpDevice::list_devices() {
                        self.devices = new_devices;
                        // TODO: Add logic here to fix list_state if the selected device vanishes
                    }
                    last_usb_poll = std::time::Instant::now();
                }
            }
        }
        Ok(())
    }

    fn draw(&mut self, frame: &mut Frame) {
        let title = Line::from(" rsTunes-AOSP ".bold());
        let block = if self.state == AppState::SelectingDevice {
            let instructions = Line::from(vec![
                " Move Selection ".into(),
                "<Up> <Down>".blue().bold(),
                " Select Device ".into(),
                "<Enter>".blue().bold(),
                " Quit ".into(),
                "<Q> ".blue().bold(),
            ]);
            Block::bordered()
                .title(title.centered())
                .title_bottom(instructions.centered())
                .border_set(border::THICK)
        } else {
            Block::bordered()
                .title(title.centered())
                .border_set(border::THICK)
        };

        if self.state == AppState::SelectingDevice {
            let device_names: Vec<String> = self.devices
                .iter()
                .map(|d| format!("{} - {} - {}", d.manufacturer.clone().unwrap(), d.product.clone().unwrap(), d.serial_number.clone().unwrap_or("".to_string())))
                .collect();
            let list = List::new(device_names)
                .style(Color::White)
                .highlight_style(Modifier::REVERSED)
                .block(block)
                .highlight_symbol("> ");

            frame.render_stateful_widget(list, frame.area(), &mut self.list_state);
        } else {
            let list_items: Vec<ListItem> = self.log
                .iter()
                .map(|i| ListItem::new(i.to_string()))
                .collect();

            let list = List::new(list_items)
                .block(block);

            frame.render_stateful_widget(list, frame.area(), &mut self.list_state);

            let last_index = self.log.len().saturating_sub(1);
            self.list_state.select(Some(last_index));
        }
    }

    /// updates the application's state based on user input
    fn handle_events(&mut self) -> io::Result<()> {
        match event::read()? {
            // it's important to check that the event is a key press event as
            // crossterm also emits key release and repeat events on Windows.
            Event::Key(key_event) if key_event.kind == KeyEventKind::Press => {
                self.handle_key_event(key_event)
            }
            _ => {}
        };
        Ok(())
    }

    fn handle_key_event(&mut self, key_event: KeyEvent) {
        match key_event.code {
            KeyCode::Char('q') => self.exit(),
            KeyCode::Up => self.list_state.select_previous(),
            KeyCode::Down => self.list_state.select_next(),
            KeyCode::Enter => {
                let handle = tokio::runtime::Handle::current();
                self.state = AppState::Syncing;
                self.device = Some(handle.block_on(mtp_rs::mtp::MtpDevice::open_by_serial(self.devices[self.list_state.selected().unwrap()].serial_number.clone().unwrap().as_str())).unwrap());
                self.copy();
            },
            _ => {}
        }
    }

    fn exit(&mut self) {
        self.exit = true;
    }

    fn copy(&mut self) {
        let (tx, rx) = tokio::sync::mpsc::channel::<SyncEvent>(100);
        self.rx = Some(rx);
        let music_dir = dirs::home_dir()
            .unwrap()
            .join("Music");
        let device = self.device.take().expect("Device not opened!");
        tokio::spawn(async move {
            let _ = tx.send(SyncEvent::Message(format!("Scanning: {}", music_dir.display()))).await;
            let binding = device.storages().await.unwrap();
            let storage = binding.first().unwrap(); // modern android devices should only have one mtp device, that being its internal storage
            let parent_id = find_music_folder_id(&storage).await;
            if parent_id == StorageId(0xffffffff) {
                return Err::<(), String>("Could not find Music folder on phone".into());
            }

            let all_files = storage.list_objects(Some(get_object_handler_for_music(&storage).await)).await.unwrap();
            for file in all_files.iter() {
                let _ = tx.send(SyncEvent::Message(format!("Removing: {}", file.filename)));
                tokio::time::sleep(std::time::Duration::from_millis(250)).await;
                storage.delete(file.handle).await.map_err(|e| e.to_string())?;
            }

            if let Ok(entries) = std::fs::read_dir(music_dir) {
                futures::stream::iter(entries.flatten())
                .for_each_concurrent(10, |entry| {
                    let tx = tx.clone();
                    async move {
                        let path = entry.path();
                        if path.is_file() {
                            let filename = path.file_name().unwrap().to_string_lossy();
                    
                            // Tell the UI what we are doing
                            let _ = tx.send(SyncEvent::Message(format!("Syncing: {}", filename))).await;
                            //tokio::time::sleep(std::time::Duration::from_millis(250)).await;
                    
                            // THE ACTUAL BLOCKING MTP CALL
                            // Since libmtp is synchronous, we wrap it in spawn_blocking
                            let content = std::fs::read(path.to_str().unwrap()).unwrap();
                            let info = NewObjectInfo::file(filename, content.len() as u64);
                            let stream = futures::stream::iter(vec![Ok::<_, std::io::Error>(Bytes::from(content))]);
                            let _ = storage.upload(Some(get_object_handler_for_music(&storage).await), info, Box::pin(stream)).await.unwrap();
                        }
                    }
                }).await;
            }
            let _ = tx.send(SyncEvent::Complete).await;
            Ok(())
        });
    }
}

pub async fn get_object_handler_for_music(device: &Storage) -> ObjectHandle {
    // Usually, 0 is the root/parent of the storage
    let files = device.list_objects(None).await.unwrap();

    // Look for a folder named "Music"
    files.iter()
    .find(|f| f.filename.to_lowercase() == "music" && f.is_folder())
    .unwrap()
    .handle
}

pub async fn find_music_folder_id(device: &Storage) -> StorageId {
    // Usually, 0 is the root/parent of the storage
    let files = device.list_objects(None).await.unwrap();

    // Look for a folder named "Music"
    files.iter()
    .find(|f| f.filename.to_lowercase() == "music" && f.is_folder())
    .map(|f| f.storage_id)
    .unwrap_or(StorageId(0xffffffff)) // Return a "not found" sentinel
}
