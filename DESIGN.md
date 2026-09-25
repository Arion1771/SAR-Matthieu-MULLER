# Design — Communication Channels

## Plan

- Introduction
- Broker
- Channel
- Task -> modèle d'exécution
- Javadoc
- Queue -> version événementielle (QueueBroker, MessageQueue, Task)

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

# Design - Message Queues

## Principe

- Version événementielle de la couche de communication : plus de `synchronized`, plus de `wait`, plus aucune méthode bloquante.
- Tout s'exécute sur un seul thread, la pompe à événements (`Executor`, thread "Event Pump").
- Une Task n'est plus un thread : c'est un contexte d'exécution, on lui poste des Runnable qui sont exécutés un par un, dans l'ordre, par la pompe.
- Les méthodes qui bloquaient (`accept`, `connect`, `read`, `write`) deviennent asynchrones : elles retournent tout de suite et le résultat arrive plus tard par un listener.
- `accept` devient `bind` / `unbind`, `read` disparait (remplacé par un listener de réception), `write` devient `send`.
- On ne travaille plus sur un flux d'octets mais sur des messages : un message envoyé est reçu en entier, en une seule fois.
- Propriétés garanties : FIFO et lossless, un message ne peut pas "échouer" sinon on perd le FIFO.
- Toutes les méthodes de l'API doivent être appelées depuis la pompe (dans un Runnable posté sur une Task), sauf `Bootstrap.newTask`.

## Executor (pompe à événements)

- Singleton, un seul thread pour tout le système.
- File d'événements FIFO : un événement = (Task, Runnable).
- File des événements retardés triée par date d'échéance (`eta`), remis dans la file normale quand leur date est passée.
- Avant d'exécuter un événement, la pompe positionne la Task courante (`Task.task()` retourne la Task dont le Runnable est en cours).
- Si la Task est morte, l'événement est ignoré.
- Si le Runnable lève une exception non rattrapée, la Task échoue (`fail(th)`).
- Comme il n'y a qu'un thread, deux Runnable ne s'exécutent jamais en même temps : pas besoin de verrou dans QueueBroker et MessageQueue.

## Task

