# Jade Plugin for Paper

A Paper server plugin that provides Jade server-side data to Fabric clients. When a player with the Jade mod connects, the plugin responds with entity and block information that the Jade HUD overlay displays.

## Requirements
- Paper 1.21.6 (26.1.2) or compatible
- Fabric client with Jade mod installed

## How It Works
The plugin intercepts Jade's custom network payloads using Paper's plugin messaging system. When a Fabric client joins:
1. Client sends `jade:client_handshake` with protocol version
2. Plugin responds with `jade:server_handshake` listing available data providers
3. Client periodically sends `jade:request_entity` / `jade:request_block`
4. Plugin queries the server world and responds with `jade:receive_data` (NBT format)

## Implemented Features

### Protocol (5/5)
- [x] `jade:client_handshake`
- [x] `jade:server_handshake`
- [x] `jade:request_entity`
- [x] `jade:request_block`
- [x] `jade:receive_data`

### Block Providers (15/19)
- [x] `minecraft:brewing_stand` — Fuel + brew time (server)
- [x] `minecraft:beehive` — Bees + honey level (server)
- [x] `minecraft:command_block` — Command text (server)
- [x] `minecraft:hopper_lock` — Lock status (server)
- [x] `minecraft:redstone` — Signal strength (server)
- [x] `minecraft:mob_spawner.cooldown` — Trial spawner cooldown (server)
- [x] `minecraft:enchantment_power` — Enchanting table power (client)
- [x] `minecraft:total_enchantment_power` — Total enchantment power (client)
- [x] `minecraft:player_head` — Player head owner (client)
- [x] `minecraft:note_block` — Note pitch (client)
- [x] `minecraft:crop_progress` — Crop growth % (client)
- [x] `minecraft:mob_spawner` — Spawner entity type (client)
- [x] `minecraft:tnt_stability` — TNT stability (client)
- [x] `minecraft:harvest_tool` — Required harvest tool (client)
- [x] `minecraft:item_ber` — Block entity render item (client)
- [ ] `minecraft:jukebox` — Playing record
- [ ] `minecraft:lectern` — Book name
- [ ] `minecraft:furnace` — Smelting progress + inventory
- [ ] `minecraft:shelf` — Chiseled bookshelf slot

### Entity Providers (12/14)
- [x] `minecraft:entity_health` — Absorption hearts (server) / health bar (client)
- [x] `minecraft:entity_armor` — Armor bar (client)
- [x] `minecraft:animal_owner` — Tamed animal owner name (server)
- [x] `minecraft:mob_growth` — Baby animal time until adult (server)
- [x] `minecraft:mob_breeding` — Breeding cooldown / "Fed" indicator (server)
- [x] `minecraft:waxed` — Copper Golem waxed status (server)
- [x] `minecraft:zombie_villager` — Conversion timer (server)
- [ ] `minecraft:potion_effects` — Active potion effects
- [ ] `minecraft:next_entity_drop` — Next egg/scute/etc. timer
- [x] `minecraft:horse_stats` — Horse jump/speed (client)
- [x] `minecraft:item_frame` — Item frame contents (client)
- [x] `minecraft:painting` — Painting variant (client)
- [x] `minecraft:pet_armor` — Horse armor equipped (client)
- [x] `minecraft:villager_profession` — Villager profession (client)

### Accessibility Providers (0/9)
- [ ] `jade_access:sign` — Sign text
- [ ] `jade_access:block` — Block details
- [ ] `jade_access:block_body` — Block details body
- [ ] `jade_access:block_amount` — Block amount
- [ ] `jade_access:entity` — Entity details
- [ ] `jade_access:entity_body` — Entity details body
- [ ] `jade_access:entity_variant` — Entity variant
- [ ] `jade_access:held_item` — Held item
- [ ] `jade_access:npc_description` — NPC description

### Debug Providers (0/5)
- [X] `jade:registry_name` — Registry name
- [X] `jade:block_properties` — Block properties
- [X] `jade:block_states` — Block states
- [X] `jade:loot_table` — Block loot table
- [X] `jade:entity_attributes` — Entity attributes

## Build
```
./gradlew build
```
Output: `build/libs/JadePlugin-26.1.0.jar`

Requires JDK 21+ to build.

## Credits
- Jade mod by Snownee (https://github.com/Snownee/Jade)
- Plugin by Jonyboylovespie
