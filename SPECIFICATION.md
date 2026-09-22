# Communication Channels

## Broker

La classe Broker crée une communication (un Channel) entre deux interlocuteurs.

Une connexion sur un port ne pourra être acceptée que si quelqu'un a demandé une connexion sur ce port précédemment.

Plusieurs demandes de connexion (`connect`) peuvent être en attente simultanément sur un même port : elles sont mises en file d'attente et satisfaites une par une par des appels successifs à `accept`. En revanche, un Broker ne peut avoir qu'un seul `accept` en cours à la fois sur un port donné.

Un Broker peut se connecter à lui-même.

Elle a les méthodes suivantes :

- Le constructeur permet de créer un Broker avec un nom en String non nul et non vide (un nom vide ne sera pas accepté, et un nom ne sera pas accepté s'il existe déjà un Broker du même nom). Si le nom est nul, vide ou déjà utilisé, la création du Broker est refusée.
- La méthode "accept" permet d'accepter une connexion entrante sur un port particulier. Le port est un int donné en paramètre. On retourne le Channel sur lequel la connexion a été faite.

  La méthode "accept" refuse la connexion si le port est inférieur à 0 ou supérieur à 65535. Elle refuse aussi la connexion si aucune demande n'a été faite sur ce port. Un Broker ne peut accepter qu'une seule connexion sur un port à la fois.
- La méthode "connect" permet de demander une connexion à un autre Broker. Elle prend en paramètre un name en String qui est le nom d'un autre Broker ainsi qu'un port en int sur lequel sera faite la communication. Elle ouvre une demande de connexion à un Broker ayant le nom name sur le port donné en argument et renvoie un Channel pour cette connexion.

  La méthode "connect" refuse la connexion si name est nul ou vide, ou si le port est inférieur à 0 ou supérieur à 65535. Si aucun Broker ne porte le nom donné, "connect" ne refuse pas la demande : elle renvoie `null`, afin de pouvoir être retentée plus tard si ce Broker est créé ultérieurement.

## Channel

La classe Channel permet la connexion entre deux Brokers.

Elle a les méthodes suivantes :

- La méthode "read" permet de lire des données. Elle prend en paramètre un tableau d'octets dans lequel on rentrera les données lues, un offset en int permettant de savoir où commencer à lire ainsi qu'une longueur en int sur laquelle on veut lire (en nombre d'octets). Elle retourne le nombre de bytes qui ont vraiment été lus.

  Si la longueur donnée en paramètre est supérieure à la longueur des données présentes dans le tableau, alors on lit tout ce qu'il y a et on précise que la lecture a été arrêtée et combien a été lu.

  Si l'offset est supérieur à la longueur du tableau, alors on refuse la lecture et on renvoie une erreur. Si l'offset est égal à la longueur du tableau, la lecture est valide mais ne lit aucun octet.
- La méthode "write" permet d'écrire des données. Elle prend en paramètre un tableau d'octets, un offset en int permettant de savoir où commencer à écrire ainsi qu'une longueur en int de ce que l'on veut écrire (en nombre d'octets).

  Si la longueur donnée en paramètre est supérieure à la longueur du tableau rentré en paramètre, alors on écrit tout.

  Si on ne peut pas tout écrire (longueur supérieure à l'espace disponible), on écrit ce que l'on peut : "write" n'a jamais à écrire tous les octets demandés, elle écrit au minimum 1 octet et retourne le nombre d'octets réellement écrits. Une longueur nulle est acceptée et n'écrit aucun octet.

  Si l'offset est supérieur à la fin du tableau, alors on renvoie une erreur et on refuse d'écrire.
- La méthode "disconnect" déconnecte le Broker qui l'appelle. Elle peut être appelée plusieurs fois sans provoquer d'erreur : seul le premier appel a un effet, les suivants sont simplement ignorés.
- La méthode "disconnected" est une méthode qui permet à un Broker de savoir s'il est déconnecté du Channel sur lequel la méthode est appelée.

Si l'on donne des tableaux nuls, une longueur négative ou un offset négatif en paramètre alors on renvoie un code d'erreur et l'on refuse la lecture ou l'écriture.

Une lecture ou une écriture sur un Channel dont l'un des deux Broker est déconnecté n'est pas une erreur : une lecture sans donnée disponible sur un Channel déconnecté retourne 0, et une écriture sur un Channel déconnecté ignore silencieusement les octets à écrire (aucun code d'erreur n'est renvoyé dans les deux cas).

## Task

La classe Task permet de lancer une tâche sur un thread (elle étend la classe Thread).

Elle a les méthodes suivantes :

- Le constructeur permet de créer une Task. Il prend en paramètre un Broker et un Runnable.

  Si l'un des deux paramètres est nul, alors on refuse la création et on renvoie une erreur.
- La méthode "getBroker" permet d'obtenir le Broker associé à cette tâche. Elle retourne toujours ce Broker et ne renvoie jamais une valeur nulle pour une Task valide, puisque le constructeur garantit déjà qu'une Task possède toujours un Broker non nul. Elle peut être appelée aussi bien avant qu'après le démarrage du Thread, et retourne toujours le même Broker.