- `post(Runnable r)` : ajoute r à la fin de la file de la pompe, associé à cette Task. Ne fait rien si la Task est morte.
- `post(Runnable r, int delay)` : pareil mais r ne sera exécuté qu'après `delay` millisecondes au minimum.
- `newTask(String name)` : crée une nouvelle Task, vide (rien n'est posté dessus).
- `newBroker(String name)` : crée un QueueBroker dont la Task propriétaire est la Task courante (celle qui le crée).
- `getBroker` : retourne le broker associé à la Task (peut être null).
- `set(Listener l)` : enregistre le listener et retient la Task appelante, c'est sur cette Task que `completed` / `failed` seront postés.
- `exit(Object o)` : la Task se termine normalement avec le résultat o (peut être null), elle devient morte.
- `fail(Throwable th)` : la Task échoue avec la cause th, elle devient morte.
- `dead` : true si la Task a fait `exit` ou `fail`.
- À la mort d'une Task (méthode privée `done`) :
  - toutes les MessageQueue enregistrées sur cette Task sont fermées (`close`) ;
  - si un listener est posé, on poste `failed` ou `completed` sur la Task qui a posé le listener ;
  - sinon, en cas d'échec, on affiche la cause.
- `exit` ou `fail` sur une Task déjà morte -> IllegalStateException (erreur d'utilisation).

## Bootstrap

- Classe `Boot` avec un constructeur public sans argument, instanciée une seule fois.
- `newTask(Runnable r, String name)` : crée la première Task et lui poste r. C'est le seul point d'entrée appelé depuis le thread main.
- Une fois démarré, on ne passe plus par le Bootstrap mais par les méthodes de Task.

## QueueBroker

- Nom unique, non nul et non vide, sinon IllegalArgumentException.
- Un QueueBrokerManager (singleton, comme le Broker Manager des channels) connait tous les QueueBroker :
  - `void add(CQueueBroker)`
  - `CQueueBroker get(String name)`
- Le broker est possédé par la Task qui l'a créé (`getTask`), tous ses listeners (BindListener et ConnectListener) sont exécutés sur cette Task : on fait toujours `getTask().post(...)`, jamais un appel direct.
- Une map port -> BindListener pour les ports liés.

### bind (port, listener)

- Retourne false si le port est invalide (< 0 ou > 65535), si le listener est null ou si le port est déjà lié sur ce broker.
- Sinon enregistre le listener pour ce port et retourne true.
- Pour chaque connexion acceptée sur ce port, `accepted(queue)` est posté sur la Task du broker, avec le bout de la queue qui appartient à ce broker.

### unbind (port)

- Retourne false si le port n'est pas lié.
- Sinon retire le listener de la map, poste `unbound()` sur la Task du broker et retourne true.
- Les queues déjà acceptées sur ce port ne sont pas touchées, seules les nouvelles connexions seront refusées.

### connect (name, port, listener)

- Retourne false si le nom est null/vide, le port invalide, le listener null, ou si aucun broker ne porte ce nom (on cherche dans le QueueBrokerManager).
- Sinon retourne true tout de suite, et la suite est asynchrone : on poste la demande sur la Task du broker distant.
- Quand la demande est traitée côté distant :
  - si le port est lié : on crée les deux bouts de la queue (un pour chaque broker), on poste `accepted(q1)` sur la Task du broker distant et `connected(q2)` sur la Task du broker qui a fait le connect ;
  - si le port n'est pas lié (jamais lié ou déjà `unbind`) : on poste `refused()` sur la Task du broker qui a fait le connect.
- Plusieurs connect sur le même port sont possibles, chacun donne sa propre queue.
- Un broker peut se connecter à lui-même.

## MessageQueue

- Une connexion = deux objets CMessageQueue, un pour chaque bout, chacun connait l'autre (`peer`), comme les deux CChannel.
- Chaque bout appartient au broker qui l'a créé (`broker()`), et est enregistré sur la Task de ce broker pour être fermé si cette Task meurt.
- Attributs de chaque bout :
  - `broker` : le broker propriétaire ;
  - `peer` : l'autre bout ;
  - `listener` + `listenerTask` : le listener de réception et la Task sur laquelle il a été posé ;
  - `pending` : liste FIFO des messages reçus mais pas encore délivrés (pas encore de listener) ;
  - `state` : OPEN, CLOSING ou CLOSED.

### setListener (l)

- Enregistre l et retient la Task courante (`Task.task()`) : `received` et `closed` seront toujours postés sur cette Task.
- Les messages arrivés avant le setListener sont gardés dans `pending` et délivrés, dans l'ordre, dès que le listener est posé : aucun message n'est perdu.
- Si la queue est déjà fermée, on poste directement `closed()`.

### send (bytes, offset, length, listener)

- Vérification des arguments, retourne false et ne fait rien si :
  - bytes est null ;
  - offset < 0 ou length < 0 ;
  - offset + length > bytes.length (un message est une plage contiguë, pas de retour au début).
- Sinon retourne true.
- On copie tout de suite la plage dans un nouveau tableau de taille length : c'est ce tableau qui sera donné à `received(byte[] msg)` de l'autre côté.
- Comme la plage est copiée, la queue n'a plus besoin des octets de l'utilisateur : on poste `sent(bytes, offset, length)` sur la Task courante pour rendre l'ownership (ça peut arriver avant le `received` de l'autre côté, c'est autorisé).
- Tant que `sent` n'a pas été appelé, l'utilisateur ne doit pas modifier la plage.
- Le message copié est posté sur le bout distant qui le délivre à son listener (ou le met dans `pending`).
- Une seule file d'événements pour toute la pompe -> les messages arrivent dans l'ordre d'envoi (FIFO), et rien n'est perdu (lossless).
- Si mon bout est CLOSED ou CLOSING : le message est jeté, on poste quand même `sent(...)` pour rendre l'ownership et on retourne true (même "mensonge" volontaire que pour le write sur un channel déconnecté).

### close / closed

On distingue les deux bouts :
- le bout fermé (closed end point) : celui sur lequel on appelle `close` ;
- le bout qui se ferme (closing end point) : l'autre, qui réagit à la demande de fermeture.

`close` sur un bout OPEN :
- il passe directement CLOSED ;
- les messages encore dans `pending` sont jetés, les messages qui arrivent ensuite aussi ;
- on poste `closed()` sur son listener (s'il y en a un) ;
- on poste une demande de fermeture vers l'autre bout. Cette demande passe par la même file que les messages, donc elle arrive après tous les messages envoyés avant le close.

Quand le bout distant reçoit la demande de fermeture :
- il passe CLOSING ;
- il continue de délivrer les messages déjà reçus (dans `pending`) : tout ce qui a été envoyé avant le close est reçu ;
- tout message qu'il essaie d'envoyer est jeté (ownership rendu par `sent`) ;
- quand il n'a plus rien à délivrer, il passe CLOSED et poste `closed()` sur son listener.
- S'il n'a pas encore de listener, il reste CLOSING avec ses messages en attente, jusqu'au setListener.

Cas particuliers :
- `close` plusieurs fois sur le même bout : accepté, seul le premier appel a un effet.
- `close` sur le bout CLOSING : accepté, il passe CLOSED tout de suite, jette ce qui reste dans `pending` et poste `closed()`.
- `close` des deux côtés en même temps : chacun passe CLOSED, la demande de fermeture qui arrive sur un bout déjà CLOSED est ignorée.
- `closed()` n'est notifié qu'une seule fois par bout.
- `closed` retourne true si mon bout est CLOSED, false s'il est OPEN ou CLOSING.

## Classes

- `Boot` : implémente Bootstrap.
- `CQueueBroker` : implémente QueueBroker (name, task, map des ports liés).
- `QueueBrokerManager` : singleton, map nom -> CQueueBroker.
- `CMessageQueue` : implémente MessageQueue (un bout de la connexion).
- `CTask` et `Executor` (dans utils) : la Task événementielle et la pompe.
