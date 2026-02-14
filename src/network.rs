use crate::types::{CursorPosition, OwotState, Tile, TileProperties};
use futures_util::{SinkExt, StreamExt};
use serde_json::{json, Value};
use std::sync::{Arc, Mutex};
use tokio::sync::mpsc::UnboundedReceiver;
use tokio_tungstenite::tungstenite::Message;

pub const BASE_URL: &str = "ourworldoftext.com";

#[derive(Debug, Clone)]
pub struct Edit {
    pub tile_x: i32,
    pub tile_y: i32,
    pub char_x: i32,
    pub char_y: i32,
    pub timestamp: i64,
    pub character: String,
    pub edit_id: i64,
    pub color: i32,
    pub bgcolor: i32,
}

#[derive(Debug, Clone)]
pub enum NetworkMessage {
    Fetch {
        min_x: i32,
        min_y: i32,
        max_x: i32,
        max_y: i32,
    },
    Write {
        edits: Vec<Edit>,
    },
    Cursor {
        position: CursorPosition,
    },
}

pub fn spawn_network_task(
    world_name: String,
    state: Arc<Mutex<OwotState>>,
    mut outbound: UnboundedReceiver<NetworkMessage>,
) {
    std::thread::spawn(move || {
        let runtime = tokio::runtime::Runtime::new().expect("tokio runtime");
        runtime.block_on(async move {
            let ws_url = format!("wss://{BASE_URL}/{world_name}/ws");
            let (ws_stream, _) = match tokio_tungstenite::connect_async(&ws_url).await {
                Ok(result) => result,
                Err(error) => {
                    log::error!("WebSocket connection failed: {error}");
                    return;
                }
            };
            let (mut ws_writer, mut ws_reader) = ws_stream.split();

            let state_reader = state.clone();
            let read_task = tokio::spawn(async move {
                while let Some(message) = ws_reader.next().await {
                    match message {
                        Ok(Message::Text(text)) => {
                            handle_incoming(&state_reader, &text);
                        }
                        Ok(Message::Binary(_)) => {}
                        Ok(Message::Close(_)) => break,
                        Ok(_) => {}
                        Err(error) => {
                            log::error!("WebSocket read error: {error}");
                            break;
                        }
                    }
                }
            });

            let write_task = tokio::spawn(async move {
                while let Some(message) = outbound.recv().await {
                    let payload = match message {
                        NetworkMessage::Fetch {
                            min_x,
                            min_y,
                            max_x,
                            max_y,
                        } => json!({
                            "kind": "fetch",
                            "fetchRectangles": [{
                                "minX": min_x,
                                "minY": min_y,
                                "maxX": max_x,
                                "maxY": max_y,
                            }]
                        }),
                        NetworkMessage::Write { edits } => {
                            let edits_payload: Vec<Value> = edits
                                .into_iter()
                                .map(|edit| {
                                    json!([
                                        edit.tile_y,
                                        edit.tile_x,
                                        edit.char_y,
                                        edit.char_x,
                                        edit.timestamp,
                                        edit.character,
                                        edit.edit_id,
                                        edit.color,
                                        edit.bgcolor,
                                    ])
                                })
                                .collect();
                            json!({
                                "kind": "write",
                                "edits": edits_payload
                            })
                        }
                        NetworkMessage::Cursor { position } => json!({
                            "kind": "cursor",
                            "position": {
                                "tileX": position.tile_x,
                                "tileY": position.tile_y,
                                "charX": position.char_x,
                                "charY": position.char_y,
                            }
                        }),
                    };

                    if let Err(error) = ws_writer
                        .send(Message::Text(payload.to_string()))
                        .await
                    {
                        log::error!("WebSocket send error: {error}");
                        break;
                    }
                }
            });

            tokio::select! {
                _ = read_task => {},
                _ = write_task => {},
            }
        });
    });
}

fn handle_incoming(state: &Arc<Mutex<OwotState>>, text: &str) {
    let payload: Value = match serde_json::from_str(text) {
        Ok(value) => value,
        Err(error) => {
            log::error!("Failed to parse WS message: {error}");
            return;
        }
    };

    let kind = payload
        .get("kind")
        .and_then(Value::as_str)
        .unwrap_or("unknown");

    match kind {
        "channel" => {
            if let Some(sender) = payload.get("sender").and_then(Value::as_str) {
                if let Ok(mut state) = state.lock() {
                    state.channel_id = Some(sender.to_string());
                }
            }
        }
        "fetch" => {
            if let Some(tiles) = payload.get("tiles") {
                apply_tiles(state, tiles);
            }
        }
        "tile" | "tileUpdate" | "write" => {
            if let Some(tiles) = payload.get("tiles") {
                apply_tiles(state, tiles);
            } else if let Some(tile) = payload.get("tile") {
                apply_tiles(state, tile);
            }
        }
        _ => {}
    }
}

fn apply_tiles(state: &Arc<Mutex<OwotState>>, tiles: &Value) {
    let mut guard = match state.lock() {
        Ok(guard) => guard,
        Err(_) => return,
    };

    if let Some(tile_array) = tiles.as_array() {
        for tile_entry in tile_array {
            insert_tile(&mut guard, tile_entry);
        }
    } else if tiles.is_object() {
        insert_tile(&mut guard, tiles);
    }
}

fn insert_tile(state: &mut OwotState, tile_entry: &Value) {
    let tile_x = tile_entry
        .get("tileX")
        .and_then(Value::as_i64)
        .unwrap_or(0) as i32;
    let tile_y = tile_entry
        .get("tileY")
        .and_then(Value::as_i64)
        .unwrap_or(0) as i32;
    let content = tile_entry
        .get("content")
        .and_then(Value::as_str)
        .unwrap_or(" ")
        .to_string();
    let properties = tile_entry
        .get("properties")
        .cloned()
        .unwrap_or_else(|| json!({}));
    let properties: TileProperties = serde_json::from_value(properties).unwrap_or_default();

    let tile = Tile { content, properties };
    let key = OwotState::tile_key(tile_x, tile_y);
    state.tiles.insert(key, tile);
}

pub fn build_http_world_url(world_name: &str) -> String {
    format!("https://{BASE_URL}/{world_name}/")
}
