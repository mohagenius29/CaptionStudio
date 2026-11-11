# Caption Studio

Pick a video, add your text (or generate timed slots from the audio), choose a caption style, optionally add background music, and export an MP4 with the captions burned in.
Everything runs on the phone.

## Build the APK (GitHub Actions)
The **Build APK** workflow runs on every push to `main` (or manually from the **Actions** tab).
When it finishes you can get the APK two ways:
- **Releases page**: open the **Caption Studio (latest build)** release and download `CaptionStudio.apk`.
- **Actions run**: download the **CaptionStudio-apk** artifact.

Or open the folder in Android Studio and press Run.

## How to use
1. **Choose video.**
2. **Captions tab**:
   - *Paste text* (one caption per line). The app listens to the audio and times each line to the speech, snapping line breaks to natural pauses.
   - *Import SRT*.
   - **Transcribe audio**: analyzes the soundtrack, finds speech regions, and creates timed caption slots. Replace the placeholder text with the real words or lyrics. (Full automatic speech-to-text with an on-device model can be plugged in later.)
   - Every caption can be edited: text, start, end, and "Start = now / End = now" while the video plays.
3. **Audio tab** (new):
   - Choose a background music track.
   - Control **music volume** and **original video audio volume** (set original to 0% to mute dialogue/noise).
   - Optionally loop the music for the full video length.
   - The music is mixed into the final export via Media3 Composition.
4. **Styles tab**: 35 styles in 5 groups (Viral, Clean, Neon & Fun, Cinematic, Arabic). Tap one and the preview updates immediately.
5. **Tweak tab**: swap the font (41 fonts incl. Arabic), size, position, entrance/exit animation, colors.
6. **Export tab**: 720p or 1080p; original shape, 9:16, 16:9 or 1:1. Output goes to Movies/CaptionStudio.

## Notes
- Captions are drawn by one renderer used for both preview and export, so they match.
- Fonts are from Google Fonts (SIL Open Font License / Apache 2.0), free to use in videos.
- Word-by-word highlighting spreads words across each caption by word length (no per-word timestamps).
- Automatic speech-to-text (true ASR) is scaffolded: the app detects speech regions and creates timed editable slots. A full on-device model (Whisper / ML Kit / Vosk etc.) can be added later without rewriting the timeline or export pipeline.
- Background music is mixed with Media3 Transformer Composition (supports looping).
- Android 8.0 (API 26) or newer. Keep the screen on while exporting.
