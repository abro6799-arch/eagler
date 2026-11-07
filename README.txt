EAGLER APK PROJECT  (zero-experience guide)

1) Install AndroidIDE (GitHub releases: AndroidIDE/AndroidIDE) and open it.
   Let it finish its first-time setup (needs internet, a few GB).
2) Extract this zip on your phone.
3) Copy your EaglercraftX offline HTML into:
      EaglerApp/app/src/main/assets/
   and rename it exactly to:  index.html
4) In AndroidIDE: Open Project -> pick the EaglerApp folder. Wait for Gradle sync.
5) Tap Build/Run (or Build -> Assemble Debug). The APK ends up in:
      app/build/outputs/apk/debug/
   Install it and open "Eagler".

CONTROLS
- Left side: D-pad (W/A/S/D) + RUN toggle in the middle
- Right side: HIT (left click), USE (right click), JUMP, SNEAK
- Top right: ESC, INV, F5, CHAT   - Bottom center: hotbar 1-9
- Swipe anywhere empty to look around. In menus, just tap.
- Phone Back button = Escape.

LAG TUNING
Open MainActivity.kt and change RESOLUTION (0.5 = fastest, 1.0 = sharpest)
and LOOK_SENS (camera speed). Also lower render distance in-game.
