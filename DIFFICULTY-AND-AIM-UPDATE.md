Scramble difficulty and predictive gun update
============================================
Close Scramble. Extract this ZIP into your existing Scramble project folder,
allowing replacement. ZIP paths start with src/, bin/ and Scramble.jar.
Run run.bat, or use bash build.sh followed by bash run.sh.
In Eclipse refresh and clean/build the project before launching.

Based on GitHub revision 1bdc4588ef0b3665a30e38b026ea21caa94e5eb5.
Saved scores, audio diagnostics and project configuration are not replaced.

Every complete four-section cycle compounds scrolling speed by 5%:
level 1 = 150 px/s, level 2 = 157.5, level 3 = 165.375, etc.
The previous hard ceiling is removed. The HUD shows the percentage.

Turrets and bases calculate the earliest intersection between the ship's
constant-velocity future position and the expanding bullet-speed circle.
World-space ship velocity includes scrolling and input, respecting movement
bounds. The projectile launches from the rendered muzzle and keeps its velocity.
If there is no possible future interception, the gun aims directly instead.
You can still evade by changing direction; terrain and bullet lifetime still apply.

Includes all matching source files, Java 8-compatible classes, rebuilt JAR,
README and tests. Existing game, input, save/load and audio checks passed, along
with 10,101 progression/interception assertions, including randomized geometry.
Audible output and interactive Windows gameplay still need local checking.
