# Rewound Fabric Mod API

A fork of [Hypixel's FabricModAPI](https://github.com/HypixelDev/FabricModAPI) that implements the
[Rewound Mod API](https://github.com/HypixelNCP/modapi) for Fabric. It's for end users; developers should look at the core
library.

## Building

Build with JDK 25 or newer. The mod is in `build/libs/`.

```sh
./gradlew build
```

## Developer Usage

The mod is available through [JitPack](https://jitpack.io/#HypixelNCP/FabricModAPI).

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.HypixelNCP:FabricModAPI:1.0.3'
}
```

## Debugging

Debug mode runs in a development environment, or with `-Dcc.rewound.modapi.debug=true`. It subscribes to every event and
logs what the server sends.

## License

MIT, as upstream. See [LICENSE](LICENSE).
