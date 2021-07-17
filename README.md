# ShipGame
Project for learning Java networking and multithreaded programming

### What it is?
A small multiplayer game made using Client-Server architecture in Java. It's done using Swing which may not be the best choise for gamedev but it works (sometimes).

### How does it work?
First, you use the Launcher to Start a server on particular port and IP. Then, (if port-forwarding and everything is configured properly) your friend (or also you, if you have no friends) connect to you using "Join" option and then you play. Gameplay is not very advanced, one player controls the ship and tries to avoid asteroids. Second player manages fuel system, AC and power. The more time you survive the better (Random disconnects are definetely not caused by me not understanding how closing inputstreams work).
