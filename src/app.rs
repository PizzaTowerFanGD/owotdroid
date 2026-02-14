use crate::network::{build_http_world_url, spawn_network_task, Edit, NetworkMessage};
use crate::types::{CursorPosition, OwotState, Tile, TileProperties};
use eframe::egui;
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};
use tokio::sync::mpsc::{unbounded_channel, UnboundedSender};
use unicode_segmentation::UnicodeSegmentation;

const TILE_WIDTH: i32 = 16;
const TILE_HEIGHT: i32 = 8;

pub struct OwotApp {
    state: Arc<Mutex<OwotState>>,
    outbound: UnboundedSender<NetworkMessage>,
    pos_x: f64,
    pos_y: f64,
    cursor: CursorPosition,
    write_buffer: Vec<Edit>,
    last_flush: Instant,
    last_fetch: Instant,
    next_edit_id: i64,
    cell_size: egui::Vec2,
}

impl OwotApp {
    pub fn new(world_name: String) -> Self {
        let state = Arc::new(Mutex::new(OwotState::default()));
        let (tx, rx) = unbounded_channel();
        spawn_network_task(world_name.clone(), Arc::clone(&state), rx);
        spawn_world_metadata_fetch(world_name.clone());

        Self {
            state,
            outbound: tx,
            pos_x: 0.0,
            pos_y: 0.0,
            cursor: CursorPosition::default(),
            write_buffer: Vec::new(),
            last_flush: Instant::now(),
            last_fetch: Instant::now(),
            next_edit_id: 1,
            cell_size: egui::vec2(12.0, 18.0),
        }
    }

    fn handle_drag(&mut self, ctx: &egui::Context) {
        let delta = ctx.input(|i| i.pointer.delta());
        if ctx.input(|i| i.pointer.is_down()) {
            self.pos_x -= delta.x as f64 / self.cell_size.x as f64;
            self.pos_y -= delta.y as f64 / self.cell_size.y as f64;
        }
    }

    fn handle_click(&mut self, ctx: &egui::Context, panel_rect: egui::Rect) {
        if let Some(pos) = ctx.input(|i| i.pointer.latest_pos()) {
            if ctx.input(|i| i.pointer.any_pressed()) && panel_rect.contains(pos) {
                let local = pos - panel_rect.min;
                let char_x = (local.x / self.cell_size.x + self.pos_x as f32).floor() as i32;
                let char_y = (local.y / self.cell_size.y + self.pos_y as f32).floor() as i32;
                let (tile_x, tile_y, local_x, local_y) = screen_to_tile(char_x, char_y);
                self.cursor = CursorPosition {
                    tile_x,
                    tile_y,
                    char_x: local_x,
                    char_y: local_y,
                };
                let _ = self.outbound.send(NetworkMessage::Cursor {
                    position: self.cursor,
                });
            }
        }
    }

    fn handle_text_input(&mut self, ctx: &egui::Context) {
        let events = ctx.input(|i| i.events.clone());
        for event in events {
            if let egui::Event::Text(text) = event {
                for grapheme in text.graphemes(true) {
                    if grapheme == "\n" || grapheme == "\r" {
                        continue;
                    }
                    self.apply_character(grapheme);
                }
            }
        }
    }

    fn apply_character(&mut self, grapheme: &str) {
        let tile_key = OwotState::tile_key(self.cursor.tile_x, self.cursor.tile_y);
        let mut guard = match self.state.lock() {
            Ok(guard) => guard,
            Err(_) => return,
        };
        let tile = guard
            .tiles
            .entry(tile_key)
            .or_insert_with(|| Tile {
                content: " ".repeat((TILE_WIDTH * TILE_HEIGHT) as usize),
                properties: TileProperties::default(),
            });

        let index = (self.cursor.char_y * TILE_WIDTH + self.cursor.char_x) as usize;
        let mut graphemes: Vec<&str> = tile.content.graphemes(true).collect();
        if graphemes.len() < (TILE_WIDTH * TILE_HEIGHT) as usize {
            graphemes.resize((TILE_WIDTH * TILE_HEIGHT) as usize, " ");
        }
        if index < graphemes.len() {
            graphemes[index] = grapheme;
        }
        tile.content = graphemes.concat();

        let edit = Edit {
            tile_x: self.cursor.tile_x,
            tile_y: self.cursor.tile_y,
            char_x: self.cursor.char_x,
            char_y: self.cursor.char_y,
            timestamp: chrono_timestamp(),
            character: grapheme.to_string(),
            edit_id: self.next_edit_id,
            color: 0,
            bgcolor: -1,
        };
        self.next_edit_id += 1;
        self.write_buffer.push(edit);

        self.cursor.char_x += 1;
        if self.cursor.char_x >= TILE_WIDTH {
            self.cursor.char_x = 0;
            self.cursor.char_y += 1;
            if self.cursor.char_y >= TILE_HEIGHT {
                self.cursor.char_y = 0;
                self.cursor.tile_x += 1;
            }
        }
    }

    fn flush_writes(&mut self) {
        if self.write_buffer.is_empty() {
            return;
        }
        if self.last_flush.elapsed() < Duration::from_millis(250) {
            return;
        }
        let edits = std::mem::take(&mut self.write_buffer);
        let _ = self.outbound.send(NetworkMessage::Write { edits });
        self.last_flush = Instant::now();
    }

