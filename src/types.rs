use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct Tile {
    pub content: String,
    pub properties: TileProperties,
}

#[derive(Serialize, Deserialize, Debug, Clone, Default)]
pub struct TileProperties {
    pub writability: Option<i32>,
    pub color: Option<Vec<i32>>,
    pub bgcolor: Option<Vec<i32>>,
    pub cell_props: Option<Value>,
}

#[derive(Debug, Clone, Copy, Default)]
pub struct CursorPosition {
    pub tile_x: i32,
    pub tile_y: i32,
    pub char_x: i32,
    pub char_y: i32,
}

#[derive(Debug, Default)]
pub struct OwotState {
    pub tiles: HashMap<String, Tile>,
    pub channel_id: Option<String>,
}

impl OwotState {
    pub fn tile_key(tile_x: i32, tile_y: i32) -> String {
        format!("{tile_y},{tile_x}")
    }
}
