# To implement

- implement obstacles and path finding for AIs
- mini health bar at entities positions (done for boss 102 hounds, should be easily generalisable)
- integrating sounds
- allow players to put markers on the ground, on enemies and on allies to mark positions
- class description documentation
- improve end of game feeling (don't return directly to menu game)
- key to target furthest ally/enemy
- forbidding players to join a game if it is "launched"
- improve game assets in general
- the life frame of a dead player should not vanish entirely
- put a kind of "halo" on the selected player to help visualize who is currently targeted and show heath bar
- enforce limit in number of players allowed in a given boss

# Done but need improvement

- icon for browser bar (simply change the `icon.ico` in `frontend/src/main/resources`)

# Bugs

- sometimes the game crashes at the very beginning when clicking on "Start Fight". Unclear (yet) how to reproduce (I think this is fixed. It was caused by the first action being wrongfully created in the time at which the server starts, but only added way after when the fight actually starts).
- quite slow on Firefox
- in the GameJoined component, when the page is refreshed while the player is Ready, the selector doesn't go to the correct class name
