# Rewound Fabric Mod API

A fork of [Hypixel's FabricModAPI](https://github.com/HypixelDev/FabricModAPI) that implements the
[Rewound Mod API](https://github.com/HypixelNCP/modapi) for Fabric. It's for end users; developers should look at the core
library.

## Building

The core library isn't published to a repository yet, so install it to your local Maven repository first, then build
this mod with JDK 25 or newer.

```sh
cd ../RewoundModAPI && mvn install
cd ../RewoundFabricModAPI && ./gradlew build
```

The mod is in `build/libs/`.

## Debugging

Debug mode runs in a development environment, or with `-Dcc.rewound.modapi.debug=true`. It subscribes to every event and
logs what the server sends.

## License

MIT, as upstream. See [LICENSE](LICENSE).
