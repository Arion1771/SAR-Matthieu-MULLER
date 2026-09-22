# Design — Communication Channels

## Plan

- Introduction
- Broker
- Channel
- Task -> modèle d'exécution
- Javadoc

- Broker -> intermédiaire
- Channel -> rendez-vous

Broker -> nom unique.
Task utilise un Broker pour communiquer.
Channel -> flux d'octets (byte stream), full duplex, FIFO, dans les deux sens (les deux tâches peuvent écrire et lire).
Propriété TCP -> éviter la perte, lossless, même ordre que l'écriture.
Le flux ne garantit pas que les octets arrivent à la même vitesse (possibilité d'arriver par paquets) ; seule contrainte : une taille maximale, mais pas de taille minimale.
Comportement multi-thread -> utilisation unique -> verrou sur read et write.

Le flux ne refuse jamais l'écriture : on écrit/lit ce que l'on peut et on retourne le nombre d'octets qui ont vraiment été écrits/lus.
Il n'est pas possible de partager un Channel -> un seul lecteur et un seul écrivain à la fois, pas plus -> possibilité d'ownership.
Le flux impacte tout, il n'y a pas de notion de message ni de paquet.

## Broker

- Le constructeur passe par le Broker Manager.
- Les méthodes `accept` et `connect` sont bloquantes.
- Pas besoin d'ordre entre elles : les deux séquences sont possibles (`accept` puis `connect`, ou `connect` puis `accept`).
- Un Broker peut être associé à plusieurs tâches, mais une tâche ne peut avoir qu'un seul Broker.
- `accept (port)` -> un seul accept à la fois sur un port donné -> objet représentant le Broker en attente sur ce port.
- `connect (nom, port)` -> plusieurs connect possibles sur un même port -> passe par le Broker Manager, un singleton qui connaît tous les Broker (et que tous les Broker connaissent).
- Un objet de rendez-vous existe sur le Broker, par port.
- Un Broker peut se connecter à lui-même (si l'utilisateur fait n'importe quoi, c'est son problème : on ne protège que ce qui casse le système, pas la mauvaise utilisation).
- Si le Broker demandé est absent -> `connect` retourne `null`.

## Classe RdV

Liste de connect en attente (map), un seul accept.

- constructeur
- accept
- connect

Retourne et crée les channel.

## Broker Manager

- `void addBroker(Broker)`
- `Broker getBroker(String name)`

## Channel

Un channel est unique pour un couple de Broker sur un même port. Un tableau circulaire pour chaque sens : un channel de chaque côté -> chaque côté du tuyeau.

Deux constructeurs :
- le premier, sans argument, construit juste son IN ;
- le second prend un channel et construit son IN, prend son OUT par l'autre channel et lui set son OUT (sans setteur).

- `read` lit tous les octets dans le tableau où cette task lit. Bloquant si rien à lire (wait).
- `write` écrit tous les octets qu'il peut dans le tableau où cette task écrit. Non bloquant si pas de place pour écrire. Retourne le nombre d'octets écrits. NotifyAll quand a fini d'écrire pour réveiller l'autre côté.
- `read` ne bloque pas tant qu'il y a quelque chose à lire ; `write` ne bloque pas tant qu'il y a de la place pour écrire.
- Valeur de retour : de 1 à n, jamais 0 (sauf le cas particulier de la déconnexion ci-dessous).
- La méthode `disconnect` : deconnecte le channel
- La méthode `disconnected` : retourne true si mon coté est déconnecté, false sinon

### Si je me déconnecte :

- read bloqué : possibilité de lire malgré la déconnexion
- read alors que déconnecté : possible, retourne 0
- write alors que déconnecté : possible, retourne n
  -> "mensonge" volontaire pour éviter à l'utilisateur d'avoir à faire du try/catch partout ou de tester un code d'erreur.

### Si l'autre est déconnecté :

- read bloqué : retourne 0 quand reveillé par l'autre coté, annonce de deconnexion
- read alors que l'autre est déconnecté : lis jusqu'à ce qu'il n'y ait plus rien à lire, puis retourne 0
- write alors que déconnecté : possible, retourne n, on ecrit meme si l'autre ne lira pas

## Task

- Le constructeur :
  - prend en paramètre un Broker et un Runnable
  - initialise les attributs de la classe avec les valeurs passées en paramètre
  - vérifie que le Broker et le Runnable ne sont pas nuls, sinon throw IllegalArgument
- La méthode static `getBroker` :
  - Retourne le broker associé à cette tâche