    fn fetch_visible_tiles(&mut self, panel_rect: egui::Rect) {
        if self.last_fetch.elapsed() < Duration::from_millis(600) {
            return;
        }
        let chars_wide = (panel_rect.width() / self.cell_size.x).ceil() as i32 + 2;
        let chars_high = (panel_rect.height() / self.cell_size.y).ceil() as i32 + 2;
        let min_char_x = self.pos_x.floor() as i32;
        let min_char_y = self.pos_y.floor() as i32;
        let max_char_x = min_char_x + chars_wide;
        let max_char_y = min_char_y + chars_high;

        let min_tile_x = div_floor(min_char_x, TILE_WIDTH);
        let min_tile_y = div_floor(min_char_y, TILE_HEIGHT);
        let max_tile_x = div_floor(max_char_x, TILE_WIDTH);
        let max_tile_y = div_floor(max_char_y, TILE_HEIGHT);

        let _ = self.outbound.send(NetworkMessage::Fetch {
            min_x: min_tile_x,
            min_y: min_tile_y,
            max_x: max_tile_x,
            max_y: max_tile_y,
        });
        self.last_fetch = Instant::now();
    }

    fn render_tiles(&self, ui: &mut egui::Ui, panel_rect: egui::Rect) {
        let painter = ui.painter_at(panel_rect);
        let guard = match self.state.lock() {
            Ok(guard) => guard,
            Err(_) => return,
        };

        let chars_wide = (panel_rect.width() / self.cell_size.x).ceil() as i32 + 2;
        let chars_high = (panel_rect.height() / self.cell_size.y).ceil() as i32 + 2;
        let start_x = self.pos_x.floor() as i32;
        let start_y = self.pos_y.floor() as i32;

        for y in 0..chars_high {
            for x in 0..chars_wide {
                let char_x = start_x + x;
                let char_y = start_y + y;
                let (tile_x, tile_y, local_x, local_y) = screen_to_tile(char_x, char_y);
                let tile_key = OwotState::tile_key(tile_x, tile_y);
                let tile = guard.tiles.get(&tile_key);
                let screen_pos = panel_rect.min
                    + egui::vec2(
                        (char_x as f32 - self.pos_x as f32) * self.cell_size.x,
                        (char_y as f32 - self.pos_y as f32) * self.cell_size.y,
                    );

                let (char, bg_color) = match tile {
                    Some(tile) => {
                        let index = (local_y * TILE_WIDTH + local_x) as usize;
                        let graphemes: Vec<&str> = tile.content.graphemes(true).collect();
                        let character = graphemes
                            .get(index)
                            .copied()
                            .unwrap_or(" ")
                            .to_string();
                        let bg_color = tile
                            .properties
                            .bgcolor
                            .as_ref()
                            .and_then(|colors| colors.get(index))
                            .copied()
                            .unwrap_or(-1);
                        (character, bg_color)
                    }
                    None => ("·".to_string(), -1),
                };

                if bg_color >= 0 {
                    let rgb = color_from_int(bg_color as u32);
                    painter.rect_filled(
                        egui::Rect::from_min_size(screen_pos, self.cell_size),
                        0.0,
                        rgb,
                    );
                }

                painter.text(
                    screen_pos,
                    egui::Align2::LEFT_TOP,
                    char,
                    egui::FontId::monospace(self.cell_size.y),
                    egui::Color32::WHITE,
                );
            }
        }
    }
}

impl eframe::App for OwotApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        self.handle_drag(ctx);

        egui::CentralPanel::default().show(ctx, |ui| {
            let panel_rect = ui.max_rect();
            self.handle_click(ctx, panel_rect);
            self.handle_text_input(ctx);
            self.fetch_visible_tiles(panel_rect);
            self.render_tiles(ui, panel_rect);
        });

        self.flush_writes();
        ctx.request_repaint_after(Duration::from_millis(16));
    }
}

fn screen_to_tile(char_x: i32, char_y: i32) -> (i32, i32, i32, i32) {
    let tile_x = div_floor(char_x, TILE_WIDTH);
    let tile_y = div_floor(char_y, TILE_HEIGHT);
    let local_x = char_x - tile_x * TILE_WIDTH;
    let local_y = char_y - tile_y * TILE_HEIGHT;
    (tile_x, tile_y, local_x, local_y)
}

fn div_floor(value: i32, divisor: i32) -> i32 {
    let mut result = value / divisor;
    let remainder = value % divisor;
    if remainder < 0 {
        result -= 1;
    }
    result
}

fn color_from_int(color: u32) -> egui::Color32 {
    let r = ((color >> 16) & 0xFF) as u8;
    let g = ((color >> 8) & 0xFF) as u8;
    let b = (color & 0xFF) as u8;
    egui::Color32::from_rgb(r, g, b)
}

fn chrono_timestamp() -> i64 {
    use std::time::{SystemTime, UNIX_EPOCH};
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as i64)
        .unwrap_or(0)
}

fn spawn_world_metadata_fetch(world_name: String) {
    std::thread::spawn(move || {
        let url = build_http_world_url(&world_name);
        let runtime = tokio::runtime::Runtime::new().expect("tokio runtime");
        runtime.block_on(async move {
            if let Ok(response) = reqwest::get(&url).await {
                log::info!("Fetched world metadata: {}", response.status());
            }
        });
    });
}
