# Assistio

Assistio is a *free and open source* **voice assistant** running on Android. It supports many different **skills** and input/output methods, and it provides both **speech** and **graphical** feedback to a question. It interprets user input and (when possible) generates user output entirely **on-device**, providing privacy by design. It has **multilanguage** support, and is currently available in these languages: Czech, Dutch, English, French, German, Greek, Italian, Polish, Russian, Slovenian, Spanish, Swedish, Turkish, and Ukrainian.

<p align="center">
    <img width="80" alt="Assistio logo" src="./app/src/main/res/mipmap-xxxhdpi/ic_launcher.png">
    &emsp;
    <a href="https://github.com/lincomax/Assistio/releases">
        <img width="206" alt="Get it on GitHub" src="./meta/get-it-on-github.png">
    </a>
</p>

## Skills

Currently, Assistio can respond to:
- **search**: looks up information on **DuckDuckGo** - _Search for cat toys_
- **weather**: collects weather information from **OpenWeatherMap** - _What's the weather like?_
- **lyrics**: shows **Genius** lyrics for songs - _What's the song that goes we will we will rock you?_
- **open**: opens an app on your device - _Open NewPipe_
- **calculator**: evaluates basic calculations - _What is four thousand and two times three minus a million divided by three hundred?_
- **telephone**: view and call contacts - _Call Mom_
- **text messages**: send a text to a contact after reading it back for a spoken yes - _Text Mom saying I am on my way_
- **reminders**: add reminders and tasks to **Tasks.org** - _Remind me to call Mom at noon_
- **timer**: set, query and cancel timers - _Set a timer for five minutes_
- **current time**: query current time - _What time is it?_
- **navigation**: opens the navigation app at the requested position - _Take me to New York, fifteenth avenue_
- **media**: play, pause, previous, next song
- **translation**: translate from/to any language with **Lingva** - _How do I say Football in German?_
- **wake word control**: turn on/off the wakeword - _Stop listening_
- **notifications**: reads all notifications currently in the status bar - _What are my notifications?_
- **flashlight**: turn on/off the phone flashlight - _Turn on the flashlight_

## Speech to text

Assistio uses [Vosk](https://github.com/alphacep/vosk-api/) as its speech to text (`STT`) engine. In order to be able to run on every phone, small models are employed, weighing `~50MB`. The download from [here](https://alphacephei.com/vosk/models) starts automatically whenever needed, so the app language can be changed seamlessly.

## Wake Word

Assistio uses [OpenWakeWord](https://github.com/dscripka/openWakeWord) for wake word support, and by defaults it listens for the _Hey Dicio_ keyword. If you would like to use a different keyword, you can download other `.tflite` models from [Open Wake Word](https://github.com/dscripka/openWakeWord/releases/tag/v0.5.1) or from [this collection](https://github.com/fwartner/home-assistant-wakewords-collection). Then head to `Settings > Input and output methods > Import custom wake word` and select the `.tflite` model you downloaded. Alternatively, you can train a wake word model for a keyword of your choice by following this [Jupiter Notebook](https://github.com/dscripka/openWakeWord/blob/main/notebooks/automatic_model_training.ipynb) with this [configuration](meta/openwakeword_training_config.yml).

### Adding skills

If you want to add a new skill, or improve an existing one, check out the guide in the documentation: https://dicio.stypox.org/adding_skill.html
