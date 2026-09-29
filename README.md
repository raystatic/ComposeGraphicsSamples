# Hinglish Captions

Android app (Jetpack Compose) that adds **Hinglish captions** to short-form videos (Reels / Shorts).

Captions are never guessed by an AI from the video. They come from the actual audio:

1. **Extract audio** – `MediaExtractor` + `MediaCodec` decode the video's audio track and
   down-mix/resample it to a 16 kHz mono WAV (`captions/audio`).
2. **Transcribe** – the WAV is sent to Whisper (Groq `whisper-large-v3` or OpenAI `whisper-1`)
   with `temperature=0` and **word-level timestamps** (`captions/transcribe`).
3. **Hinglish** – every Hindi word is converted from Devanagari to casual Roman script by a
   deterministic transliterator with Hindi schwa deletion and a lexicon of creator spellings
   (`मैं नहीं समझा` → `main nahi samjha`, `वीडियो` → `video`). English words stay as they are.
   (`captions/hinglish`)
4. **Style** – 7 animated presets (Pop, Karaoke, Masala, Neon, Punch, Typewriter, Clean) with
   word-by-word highlighting, plus position and size sliders. Tap any line to fix a word.
5. **Export** – Media3 Transformer burns captions in with a `CanvasOverlay` and saves an H.264
   MP4 (original audio) to `Movies/HinglishCaptions`, ready to share.

The same `CaptionRenderer` draws the live preview and the exported frames, so what you see is
what you get.

## Setup

Open the app → **Settings** → pick a provider and paste an API key
([Groq](https://console.groq.com/keys) is fast and cheap; OpenAI also works). The key is stored
only on the device.

## Tests

`./gradlew test` covers the transliterator, caption chunking/editing, Whisper response parsing
and the audio resampler.
