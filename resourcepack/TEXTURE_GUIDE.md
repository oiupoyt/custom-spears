# CustomSpears — Resource Pack Texture Guide

## Folder Structure

```
resourcepack/
├── pack.mcmeta
└── assets/minecraft/
    ├── models/item/
    │   ├── netherite_spear.json          ← Override file (do not edit)
    │   └── custom_spears/
    │       ├── nightmare_spear.json
    │       ├── sanguine_spear.json
    │       ├── thunderspear.json
    │       ├── ashen_spear.json
    │       ├── phantom_spear.json
    │       └── fortune_spear.json
    └── textures/item/custom_spears/
        ├── nightmare_spear.png           ← Replace with your art
        ├── sanguine_spear.png            ← Replace with your art
        ├── thunderspear.png              ← Replace with your art
        ├── ashen_spear.png               ← Replace with your art
        ├── phantom_spear.png             ← Replace with your art
        └── fortune_spear.png             ← Replace with your art
```

## How to Add Your Textures

1. Open `resourcepack/assets/minecraft/textures/item/custom_spears/`
2. Replace each `.png` file with your own 16x16 (or 16x32, 16x48 for animated) texture
3. Keep the exact same filenames

## Texture Specs

- **Size**: 16x16 pixels recommended (standard Minecraft item texture)
- **Format**: PNG with transparency (RGBA)
- **Animated textures**: Use a 16xN tall PNG + a `.mcmeta` file alongside it
- **3D models**: If you want a custom 3D model (not just a flat texture), edit the corresponding
  JSON in `models/item/custom_spears/` and replace `"parent": "item/handheld"` with your model

## Custom Model Data Reference

Each spear has a unique `custom_model_data` value used by the resource pack:

| Spear          | custom_model_data |
|----------------|-------------------|
| Nightmare Spear | 1001             |
| Sanguine Spear  | 1002             |
| Thunderspear    | 1003             |
| Ashen Spear     | 1004             |
| Phantom Spear   | 1005             |
| Fortune Spear   | 1006             |

You can change these values in `config.yml` under each spear's `custom-model-data` field,
but make sure to update `netherite_spear.json` predicates to match.

## Deploying the Resource Pack

### Option A — Server Resource Pack (recommended)
1. Zip the `resourcepack/` folder contents (not the folder itself — zip `pack.mcmeta` + `assets/`)
2. Host the zip file (e.g. on a CDN, GitHub releases, or your own server)
3. Set in `server.properties`:
   ```
   resource-pack=https://your-link/customspears_rp.zip
   resource-pack-sha1=<sha1 hash of the zip>
   ```

### Option B — Client-side (testing only)
Drop the `resourcepack/` folder into `.minecraft/resourcepacks/` and enable it in-game.
