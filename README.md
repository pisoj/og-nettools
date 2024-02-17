<img src="/app/src/main/ic_launcher-playstore.png" style="width: 12rem" />

## OG Network Tools
A modern network toolbox built to feel like the original android apps **without jank**. Open and download **instantly**, seriously it's well **under 200KB!**
Why? - Becouse today's apps tend to feel slow and janky on anything but the latest hardware, no more.

**Device support** - All devices running **Android 3.0** and above are supported!

**How is is possible?** - The small size and **extreme compatibility** are possible thanks to our **no dependency** policy. This app literally has empty depecndecies block:
[Click here to verify](https://github.com/pisoj/og-nettools/blob/main/app/build.gradle.kts#L38)


![](https://github.com/pisoj/og-nettools/assets/87895700/fecbf3ca-4746-4d6b-8b36-5ddcc987be4f)

**Does it look good?** - The app uses the original andorid [**Holo**](https://android-developers.googleblog.com/2012/01/holo-everywhere.html) theme, so it's perfect if you miss **good old days**, 
but we've also given it a **modern twist** with custom implementations of UI components like floating action button for example. If you don't believe just look at screenshots below.

<img style="width: 14rem" src="https://github.com/pisoj/og-nettools/assets/87895700/ad69f7e6-45db-4d3c-8bde-c9724ec8d920" />
<img style="width: 14rem" src="https://github.com/pisoj/og-nettools/assets/87895700/da7fd4c7-3876-4783-91d1-83b6e749814c" />
<img style="width: 14rem" src="https://github.com/pisoj/og-nettools/assets/87895700/dbf7b6a7-5a2f-4fda-8771-5a385a0017c3" />
<img style="width: 14rem" src="https://github.com/pisoj/og-nettools/assets/87895700/683fbba3-fde2-4a83-8495-f382a1974e4e" />

## Contributing

Currently the thing that is needed the most is **testing**, especially on older devices **<=Android 4.3**. I unfortunately have an M1 Mac and am therefore unable to run these older emulators.
It would also be very nice if someone would take the time to extract user-facing text into **string resources** to make it **translatable**. I **don't** want to use @dimmen, it just complicates things.
Any other contributions are also welcome. There are **no specific guidelines** since this is a very small project; the only requirement is that you **care** about your **code** and
**others** who will **maintain** it. Using depracated APIs isn't as big of a problem here as it's in other projects since we need to maintain compatibility with very old devices
and avoid using any dependencies, but if you can **avoid using depracated APIs without adding an additional dependency**, you should do it. I'm also **open to any refactoring**.
