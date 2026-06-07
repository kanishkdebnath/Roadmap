# Roadmap — App Icon Generation Prompt

Concept: **Progress Ring + Check** — one bold progress ring with a single checkmark inside. Keeps the app's signature ring motif and reads as "goal / done" instantly. Designed to stay crisp when scaled down (notification/badge sizes ~24px).

## Legibility rules (why this works small)
- **One hero idea, two bold elements** (ring + check) — nothing else.
- **Thick strokes, high contrast, generous padding.** No thin lines, no small dots, no fine detail.
- **Flat vector, no 3D/shadows/texture** — those turn to noise at small sizes.

## Design tokens (from the app's design system)

| Token | Hex | Role |
|-------|-----|------|
| Brand | `#022E1C` | deep pine green — squircle background |
| Mint | `#34D39A` | ring (gradient start) |
| Cyan | `#22D3EE` | ring (gradient end) |
| White | `#FFFFFF` | checkmark (max contrast inside the ring) |
| Dark canvas | `#061A10` | optional dark-variant background |

- **Container:** rounded-square "squircle" (corner radius ≈ 30% of size, matching `GradientTile`).
- **Signature flourish:** mint→cyan linear gradient (`#34D39A → #22D3EE`) — the app's `primaryBrush` in dark mode.
- **Motif:** the app's uniform brand-colored **progress ring** + a completion check.
- **Feel:** Inter, Material 3, clean flat-vector, premium productivity aesthetic. No text in the icon.

---

## 🎯 Master prompt (tool-agnostic)

> A modern, minimal **mobile app icon** for a goal-tracking app called **"Roadmap."** Centered on a rounded-square **squircle** tile with a **deep pine-green background (#022E1C)** — flat, with at most a whisper of a darker radial vignette in the corners for depth.
>
> **One single hero element:** a **bold circular progress ring**, drawn as a **thick rounded-cap stroke** filled with a fresh **mint-to-cyan linear gradient (#34D39A → #22D3EE)**. The ring is **nearly complete (~85% of the circle) with one clean rounded gap** at the top-right, clearly reading as a progress indicator. The ring is large and confident, occupying roughly **60–65% of the icon** with comfortable padding all around.
>
> **Inside the ring:** a single **bold checkmark** in **solid white**, thick rounded-cap strokes, centered, sized to sit with breathing room inside the ring — signalling a completed goal.
>
> **Nothing else** — no extra dots, paths, text, or decoration. **Style:** clean **flat vector**, geometric, crisp high-contrast edges, lots of negative space, premium productivity-app aesthetic, Material 3 + iOS app-icon sensibility. Essentially flat — no 3D, no bevel, no heavy shadows. **Must stay clearly legible when scaled down to a small size.** Square 1:1, **1024×1024**, perfectly centered.

**Negative prompt:**
```
text, letters, numbers, watermark, signature, photorealistic, 3D, bevel, glossy
reflections, drop shadow, gradient banding, noise, thin hairlines, busy detail,
extra dots, multiple objects, paper map, compass, location pin, people, hands, low-res
```

---

## 🛠 Tool-specific variants

### Midjourney v6
```
app icon for goal-tracking app "Roadmap", deep pine-green #022E1C rounded-square
squircle, ONE bold circular progress ring (~85% with a clean gap top-right) in a
mint-to-cyan gradient (#34D39A to #22D3EE), thick rounded-cap stroke, with a single
bold white checkmark centered inside, flat vector, geometric, minimal, high contrast,
lots of padding, legible when small, no text
--ar 1:1 --style raw --stylize 120 --v 6 --no text, letters, numbers, dots, photoreal, 3d, shadow
```

### ChatGPT / DALL·E 3
Paste the full Master prompt above, then append:
```
Render as a flat vector app icon, 1024×1024, perfectly centered, no text of any kind,
exactly two elements (the gradient progress ring and one white checkmark).
Use ONLY: #022E1C (background), #34D39A→#22D3EE (ring), #FFFFFF (check).
It must remain clearly recognizable when shrunk to 48px.
```

### Recraft / Figma AI / vector (SVG-ready)
```
Flat 2D vector app icon, SVG-style, limited palette: #022E1C background, mint #34D39A,
cyan #22D3EE, white. Rounded-square container. A single thick circular progress ring
(~85% sweep, one rounded gap) with a mint→cyan gradient, plus one bold white checkmark
centered inside, rounded caps, even stroke width. Geometric, crisp, minimal, high
contrast, generous safe-zone padding, no text, no extra elements.
```

---

## 📱 Android adaptive-icon note

The project uses layered adaptive icons (`mipmap-anydpi-v26/ic_launcher.xml` → foreground + background). Generate **two layers**:

- **Background layer:** solid `#022E1C` (full bleed, 108×108dp canvas).
- **Foreground layer:** the **ring + check only**, on a **transparent background**, kept inside the central **66dp safe zone** (the system mask crops ~18dp per edge) so nothing clips on round/squircle masks.

Append to the foreground prompt: *"…ring and checkmark only, isolated on a fully transparent background, all artwork within the central 66% safe circle."*

---

## 🎨 Optional tweaks
- **Color swap:** white ring + **mint→cyan checkmark** (inverts the contrast) — try both, pick whichever pops more at small size.
- **Full ring:** drop the gap for a closed circle if the progress-gap reads as a "broken" ring when tiny.
- **Marker swap:** replace the check with a single **upward arrow tip** (roadmap/forward) or a single **filled dot** (a milestone) if you want to lean less on "done."
- **Inverted container:** make the squircle itself the **mint→cyan gradient** with a **white** ring + check (matches the `GradientTile` / `primaryBrush` treatment) — brighter alternate.
- **Dark/monochrome variant:** near-black deep-green `#061A10` background with the same mint→cyan ring — matches the dark theme.
