# Audio Recorder & Transcriber App

An Android app built with Jetpack Compose that records audio and transcribes it to text using local AI (Whisper model). The app runs completely offline without requiring internet connectivity.

## Features

- **Audio Recording**: Record high-quality audio using the device microphone
- **Local AI Transcription**: Transcribe audio to text using OpenAI's Whisper model running locally
- **File Management**: Save audio files and transcripts to local storage
- **Edit Transcriptions**: Edit and update transcriptions after AI processing
- **Delete Recordings**: Remove audio files and their associated transcripts
- **Offline Operation**: No internet connection required for transcription
- **Modern UI**: Clean, intuitive interface built with Jetpack Compose

## Technical Architecture

### Components

1. **AudioRecordingService**: Handles audio recording using MediaRecorder
2. **WhisperTranscriptionService**: Manages local AI transcription
3. **AudioRecordingRepository**: Manages data persistence and file operations
4. **AudioRecordingViewModel**: Coordinates UI state and business logic
5. **UI Components**: Compose-based interface for recording and managing audio

### Data Flow

1. User records audio → AudioRecordingService
2. Audio file saved to local storage → AudioRecordingRepository
3. User requests transcription → WhisperTranscriptionService
4. Transcription result saved alongside audio → AudioRecordingRepository
5. User can edit transcription → Updated in local storage

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24 or higher
- Device with microphone support

### Installation

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run the app on a physical device (recommended for audio recording)

### Permissions

The app requires the following permissions:
- `RECORD_AUDIO`: For recording audio
- `WRITE_EXTERNAL_STORAGE`: For saving audio files
- `READ_EXTERNAL_STORAGE`: For reading audio files

### Whisper AI Integration

Currently, the app uses a simulated transcription service for demonstration purposes. To enable real Whisper AI transcription:

1. **Download Whisper Model**: 
   - Download a Whisper model from [Hugging Face](https://huggingface.co/ggerganov/whisper.cpp/tree/main)
   - Recommended: `ggml-base.en.bin` for English transcription
   - Place the model in the app's external files directory

2. **Native Library Setup**:
   - Add Whisper.cpp native library to your project
   - Update `WhisperTranscriptionService.kt` to use actual native calls
   - Configure CMake build for native compilation

3. **Audio Format Conversion**:
   - Implement audio format conversion to 16kHz WAV
   - Consider using FFmpeg for robust audio processing

## File Structure

```
app/src/main/java/com/example/medicaid/
├── data/
│   ├── AudioRecording.kt                 # Data model for recordings
│   ├── AudioRecordingRepository.kt       # Data persistence layer
│   ├── AudioRecordingService.kt          # Audio recording functionality
│   └── WhisperTranscriptionService.kt    # AI transcription service
├── ui/
│   ├── AudioRecordingScreen.kt           # Main recording interface
│   ├── AudioRecordingViewModel.kt        # UI state management
│   ├── RecordingItem.kt                  # Recording list item component
│   └── theme/                            # App theming
└── MainActivity.kt                       # App entry point
```

## Storage Structure

- **Audio Files**: `Android/data/com.example.medicaid/files/Music/AudioRecordings/`
- **Transcripts**: `Android/data/com.example.medicaid/files/transcripts/`
- **Metadata**: `Android/data/com.example.medicaid/files/recordings.json`

## Usage

1. **Grant Permissions**: Allow microphone and storage access
2. **Record Audio**: Tap the microphone button to start recording
3. **Stop Recording**: Tap stop when finished
4. **Transcribe**: Tap the transcribe button on any recording
5. **Edit**: View and edit transcriptions by tapping the document icon
6. **Delete**: Remove recordings and transcripts as needed

## Development Notes

### Current Implementation

The app currently uses simulated transcription for demonstration purposes. This allows you to:
- Test the complete UI flow
- Verify audio recording functionality
- Understand the app architecture

### Production Considerations

For production use, implement:
- Real Whisper.cpp integration
- Audio format optimization
- Error handling for model loading
- Background processing for large files
- User feedback for long transcription processes

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Test thoroughly on physical devices
5. Submit a pull request

## License

This project is open source and available under the MIT License.

## Acknowledgments

- OpenAI for the Whisper speech recognition model
- Whisper.cpp project for the C++ implementation
- Android Jetpack Compose team for the UI framework
