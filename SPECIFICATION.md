# Communication Channels

## Broker 

La classe Broker crée une communication (un Channel) entre deux interlocuteurs.

Une connexion sur un port ne pourra être acceptée que si quelqu'un a demandé une connexion sur ce port précédemment.

Un Broker ne peut demander qu'une seule connexion à la fois sur un port. Il n'est donc pas possible d'avoir plusieurs connexions sur le même port.
Il est impossible pour un Broker de se connecter a lui même.

Elle a les méthodes suivantes :

- Le constructeur permet de créer un Broker avec un nom en String non nul (un nom ne sera pas accepté s'il existe déjà un Broker du même nom).
- La méthode "accept" permet d'accepter une connexion entrante sur un port particulier. Le port est un int donné en paramètre. On retourne le Channel sur lequel la connexion a été faite.
- La méthode "connect" permet de demander une connexion à un autre Broker. Elle prend en paramètre un name en String qui est le nom d'un autre Broker ainsi qu'un port en int sur lequel sera faite la communication. Elle ouvre une demande de connexion à un Broker ayant le nom name sur le port donné en argument et renvoie un Channel pour cette connexion.

Le port donné en paramètre doit etre positif. Si il ne respecte pas cela alors on refuse la connexion ou l'acceptation.

## Channel

La classe Channel permet la connexion entre deux Brokers.

Elle a les méthodes suivantes :

- La méthode "read" permet de lire des données. Elle prend en paramètre un tableau d'octets dans lequel on rentrera les données lues, un offset en int permettant de savoir où commencer à lire ainsi qu'une longueur en int sur laquelle on veut lire (en nombre d'octets). Elle retourne le nombre de bytes qui ont vraiment été lus.

  Si la longueur donnée en paramètre est supérieure à la longueur des données présentes dans le tableau, alors on lit tout ce qu'il y a et on précise que la lecture a été arrêtée et combien a été lu.

  Si l'offset est supérieur à la longueur du tableau, alors on refuse la lecture et on renvoie une erreur.
- La méthode "write" permet d'écrire des données. Elle prend en paramètre un tableau d'octets, un offset en int permettant de savoir où commencer à écrire ainsi qu'une longueur en int de ce que l'on veut écrire (en nombre d'octets).

  Si la longueur donnée en paramètre est supérieure à la longueur du tableau rentré en paramètre, alors on écrit tout.

  Si on ne peut pas tout écrire (longueur supérieure à l'espace disponible), alors on refuse l'écriture et on renvoie un code d'erreur.

  Si l'offset est supérieur à la fin de l'espace restant, alors on renvoie un code d'erreur et on refuse d'écrire.
- La méthode "disconnect" déconnecte le Broker qui l'appelle.
- La méthode "disconnected" est une méthode qui permet à un Broker de savoir s'il est déconnecté du Channel sur lequel la méthode est appelée.

Si l'on donne des tableaux nuls, une longueur négative ou un offset negatif en paramètre alors on renvoie un code d'erreur et l'on refuse la lecture ou l'écriture
Les methodes Read et Write renvois un code d'erreur et refuse si l'un des deux broker est deconnécté
## Task

La classe Task permet de lancer une tâche sur un thread (elle étend la classe Thread).

Elle a les méthodes suivantes :

- Le constructeur permet de créer une Task. Il prend en paramètre un Broker et un Runnable.

Si l'un des deux paramètres est nul, alors on refuse la création et on renvoie une erreur.
- La méthode "getBroker" permet d'obtenir le Broker associé à cette tâche. Il retourne donc ce Broker.

Si la tâche n'a aucun Broker, on remonte une erreur.