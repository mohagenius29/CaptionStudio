# Caption Studio

Pick a video, add your text, choose a caption style, export an MP4 with the captions burned in.
Everything runs on the phone, no internet needed.

## Build the APK (GitHub Actions)
The **Build APK** workflow runs on every push to `main` (or manually from the **Actions** tab).
When it finishes you can get the APK two ways:
- **Releases page**: open the **Caption Studio (latest build)** release and download `CaptionStudio.apk`.
- **Actions run**: download the **CaptionStudio-apk** artifact.

Or open the folder in Android Studio and press Run.

## How to use
1. **Choose video.**
2. **Captions tab**: *Paste text* (one caption per line). The app listens to the audio and times each
   line to the speech, snapping line breaks to natural pauses. Or *Import SRT*. Every caption can be
   edited: text, start, end, and "Start = now / End = now" while the video plays.
3. **Styles tab**: 35 styles in 5 groups (Viral, Clean, Neon & Fun, Cinematic, Arabic). Tap one and
   the preview updates immediately.
4. **Tweak tab**: swap the font (41 fonts incl. Arabic), size, position, entrance/exit animation, colors.
5. **Export tab**: 720p or 1080p; original shape, 9:16, 16:9 or 1:1. Output goes to Movies/CaptionStudio.

## Notes
- Captions are drawn by one renderer used for both preview and export, so they match.
- Fonts are from Google Fonts (SIL Open Font License / Apache 2.0), free to use in videos.
- Word-by-word highlighting spreads words across each caption by word length (no per-word timestamps).
- Automatic speech-to-text is not included in this version.
- Android 8.0 (API 26) or newer. Keep the screen on while exporting.
