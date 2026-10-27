# Glimpse embedded typography provenance

These two unmodified fonts are **native app resources**, used locally without CDN requests. They are not font assets to copy into mockups or redistribute separately; the app bundles them as permitted by the accompanying **SIL Open Font License 1.1**. The OFL notices travel with the source and must remain attached to redistributed builds as required by the licence.

| Role | Font | Android resource | Original upstream path | Exact Git blob SHA |
| --- | --- | --- | --- | --- |
| Editorial/display | Fraunces, Undercase Type (Phaedra Charles and Flavia Zimbardi) | `app/src/main/res/font/fraunces_display.ttf` | [Google Fonts Fraunces variable TTF](https://github.com/google/fonts/blob/main/ofl/fraunces/Fraunces%5BSOFT%2CWONK%2Copsz%2Cwght%5D.ttf) | `8210f9488d3c732359a9292dd09aca3f2bae830e` |
| Interface/body | Plus Jakarta Sans, Tokotype | `app/src/main/res/font/plus_jakarta_sans.ttf` | [Google Fonts Plus Jakarta variable TTF](https://github.com/google/fonts/blob/main/ofl/plusjakartasans/PlusJakartaSans%5Bwght%5D.ttf) | `0cb13a998ed525ba226d911b10d6c4c4f923a961` |

- [Fraunces OFL.txt](Fraunces-OFL.txt): original Git SHA `22fa7ce5733434d0c634f6e55116f410858e938c`
- [Plus Jakarta Sans OFL.txt](PlusJakartaSans-OFL.txt): original Git SHA `ea69b0ca12a02556c436680ea0a159efb7a748fa`
- The sources were copied **byte for byte** from `google/fonts` on 26 October 2026 and recorded as unchanged Git objects in `gamesbyian/Glimpse`. `scripts/verify-reel-typography.py` enforces both font and licence blob digests offline.
- `GlimpseTypography.kt` owns the theme mappings and deliberately limits the expressive serif to display/headline/large section titles. Legible sans remains mandatory for settings, labels and actions. User-selected large fonts, reduced motion, TalkBack and narrow 320dp layouts must be accepted independently on native devices.

This is a **proposed implementation of the October 26 visual direction**, not a claim that the owner has approved these precise font families. Do not infer approval, ship static raw font files to end users directly, or treat system font fallback as native accessibility acceptance.
