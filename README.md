# iTantra 📡

### Indian Multilingual Offline Neural Transceiver for Low-Bitrate Communication

iTantra is an **Android-based, offline multilingual communication system** developed for **SIH 2026 Problem Statement 26173 by ISRO**.

The application enables two Android devices to communicate using **speech-to-text and text-to-speech**, allowing voice communication over low-bandwidth local connections without requiring Internet connectivity.

---

## 🚀 Core Concept

Instead of transmitting voice/audio directly:

```text
🎙️ Sender Speech
       ↓
  Offline STT
       ↓
      Text
       ↓
 Local Wireless Link
       ↓
      Text
       ↓
  Offline TTS
       ↓
🔊 Receiver Speech
```

This significantly reduces the amount of data that needs to be transmitted compared with raw audio communication.

---

## ✨ Features

* 🎙️ **Offline Speech-to-Text (STT)**
* 🔊 **Offline Text-to-Speech (TTS)**
* 📡 **Low-bandwidth text-based communication**
* 📱 **Android-to-Android communication**
* 📴 **No Internet dependency**
* 🎙️ **Push-to-Talk / Walkie-Talkie mode**
* 💬 **Real-time speech communication**
* 🌐 Support architecture for **10 Indian languages + English**
* 🚨 **Emergency/priority message handling**
* ⚡ Low-latency communication
* 💾 Lightweight on-device AI processing
* 📊 Performance and latency monitoring

---

## 🌐 Supported Languages

The application is designed for:

* Hindi
* Gujarati
* Marathi
* Kannada
* Malayalam
* Tamil
* Telugu
* Odia
* Bengali
* English

---

## 🏗️ Architecture

```text
                    iTANTRA
                       │
          ┌────────────┴────────────┐
          │                         │
      SENDER DEVICE            RECEIVER DEVICE
          │                         │
     🎙️ Microphone              📡 Receive
          ↓                         ↓
     Offline STT               Text Decode
          ↓                         ↓
        Text                    Offline TTS
          ↓                         ↓
   Text Encoding              🔊 Speaker
          │
          └──── Local Wireless ────┘
                Wi-Fi / Bluetooth
```

---

## 🛠️ Technology Stack

### Android

* **Kotlin**
* **Android SDK**
* **Jetpack Compose / Android UI**
* **Gradle**

### AI / ML

* On-device Speech-to-Text
* On-device Text-to-Speech
* Open-source ML models
* ONNX Runtime / LiteRT for mobile inference
* Model quantization and optimization

### Communication

* Local Wi-Fi
* Wi-Fi Direct
* Bluetooth
* Text-based packet transmission

### Development

* Android Studio
* Cursor
* Git
* GitHub

---

## 🔐 Offline-First Design

iTantra is designed to operate without Internet connectivity.

The STT and TTS models run directly on the Android device.

```text
Internet
   ❌
   │
   X
   │
Android Device
 ├── STT Model
 ├── TTS Model
 └── Communication Module
```

No cloud-based speech APIs are required for the core communication pipeline.

---

## 📡 Communication Model

The system does **not transmit raw speech audio** during normal communication.

Instead:

```text
Speech
  ↓
STT
  ↓
Text
  ↓
Compact Packet
  ↓
Local Wireless Network
  ↓
Text
  ↓
TTS
  ↓
Speech
```

This allows the system to operate efficiently over constrained communication links.

---

## 🎙️ Push-to-Talk Mode

The sender can hold a **Push-to-Talk** button to communicate.

```text
HOLD
 ↓
Start Recording
 ↓
Offline STT
 ↓
Sentence Detection
 ↓
Transmit Text
 ↓
Release
```

The receiver automatically converts the received text into speech.

---

## 🚨 Emergency Communication

iTantra supports priority-based messages.

```text
NORMAL
IMPORTANT
EMERGENCY
```

Emergency messages can be prioritized for faster transmission and immediate audio playback.

---

## ⚡ Performance Goals

The system is designed to optimize:

* STT accuracy
* TTS intelligibility
* End-to-end latency
* Model size
* RAM consumption
* CPU utilization
* Real-Time Factor (RTF)
* Communication bandwidth

All performance values should be measured on the target Android hardware rather than hardcoded.

---

## 📂 Project Structure

```text
iTantra/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   │
│   ├── build.gradle
│   └── proguard-rules.pro
│
├── models/
│   ├── stt/
│   └── tts/
│
├── gradle/
├── build.gradle
├── settings.gradle
└── README.md
```

---

## ⚙️ Installation

### Requirements

* Android Studio
* Android SDK
* Android device or Android Emulator
* Android 8.0+ recommended
* Sufficient storage for local AI models

### Clone Repository

```bash
git clone https://github.com/K-2004p/iTantra_app.git
cd iTantra_app
```

### Build

Open the project in Android Studio and allow Gradle to synchronize.

Then:

```bash
./gradlew assembleDebug
```

For Windows:

```powershell
.\gradlew.bat assembleDebug
```

The generated APK will be available under:

```text
app/build/outputs/apk/debug/
```

---

## 📱 Two-Device Demonstration

### Device A — Sender

```text
Select: SENDER
Select Language
Connect to Receiver
Press HOLD TO TALK
Speak
```

### Device B — Receiver

```text
Select: RECEIVER
Connect to Sender
Receive Text
Offline TTS
Hear Speech
```

### Complete Flow

```text
        DEVICE A
     🎙️ Speak
         ↓
     Offline STT
         ↓
        TEXT
         ↓
   Local Wi-Fi / BT
         ↓
        TEXT
         ↓
     Offline TTS
         ↓
     🔊 DEVICE B
```

---

## 🔒 Privacy

All speech processing is intended to happen locally on the device.

The application does not require uploading voice recordings to external servers for the core STT/TTS pipeline.

---

## 🎯 SIH 2026 Alignment

**Problem Statement:** 26173

**Organization:** Indian Space Research Organisation (ISRO)

**Objective:** Build a lightweight multilingual STT/TTS aided neural transceiver capable of operating over low-bitrate links.

iTantra focuses on:

* Offline AI
* Indian-language speech processing
* Low-bandwidth communication
* Real-time communication
* Lightweight edge inference
* Android deployment

---

## 👥 Project

**iTantra — Offline Multilingual Neural Transceiver**

Built for **Smart India Hackathon 2026**.
